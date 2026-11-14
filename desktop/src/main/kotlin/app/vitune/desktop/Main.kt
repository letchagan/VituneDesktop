package app.vitune.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.vitune.desktop.model.AppState
import app.vitune.desktop.player.AudioEngine
import app.vitune.desktop.player.PlayerController
import app.vitune.desktop.player.YtDlp
import app.vitune.desktop.ui.App
import app.vitune.desktop.ui.AppTheme
import app.vitune.desktop.ui.MissingVlcScreen
import kotlinx.coroutines.CancellationException
import java.nio.file.Path
import kotlin.io.path.Path

/** %LOCALAPPDATA%\ViTuneDesktop on Windows, ~/.vitune-desktop elsewhere. */
private val dataDir: Path =
    System.getenv("LOCALAPPDATA")?.let { Path(it, "ViTuneDesktop") }
        ?: Path(System.getProperty("user.home"), ".vitune-desktop")

private fun Path.toFile() = java.io.File(this.toString())

fun main() {
    val vlcAvailable = AudioEngine.isVlcAvailable()

    application {
        val windowState = rememberWindowState(width = 1180.dp, height = 760.dp)

        if (!vlcAvailable) {
            Window(onCloseRequest = ::exitApplication, title = "ViTune", state = windowState) {
                AppTheme { MissingVlcScreen() }
            }
            return@application
        }

        val scope = rememberCoroutineScope()
        val ytDlp = remember { YtDlp(dataDir) }
        val engine = remember { AudioEngine() }
        val controller = remember { PlayerController(scope, engine, ytDlp) }
        val appState = remember { AppState(dataDir.toFile()) }

        var setupStatus by remember { mutableStateOf<String?>("Getting ready…") }
        var setupFailed by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            try {
                ytDlp.prepare { status -> setupStatus = status }
                appState.load()
                setupStatus = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setupFailed = true
                setupStatus = "Couldn't set up yt-dlp: ${e.message}. Check your connection and restart the app."
            }
        }

        DisposableEffect(Unit) {
            onDispose { engine.release() }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = controller.current?.let { "${it.title} – ViTune" } ?: "ViTune",
            state = windowState,
            onKeyEvent = { event ->
                // Ctrl+P play/pause, Ctrl+→ next, Ctrl+← previous
                val action: (() -> Unit)? =
                    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) null
                    else when (event.key) {
                        Key.P -> controller::togglePlayPause
                        Key.DirectionRight -> controller::next
                        Key.DirectionLeft -> controller::previous
                        else -> null
                    }
                action?.invoke()
                action != null
            }
        ) {
            AppTheme {
                App(controller, appState, setupStatus, setupFailed)
            }
        }
    }
}
