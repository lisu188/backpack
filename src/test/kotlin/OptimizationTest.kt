import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class OptimizationTest {
    @Test fun parsesDefaultsAndBounds() {
        assertEquals(OptimizationInput(10, 10000), parseOptimizationInput("10", "10000"))
        assertEquals(OptimizationInput(1, MAX_INPUT), parseOptimizationInput(" 1 ", " $MAX_INPUT "))
    }

    @Test fun rejectsInvalidCapacity() {
        for (value in listOf("", "0", "-1", "1.5", "NaN", "Infinity", "100001", "2147483648")) {
            assertNull(parseOptimizationInput(value, "10"), value)
        }
    }

    @Test fun rejectsInvalidItemCount() {
        for (value in listOf("", "0", "-1", "1.5", "NaN", "Infinity", "100001", "2147483648")) {
            assertNull(parseOptimizationInput("10", value), value)
        }
    }

    @Test fun coreRejectsInvalidInputs() {
        assertThrows(IllegalArgumentException::class.java) { OptimizationInput(0, 1) }
        assertThrows(IllegalArgumentException::class.java) { OptimizationInput(1, MAX_INPUT + 1) }
        assertThrows(IllegalArgumentException::class.java) { BackpackSearch(1, floatArrayOf()) }
        for (weight in listOf(-0.1f, 1.1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { BackpackSearch(1, floatArrayOf(weight)) }
        }
    }

    @Test fun acceptsOneBitAtATimeIncludingExactCapacity() {
        val search = BackpackSearch(1, floatArrayOf(0.25f, 0.75f))
        assertTrue(search.tryFlip(0))
        assertEquals(listOf(true, false), search.genome())
        assertEquals(0.75, search.remaining)
        assertTrue(search.tryFlip(1))
        assertEquals(listOf(true, true), search.genome())
        assertEquals(0.0, search.remaining)
    }

    @Test fun rejectsOverweightWithoutChangingSelectionOrScore() {
        val search = BackpackSearch(1, floatArrayOf(0.75f, 0.5f))
        assertTrue(search.tryFlip(0))
        assertFalse(search.tryFlip(1))
        assertEquals(listOf(true, false), search.genome())
        assertEquals(0.25, search.remaining)
    }

    @Test fun rejectsRemovingSelectedItemAsANonImprovement() {
        val search = BackpackSearch(1, floatArrayOf(0.5f))
        assertTrue(search.tryFlip(0))
        assertFalse(search.tryFlip(0))
        assertEquals(listOf(true), search.genome())
        assertEquals(0.5, search.remaining)
    }

    @Test fun zeroWeightIsNotAnImprovement() {
        val search = BackpackSearch(1, floatArrayOf(0.0f))
        assertFalse(search.tryFlip(0))
        assertEquals(listOf(false), search.genome())
    }

    @Test fun inputWeightMutationCannotChangeTheSearch() {
        val weights = floatArrayOf(0.5f)
        val search = BackpackSearch(1, weights)
        weights[0] = 0.75f
        search.tryFlip(0)
        assertEquals(0.5, search.remaining)
    }

    @Test fun cachedDeltaMatchesIndependentFullRescoring() {
        val random = Random(734)
        val weights = FloatArray(100) { random.nextFloat() }
        val search = BackpackSearch(10, weights)
        var reference = BooleanArray(weights.size)
        fun score(genome: BooleanArray): Double = 10 - weights.indices.sumOf {
            if (genome[it]) weights[it].toDouble() else 0.0
        }
        repeat(3000) {
            val index = random.nextInt(weights.size)
            val candidate = reference.copyOf()
            candidate[index] = !candidate[index]
            val candidateScore = score(candidate)
            val accepted = candidateScore < score(reference) && candidateScore >= 0
            assertEquals(accepted, search.tryFlip(index))
            if (accepted) reference = candidate
            assertEquals(reference.toList(), search.genome())
            assertEquals(score(reference), search.remaining)
        }
    }

    @Test fun injectedSeedIsReproducible() = runBlocking {
        val input = OptimizationInput(3, 20)
        assertEquals(optimizeBackpack(input, Random(42), 2000), optimizeBackpack(input, Random(42), 2000))
    }

    @Test fun zeroIterationsReturnsEmptySelection() = runBlocking {
        val result = optimizeBackpack(OptimizationInput(1, 3), Random(42), 0)
        assertEquals(listOf(false, false, false), result.genome)
        assertEquals(1.0, result.remaining)
    }

    @Test fun rejectsInvalidIterationLimits() {
        for (iterations in listOf(-1, SEARCH_ITERATIONS + 1)) {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { optimizeBackpack(OptimizationInput(1, 1), iterations = iterations) }
            }
        }
    }

    @Test fun cancellationStopsAHotLoopWithNoImprovements() {
        val job = Job()
        var draws = 0
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int {
                if (++draws == 10) job.cancel()
                return 0
            }
        }
        assertThrows(CancellationException::class.java) {
            runBlocking { withContext(job) { optimizeBackpack(OptimizationInput(1, 2), random, 100000) } }
        }
        assertEquals(10, draws)
    }

    @Test fun cancellationAlsoStopsWeightGeneration() {
        val job = Job()
        var draws = 0
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int {
                draws++
                job.cancel()
                return 0
            }
        }
        assertThrows(CancellationException::class.java) {
            runBlocking { withContext(job) { optimizeBackpack(OptimizationInput(1, MAX_INPUT), random) } }
        }
        assertEquals(1, draws)
    }

    @Test fun reportedProgressImprovesAndMatchesTheFinalScore() = runBlocking {
        val progress = mutableListOf<Double>()
        val result = optimizeBackpack(OptimizationInput(3, 50), Random(42), 5000) { progress.add(it) }
        assertTrue(progress.isNotEmpty())
        assertTrue(progress.zipWithNext().all { (before, after) -> after < before })
        assertEquals(result.remaining, progress.last())
        assertTrue(result.remaining >= 0)
        assertEquals(3 - result.itemWeights.indices.sumOf {
            if (result.genome[it]) result.itemWeights[it].toDouble() else 0.0
        }, result.remaining)
    }

    @Test fun callbackCancellationPropagates() {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                optimizeBackpack(OptimizationInput(1, 10), Random(42), 2000) { throw CancellationException("stop") }
            }
        }
    }
}
