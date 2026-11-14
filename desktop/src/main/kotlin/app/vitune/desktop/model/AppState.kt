package app.vitune.desktop.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class AppState(private val dataDir: File) {
    private val playlistsFile = File(dataDir, "playlists.json")
    private val favoritesFile = File(dataDir, "favorites.json")
    private val mutex = Mutex()

    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var favorites by mutableStateOf<Set<String>>(emptySet())
        private set
    var recentlyPlayed by mutableStateOf<List<Song>>(emptyList())
        private set

    suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                dataDir.mkdirs()
                // Load playlists
                if (playlistsFile.exists()) {
                    try {
                        val content = playlistsFile.readText()
                        // TODO: Parse JSON
                    } catch (e: Exception) {
                        println("Failed to load playlists: ${e.message}")
                    }
                }
                // Load favorites
                if (favoritesFile.exists()) {
                    try {
                        val content = favoritesFile.readText()
                        // TODO: Parse JSON
                    } catch (e: Exception) {
                        println("Failed to load favorites: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun save() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                // TODO: Save playlists and favorites to JSON
            }
        }
    }

    suspend fun addPlaylist(playlist: Playlist) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                playlists = playlists + playlist
            }
        }
        save()
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                playlists = playlists.map { if (it.id == playlist.id) playlist else it }
            }
        }
        save()
    }

    suspend fun deletePlaylist(playlistId: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                playlists = playlists.filter { it.id != playlistId }
            }
        }
        save()
    }

    suspend fun addToFavorites(songId: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                favorites = favorites + songId
            }
        }
        save()
    }

    suspend fun removeFromFavorites(songId: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                favorites = favorites - songId
            }
        }
        save()
    }

    suspend fun addToRecentlyPlayed(song: Song) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                recentlyPlayed = (listOf(song) + recentlyPlayed).take(100)
            }
        }
    }

    fun isFavorite(songId: String): Boolean = favorites.contains(songId)
}
