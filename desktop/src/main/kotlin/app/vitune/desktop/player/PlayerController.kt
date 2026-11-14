package app.vitune.desktop.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.vitune.desktop.model.MusicRepository
import app.vitune.desktop.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF, ONE, ALL
}

/**
 * Owns the play queue and playback state. All public functions and state are meant
 * to be used from the UI thread; VLC callbacks are forwarded there.
 */
class PlayerController(
    private val scope: CoroutineScope,
    private val engine: AudioEngine,
    private val ytDlp: YtDlp
) {
    var queue by mutableStateOf<List<Song>>(emptyList())
        private set
    var currentIndex by mutableStateOf(-1)
        private set
    val current: Song? get() = queue.getOrNull(currentIndex)

    var isPlaying by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var volume by mutableStateOf(80)
        private set
    var error by mutableStateOf<String?>(null)
    var isShuffled by mutableStateOf(false)
        private set
    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set

    private var loadJob: Job? = null
    private var radioJob: Job? = null
    private var reachedEnd = false

    init {
        engine.listener = object : AudioEngine.Listener {
            override fun onPlaying() {
                onUi {
                    isPlaying = true
                    isLoading = false
                    engine.setVolume(volume)
                }
            }

            override fun onPaused() {
                onUi { isPlaying = false }
            }

            override fun onFinished() {
                onUi { onTrackEnded() }
            }

            override fun onError() {
                onUi {
                    val failedIndex = currentIndex
                    error = "Couldn't play “${current?.title}”, skipping."
                    isLoading = false
                    isPlaying = false
                    delay(1500)
                    // Don't skip if the user picked another song in the meantime
                    if (currentIndex == failedIndex && !isPlaying && !isLoading) onTrackEnded()
                }
            }

            override fun onTime(ms: Long) {
                onUi { positionMs = ms }
            }

            override fun onLength(ms: Long) {
                onUi { if (ms > 0) durationMs = ms }
            }
        }
    }

    /** Plays [song] and fills the queue with YouTube Music's radio for it, like tapping a song in ViTune. */
    fun playRadio(song: Song) {
        queue = listOf(song)
        currentIndex = 0
        startCurrent()

        radioJob?.cancel()
        radioJob = scope.launch {
            val related = runCatching { MusicRepository.radio(song.id) }.getOrDefault(emptyList())
            // Only append if the user hasn't started something else in the meantime
            if (queue.firstOrNull()?.id == song.id) {
                val known = queue.map { it.id }.toMutableSet()
                queue = queue + related.filter { known.add(it.id) }
                prefetchNext()
            }
        }
    }

    fun playAt(index: Int) {
        if (index !in queue.indices) return
        currentIndex = index
        startCurrent()
    }

    fun playNext(song: Song) {
        val playing = current ?: return playRadio(song)
        if (playing.id == song.id) return
        val withoutSong = queue.filter { it.id != song.id }
        val insertAt = withoutSong.indexOf(playing) + 1
        queue = withoutSong.toMutableList().apply { add(insertAt, song) }
        currentIndex = queue.indexOf(playing)
    }

    fun addToQueue(song: Song) {
        if (current == null) return playRadio(song)
        if (queue.none { it.id == song.id }) queue = queue + song
    }

    fun removeFromQueue(index: Int) {
        if (index !in queue.indices || index == currentIndex) return
        queue = queue.toMutableList().apply { removeAt(index) }
        if (index < currentIndex) currentIndex--
    }

    fun togglePlayPause() {
        when {
            current == null || isLoading -> Unit
            reachedEnd -> startCurrent()
            isPlaying -> engine.pause()
            else -> engine.resume()
        }
    }

    fun next() {
        if (currentIndex < queue.lastIndex) {
            currentIndex++
            startCurrent()
        }
    }

    fun previous() {
        if (positionMs > 3_000 || currentIndex <= 0) {
            seekTo(0)
        } else {
            currentIndex--
            startCurrent()
        }
    }

    fun seekTo(ms: Long) {
        engine.seek(ms)
        positionMs = ms
    }

    fun changeVolume(value: Int) {
        volume = value.coerceIn(0, 100)
        engine.setVolume(volume)
    }

    fun toggleShuffle() {
        isShuffled = !isShuffled
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    private fun onTrackEnded() {
        when {
            repeatMode == RepeatMode.ONE -> {
                seekTo(0)
                engine.resume()
            }
            currentIndex < queue.lastIndex -> next()
            repeatMode == RepeatMode.ALL -> {
                currentIndex = 0
                startCurrent()
            }
            else -> {
                isPlaying = false
                reachedEnd = true
            }
        }
    }

    private fun startCurrent() {
        val song = current ?: return
        loadJob?.cancel()
        engine.stop()

        reachedEnd = false
        isLoading = true
        isPlaying = false
        positionMs = 0
        durationMs = song.durationMs
        error = null

        loadJob = scope.launch {
            try {
                val url = ytDlp.streamUrl(song.id)
                engine.play(url)
                prefetchNext()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isLoading = false
                error = "Couldn't load “${song.title}”: ${e.message}"
            }
        }
    }

    /** Resolves the next song's stream URL early so skipping or autoplay starts faster. */
    private fun prefetchNext() {
        val upcoming = queue.getOrNull(currentIndex + 1) ?: return
        scope.launch { runCatching { ytDlp.streamUrl(upcoming.id) } }
    }

    private fun onUi(block: suspend () -> Unit) {
        scope.launch(Dispatchers.Main) { block() }
    }
}
