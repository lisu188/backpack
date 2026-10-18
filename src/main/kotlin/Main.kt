import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.JavaFx)
    private var optimizationJob: Job? = null
    private var stopping = false

    override fun start(primaryStage: Stage) {
        val mainPane = VBox(10.0)
        mainPane.alignment = Pos.CENTER
        mainPane.padding = Insets(10.0)

        val backpackSizeLabel = Label("Backpack size:")
        val backpackSizeField = TextField("10")
        val noItemsLabel = Label("Number of items:")
        val noItemsField = TextField("10000")
        val startButton = Button("Start")
        val outputArea = TextArea()
        outputArea.isEditable = false

        startButton.setOnAction {
            if (optimizationJob != null) {
                optimizationJob?.cancel()
                startButton.text = "Cancelling..."
                startButton.isDisable = true
                return@setOnAction
            }

            // Read and validate controls on the JavaFX thread before starting work.
            val input = parseOptimizationInput(backpackSizeField.text, noItemsField.text)
            outputArea.clear()
            if (input == null) {
                outputArea.text = "Capacity and item count must be whole numbers from 1 to $MAX_INPUT."
                return@setOnAction
            }
            backpackSizeField.isDisable = true
            noItemsField.isDisable = true
            startButton.text = "Cancel"
            optimizationJob = appScope.launch {
                try {
                    val result = withContext(Dispatchers.Default) {
                        optimizeBackpack(input, onImprovement = { remaining ->
                            withContext(Dispatchers.JavaFx) {
                                outputArea.appendText("Score: $remaining\n")
                            }
                        })
                    }
                    outputArea.appendText("\nItem Weights: ${result.itemWeights}\n")
                    outputArea.appendText("\nBest Genome: ${result.genome}\n")
                } catch (cancelled: CancellationException) {
                    if (!stopping) outputArea.appendText("\nCancelled.\n")
                } catch (error: Exception) {
                    if (!stopping) outputArea.appendText("\nSearch failed: ${error.message}\n")
                } finally {
                    optimizationJob = null
                    if (!stopping) {
                        backpackSizeField.isDisable = false
                        noItemsField.isDisable = false
                        startButton.text = "Start"
                        startButton.isDisable = false
                    }
                }
            }
        }

        mainPane.children.addAll(
            backpackSizeLabel, backpackSizeField, noItemsLabel, noItemsField,
            startButton, outputArea
        )
        primaryStage.scene = Scene(mainPane)
        primaryStage.title = "Backpack Optimization"
        primaryStage.show()
    }

    override fun stop() {
        stopping = true
        appScope.cancel()
    }
}

fun main() {
    Application.launch(MyApp::class.java)
}
