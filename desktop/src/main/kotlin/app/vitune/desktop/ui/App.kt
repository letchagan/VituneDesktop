package app.vitune.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vitune.desktop.model.AppState
import app.vitune.desktop.model.Playlist
import app.vitune.desktop.player.PlayerController
import java.awt.Desktop
import java.net.URI

@Composable
fun App(
    controller: PlayerController,
    appState: AppState,
    setupStatus: String?,
    setupFailed: Boolean
) {
    val scope = rememberCoroutineScope()
    var currentNavItem by remember { mutableStateOf(NavigationItem.HOME) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (setupStatus != null) {
                Banner(
                    text = setupStatus,
                    isError = setupFailed,
                    showSpinner = !setupFailed
                )
            }

            Row(Modifier.weight(1f).fillMaxWidth()) {
                // Sidebar
                Sidebar(
                    currentNavItem = currentNavItem,
                    onNavItemSelected = { item ->
                        currentNavItem = item
                        selectedPlaylist = null
                    },
                    playlists = appState.playlists,
                    onPlaylistSelected = { playlist ->
                        selectedPlaylist = playlist
                        currentNavItem = NavigationItem.PLAYLISTS
                    },
                    onCreatePlaylist = { name, description ->
                        scope.launch {
                            val newPlaylist = Playlist.newPlaylist(name, description)
                            appState.addPlaylist(newPlaylist)
                        }
                    }
                )

                // Main content area
                when {
                    selectedPlaylist != null -> {
                        PlaylistPanel(
                            playlist = selectedPlaylist!!,
                            controller = controller,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    currentNavItem == NavigationItem.HOME -> {
                        HomePanel(
                            controller = controller,
                            recentlyPlayed = appState.recentlyPlayed,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    currentNavItem == NavigationItem.LIKED -> {
                        LikedSongsPanel(
                            appState = appState,
                            controller = controller,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    else -> {
                        SearchPanel(controller, Modifier.weight(1f).fillMaxHeight())
                    }
                }

                // Queue panel
                QueuePanel(controller, Modifier.width(360.dp).fillMaxHeight())
            }

            controller.error?.let { message ->
                Banner(text = message, isError = true, onDismiss = { controller.error = null })
            }

            PlayerBar(
                controller = controller,
                isFavorite = controller.current?.let { appState.isFavorite(it.id) } ?: false,
                onFavoriteToggle = controller.current?.let { song ->
                    {
                        if (appState.isFavorite(song.id)) {
                            // TODO: Handle favorite removal
                        } else {
                            // TODO: Handle favorite addition
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun Banner(
    text: String,
    isError: Boolean,
    showSpinner: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            if (showSpinner) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp)
            )
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(AppIcons.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun MissingVlcScreen() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 520.dp).padding(32.dp)
            ) {
                Text(
                    text = "VLC media player is needed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "ViTune Desktop plays audio through VLC. Install the 64-bit version of VLC, " +
                        "then restart this app.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    runCatching { Desktop.getDesktop().browse(URI("https://www.videolan.org/vlc/")) }
                }) {
                    Text("Download VLC")
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Or run:  winget install VideoLAN.VLC",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}
