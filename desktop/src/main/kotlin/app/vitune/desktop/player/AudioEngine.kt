package app.vitune.desktop.player

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

/**
 * Plays audio through VLC (via vlcj). This replaces ExoPlayer/Media3 from the Android app.
 *
 * Listener callbacks arrive on a VLC thread. Never call back into the player from
 * inside a callback; hop to another thread first (PlayerController does this).
 */
class AudioEngine {
    interface Listener {
        fun onPlaying()
        fun onPaused()
        fun onFinished()
        fun onError()
        fun onTime(ms: Long)
        fun onLength(ms: Long)
    }

    private val factory = MediaPlayerFactory("--no-video", "--quiet")
    private val player: MediaPlayer = factory.mediaPlayers().newMediaPlayer()

    @Volatile
    var listener: Listener? = null

    init {
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) { listener?.onPlaying() }
            override fun paused(mediaPlayer: MediaPlayer) { listener?.onPaused() }
            override fun finished(mediaPlayer: MediaPlayer) { listener?.onFinished() }
            override fun error(mediaPlayer: MediaPlayer) { listener?.onError() }
            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) { listener?.onTime(newTime) }
            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) { listener?.onLength(newLength) }
        })
    }

    fun play(url: String) {
        player.media().play(url)
    }

    fun pause() = player.controls().setPause(true)
    fun resume() = player.controls().setPause(false)
    fun stop() = player.controls().stop()
    fun seek(ms: Long) = player.controls().setTime(ms)

    /** 0–100. VLC only accepts this once playback has started, so the controller re-applies it. */
    fun setVolume(volume: Int) {
        player.audio().setVolume(volume)
    }

    fun release() {
        listener = null
        player.release()
        factory.release()
    }

    companion object {
        /** Finds the installed VLC (normally under Program Files). */
        fun isVlcAvailable(): Boolean = runCatching { NativeDiscovery().discover() }.getOrDefault(false)
    }
}
