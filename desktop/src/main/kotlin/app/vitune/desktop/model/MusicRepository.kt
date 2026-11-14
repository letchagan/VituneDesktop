package app.vitune.desktop.model

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.innertube.models.bodies.SearchBody
import app.vitune.providers.innertube.requests.nextPage
import app.vitune.providers.innertube.requests.searchPage
import app.vitune.providers.innertube.utils.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thin wrapper around ViTune's YouTube Music (Innertube) provider. */
object MusicRepository {
    suspend fun searchSongs(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        Innertube.searchPage(
            body = SearchBody(query = query, params = Innertube.SearchFilter.Song.value),
            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
        )
            ?.map { page -> page?.items.orEmpty().mapNotNull { it.toSong() }.distinctBy { it.id } }
            ?: Result.success(emptyList())
    }

    /** Songs YouTube Music would play after [videoId] (its "radio"/autoplay list). */
    suspend fun radio(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        Innertube.nextPage(NextBody(videoId = videoId))
            ?.getOrNull()
            ?.itemsPage
            ?.items
            .orEmpty()
            .mapNotNull { it.toSong() }
            .distinctBy { it.id }
    }
}
