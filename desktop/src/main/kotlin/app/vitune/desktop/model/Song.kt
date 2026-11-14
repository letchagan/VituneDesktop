package app.vitune.desktop.model

import app.vitune.providers.innertube.Innertube

data class Song(
    val id: String,
    val title: String,
    val artists: String,
    val durationText: String?,
    val thumbnailSmall: String?,
    val thumbnailLarge: String?
) {
    /** Duration in milliseconds parsed from text like "3:45" or "1:02:10", or 0 if unknown. */
    val durationMs: Long
        get() = durationText
            ?.split(':')
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.fold(0L) { acc, part -> acc * 60 + part }
            ?.times(1000)
            ?: 0L
}

fun Innertube.SongItem.toSong(): Song? {
    val videoId = info?.endpoint?.videoId ?: return null
    return Song(
        id = videoId,
        title = info?.name ?: "Unknown title",
        artists = authors?.mapNotNull { it.name }?.joinToString(", ").orEmpty(),
        durationText = durationText,
        thumbnailSmall = thumbnail?.size(120),
        thumbnailLarge = thumbnail?.size(544)
    )
}
