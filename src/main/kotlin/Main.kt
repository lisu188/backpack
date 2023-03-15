import javafx.application.Application
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.random.nextUInt

class MyApp : Application() {
    private val backpackSizeProperty = SimpleDoubleProperty(10.0)
    private val noItemsProperty = SimpleDoubleProperty(10000.0)

    override fun start(primaryStage: Stage) {
        val mainPane = VBox(10.0)
        mainPane.alignment = Pos.CENTER
        mainPane.padding = Insets(10.0)

        val backpackSizeLabel = Label("Backpack size:")
        val backpackSizeField = TextField("10")
        backpackSizeField.textProperty().addListener { _, _, newValue ->
            backpackSizeProperty.set(newValue.toDoubleOrNull() ?: 10.0)
        }

        val noItemsLabel = Label("Number of items:")
        val noItemsField = TextField("10000")
        noItemsField.textProperty().addListener { _, _, newValue ->
            noItemsProperty.set(newValue.toDoubleOrNull() ?: 10000.0)
        }

        val startButton = Button("Start")
        val outputArea = TextArea()

        startButton.setOnAction {
            outputArea.clear()
            GlobalScope.launch(Dispatchers.IO) {
                runOptimization(
                    backpackSizeProperty.get().toInt(),
                    noItemsProperty.get().toInt(),
                    outputArea
                )
            }
        }

        mainPane.children.addAll(
            backpackSizeLabel,
            backpackSizeField,
            noItemsLabel,
            noItemsField,
            startButton,
            outputArea
        )

        val scene = Scene(mainPane)
        primaryStage.scene = scene
        primaryStage.title = "Backpack Optimization"
        primaryStage.show()
    }
}

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


suspend fun runOptimization(backpackSize: Int, noItems: Int, outputArea: TextArea) {
    val itemWeights = (0 until noItems).map { Random.nextFloat() }
    var genome = (0 until noItems).map { false }.toTypedArray()

    (1 until 10000000).forEach { _ ->
        val newGenome = mutate(genome)
        val oldScore = score(genome, backpackSize, itemWeights)
        val newScore = score(newGenome, backpackSize, itemWeights)
        if (newScore < oldScore && (newScore < 0).not()) {
            genome = newGenome
            withContext(Dispatchers.JavaFx) {
                outputArea.appendText("Score: $newScore\n")
            }
        }
    }
    withContext(Dispatchers.JavaFx) {
        outputArea.appendText("\nItem Weights: $itemWeights\n")
        outputArea.appendText("\nBest Genome: ${genome.toList()}\n")
    }
}

fun main() {
    Application.launch(MyApp::class.java)
}

