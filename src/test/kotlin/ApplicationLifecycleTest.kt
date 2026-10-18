import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import java.util.concurrent.TimeUnit

class ApplicationLifecycleTest {
    companion object {
        @JvmStatic @BeforeAll fun startToolkit() {
            val ready = CompletableFuture<Unit>()
            Platform.startup {
                Platform.setImplicitExit(false)
                ready.complete(Unit)
            }
            ready.get(10, TimeUnit.SECONDS)
        }

        @JvmStatic @AfterAll fun stopToolkit() {
            Platform.exit()
        }

        private fun <T> onFx(action: () -> T): T {
            val result = CompletableFuture<T>()
            Platform.runLater {
                try { result.complete(action()) } catch (error: Throwable) { result.completeExceptionally(error) }
            }
            return result.get(10, TimeUnit.SECONDS)
        }
    }

    private data class Ui(val app: MyApp, val stage: Stage, val button: Button,
                          val fields: List<TextField>, val output: TextArea)

    private fun createApp(): Ui = onFx {
        val app = MyApp()
        val stage = Stage()
        app.start(stage)
        val children = (stage.scene.root as VBox).children
        Ui(app, stage, children.filterIsInstance<Button>().single(),
           children.filterIsInstance<TextField>(), children.filterIsInstance<TextArea>().single())
    }

    private fun close(ui: Ui) = onFx { ui.app.stop(); ui.stage.close() }

    private fun capture(ui: Ui) = onFx {
        val image = ui.stage.scene.snapshot(null)
        val pixels = image.pixelReader
        val buffered = BufferedImage(image.width.toInt(), image.height.toInt(), BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until buffered.height) for (x in 0 until buffered.width) {
            buffered.setRGB(x, y, pixels.getArgb(x, y))
        }
        val output = File("build/reports/ui/backpack-cancelled.png")
        check(output.parentFile.mkdirs() || output.parentFile.isDirectory)
        check(ImageIO.write(buffered, "png", output))
    }

    private fun drainCancelledLaunch() {
        // The queued launch completes, then its queued UI cleanup executes.
        onFx { }
        onFx { }
    }

    @Test fun cancellingBeforeLaunchBodyStartsReleasesControlsAndAllowsRestart() {
        val ui = createApp()
        try {
            repeat(2) {
                onFx {
                    assertEquals("Start", ui.button.text)
                    assertFalse(ui.button.isDisable)
                    assertTrue(ui.fields.none { it.isDisable })
                    ui.button.fire()
                    assertEquals("Cancel", ui.button.text)
                    assertTrue(ui.fields.all { it.isDisable })
                    // Both events occur before the dispatched search body can start.
                    ui.button.fire()
                    assertEquals("Cancelling...", ui.button.text)
                    assertTrue(ui.button.isDisable)
                }
                drainCancelledLaunch()
                onFx {
                    assertEquals("Start", ui.button.text)
                    assertFalse(ui.button.isDisable)
                    assertTrue(ui.fields.none { it.isDisable })
                    assertTrue(ui.output.text.contains("Cancelled."))
                }
            }
            capture(ui)
        } finally { close(ui) }
    }

    @Test fun invalidInputDoesNotStartOrDisableTheUi() {
        val ui = createApp()
        try {
            onFx {
                ui.fields[0].text = "0"
                ui.button.fire()
                assertEquals("Start", ui.button.text)
                assertFalse(ui.button.isDisable)
                assertTrue(ui.fields.none { it.isDisable })
                assertTrue(ui.output.text.contains("whole numbers"))
            }
        } finally { close(ui) }
    }
}
