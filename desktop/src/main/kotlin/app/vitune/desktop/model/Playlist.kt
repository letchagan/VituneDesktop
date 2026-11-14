package app.vitune.desktop.model

import java.time.LocalDateTime

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val songs: List<Song> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val thumbnailUrl: String? = null
) {
    companion object {
        fun newPlaylist(name: String, description: String = ""): Playlist {
            return Playlist(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                songs = emptyList(),
                createdAt = LocalDateTime.now()
            )
        }
    }

    fun withSongs(songs: List<Song>) = copy(songs = songs)
    fun withName(name: String) = copy(name = name)
    fun withDescription(description: String) = copy(description = description)
}
