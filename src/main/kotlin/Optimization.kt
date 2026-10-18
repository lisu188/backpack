import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.random.Random

const val MAX_INPUT = 100_000
const val SEARCH_ITERATIONS = 9_999_999
private const val PROGRESS_BATCH = 1024

data class OptimizationInput(val capacity: Int, val itemCount: Int) {
    init {
        require(capacity in 1..MAX_INPUT && itemCount in 1..MAX_INPUT)
    }
}

fun parseOptimizationInput(capacity: String, itemCount: String): OptimizationInput? {
    val parsedCapacity = capacity.trim().toIntOrNull() ?: return null
    val parsedCount = itemCount.trim().toIntOrNull() ?: return null
    if (parsedCapacity !in 1..MAX_INPUT || parsedCount !in 1..MAX_INPUT) return null
    return OptimizationInput(parsedCapacity, parsedCount)
}

data class OptimizationResult(val itemWeights: List<Float>, val genome: List<Boolean>, val remaining: Double)

/** Single-bit greedy transitions; candidate evaluation does not copy or rescore the genome. */
internal class BackpackSearch(private val capacity: Int, itemWeights: FloatArray) {
    private val weights = itemWeights.copyOf()
    private val selected = BooleanArray(weights.size)
    private var totalWeight = 0.0
    val remaining: Double get() = capacity - totalWeight

    init {
        require(capacity in 1..MAX_INPUT)
        require(weights.isNotEmpty() && weights.size <= MAX_INPUT)
        require(weights.all { it.isFinite() && it in 0.0f..1.0f })
    }

    fun tryFlip(index: Int): Boolean {
        val delta = if (selected[index]) -weights[index].toDouble() else weights[index].toDouble()
        val candidate = totalWeight + delta
        if (candidate > totalWeight && candidate <= capacity) {
            selected[index] = !selected[index]
            totalWeight = candidate
            return true
        }
        return false
    }

    fun genome(): List<Boolean> = selected.toList()
}

suspend fun optimizeBackpack(
    input: OptimizationInput,
    random: Random = Random.Default,
    iterations: Int = SEARCH_ITERATIONS,
    onImprovement: suspend (Double) -> Unit = {}
): OptimizationResult {
    require(iterations in 0..SEARCH_ITERATIONS)
    val context = currentCoroutineContext()
    val weights = FloatArray(input.itemCount) {
        context.ensureActive()
        random.nextFloat()
    }
    val search = BackpackSearch(input.capacity, weights)
    var reportedRemaining = search.remaining
    repeat(iterations) { iteration ->
        // Even a long run with no accepted candidates must respond to cancellation.
        context.ensureActive()
        search.tryFlip(random.nextInt(input.itemCount))
        if ((iteration + 1) % PROGRESS_BATCH == 0 && search.remaining < reportedRemaining) {
            onImprovement(search.remaining)
            reportedRemaining = search.remaining
        }
    }
    context.ensureActive()
    if (search.remaining < reportedRemaining) onImprovement(search.remaining)
    return OptimizationResult(weights.toList(), search.genome(), search.remaining)
}
