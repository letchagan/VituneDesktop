package app.vitune.desktop.ui

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import app.vitune.desktop.model.MusicRepository
import app.vitune.desktop.model.Song
import app.vitune.desktop.player.PlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun SearchPanel(controller: PlayerController, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>("Search for a song, artist or album to start listening.") }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun search() {
        val text = query.trim()
        if (text.isEmpty()) return
        searchJob?.cancel()
        searchJob = scope.launch {
            isSearching = true
            message = null
            MusicRepository.searchSongs(text)
                .onSuccess { songs ->
                    results = songs
                    if (songs.isEmpty()) message = "No songs found for “$text”."
                }
                .onFailure { e ->
                    results = emptyList()
                    message = "Search failed: ${e.message ?: "check your internet connection"}"
                }
            isSearching = false
        }
    }

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search YouTube Music") },
            leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                        search()
                        true
                    } else {
                        false
                    }
                }
        )

        Box(Modifier.fillMaxSize()) {
            when {
                isSearching -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                message != null -> Text(
                    text = message!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(results, key = { _, song -> song.id }) { _, song ->
                        SongRow(
                            song = song,
                            isCurrent = controller.current?.id == song.id,
                            onClick = { controller.playRadio(song) },
                            menuItems = {
                                listOf(
                                    ContextMenuItem("Play") { controller.playRadio(song) },
                                    ContextMenuItem("Play next") { controller.playNext(song) },
                                    ContextMenuItem("Add to queue") { controller.addToQueue(song) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
