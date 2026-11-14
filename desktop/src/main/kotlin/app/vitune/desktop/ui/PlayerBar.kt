package app.vitune.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vitune.desktop.player.PlayerController
import app.vitune.desktop.player.RepeatMode

@Composable
fun PlayerBar(controller: PlayerController, isFavorite: Boolean = false, onFavoriteToggle: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val song = controller.current

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Now playing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Artwork(song?.thumbnailLarge, 60.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (song != null && song.artists.isNotEmpty()) {
                        Text(
                            text = song.artists,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (onFavoriteToggle != null) {
                    IconButton(onClick = onFavoriteToggle, enabled = song != null) {
                        Icon(
                            AppIcons.Favorite,
                            contentDescription = "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Transport controls and seek bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1.4f).widthIn(max = 640.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = controller::toggleShuffle,
                        enabled = song != null
                    ) {
                        Icon(
                            AppIcons.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (controller.isShuffled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = controller::previous, enabled = song != null) {
                        Icon(AppIcons.SkipPrevious, contentDescription = "Previous")
                    }
                    FilledIconButton(
                        onClick = controller::togglePlayPause,
                        enabled = song != null,
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (controller.isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (controller.isPlaying) AppIcons.Pause else AppIcons.Play,
                                contentDescription = if (controller.isPlaying) "Pause" else "Play"
                            )
                        }
                    }
                    IconButton(
                        onClick = controller::next,
                        enabled = controller.currentIndex < controller.queue.lastIndex
                    ) {
                        Icon(AppIcons.SkipNext, contentDescription = "Next")
                    }
                    IconButton(
                        onClick = controller::cycleRepeatMode,
                        enabled = song != null
                    ) {
                        Icon(
                            AppIcons.Repeat,
                            contentDescription = "Repeat",
                            tint = if (controller.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SeekBar(controller)
            }

            // Volume
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = AppIcons.Volume,
                    contentDescription = "Volume",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = controller.volume.toFloat(),
                    onValueChange = { controller.changeVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

@Composable
private fun SeekBar(controller: PlayerController) {
    // While dragging, show the drag position instead of the live position
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    val duration = controller.durationMs
    val position = dragPosition?.toLong() ?: controller.positionMs

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TimeLabel(formatTime(position))
        Slider(
            value = position.coerceIn(0, duration.coerceAtLeast(1)).toFloat(),
            onValueChange = { dragPosition = it },
            onValueChangeFinished = {
                dragPosition?.let { controller.seekTo(it.toLong()) }
                dragPosition = null
            },
            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
            enabled = duration > 0 && !controller.isLoading,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        TimeLabel(if (duration > 0) formatTime(duration) else "–:––")
    }
}

@Composable
private fun TimeLabel(text: String) {
    Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
