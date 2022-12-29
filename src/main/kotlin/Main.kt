import kotlin.random.Random
import kotlin.random.nextUInt


fun score(genome: Array<Boolean>, size: Int, itemWeights: List<Float>): Double {
    val sumOf = (genome.indices).sumOf { itemWeights[it] * if (genome[it]) 1.0 else 0.0 }
    return size - sumOf
}

fun mutate(oldGenome: Array<Boolean>): Array<Boolean> {
    val newGenome = oldGenome.clone()
    val index = Random.nextUInt(oldGenome.size.toUInt()).toInt()
    newGenome[index] = oldGenome[index].not()
    return newGenome
}

fun main(args: Array<String>) {
    var backpackSize = 10
    var noItems = 10000
    var itemWeights = (0 until noItems).map { Random.nextFloat() }
    var genome = (0 until noItems).map { false }.toTypedArray()

    (1 until 10000000).forEach { _ ->
        val newGenome = mutate(genome)
        val oldScore = score(genome, backpackSize, itemWeights)
        val newScore = score(newGenome, backpackSize, itemWeights)
        if (newScore < oldScore && (newScore < 0).not()) {
            genome = newGenome
            println(newScore)
        }
    }
    println(itemWeights)
    println(genome.toList())
}