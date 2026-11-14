package app.vitune.desktop.ui

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vitune.desktop.player.PlayerController

@Composable
fun QueuePanel(controller: PlayerController, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    // Keep the playing song in view as the queue advances
    LaunchedEffect(controller.currentIndex) {
        if (controller.currentIndex >= 0) listState.animateScrollToItem(controller.currentIndex)
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column {
            Text(
                text = "Up next",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp)
            )

            if (controller.queue.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        text = "Songs you play will appear here, followed by similar tracks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(controller.queue, key = { _, song -> song.id }) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = index == controller.currentIndex,
                            onClick = { controller.playAt(index) },
                            menuItems = {
                                if (index == controller.currentIndex) emptyList()
                                else listOf(
                                    ContextMenuItem("Play") { controller.playAt(index) },
                                    ContextMenuItem("Remove from queue") { controller.removeFromQueue(index) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
