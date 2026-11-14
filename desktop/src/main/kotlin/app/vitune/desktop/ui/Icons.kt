package app.vitune.desktop.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** A few Material icons, defined inline to avoid the large icons dependency. */
object AppIcons {
    val Play = icon("Play", "M8,5v14l11,-7z")
    val Pause = icon("Pause", "M6,19h4V5H6v14zM14,5v14h4V5h-4z")
    val SkipNext = icon("SkipNext", "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z")
    val SkipPrevious = icon("SkipPrevious", "M6,6h2v12H6zM9.5,12l8.5,6V6z")
    val Search = icon(
        "Search",
        "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 " +
            "5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14" +
            "C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"
    )
    val Volume = icon(
        "Volume",
        "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02z" +
            "M14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77" +
            "s-2.99,-7.86 -7,-8.77z"
    )
    val MusicNote = icon(
        "MusicNote",
        "M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z"
    )
    val Close = icon(
        "Close",
        "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"
    )
    val Home = icon(
        "Home",
        "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z"
    )
    val LibraryMusic = icon(
        "LibraryMusic",
        "M15,6H3v2h12V6zm0,4H3v2h12V10zM3,16h8v-2H3V16zM17,6v8.18C16.69,14.07 16.08,14 15.5,14c-1.66,0-3,1.34-3,3s1.34,3 3,3 3,-1.34 3,-3V8h3V6h-4z"
    )
    val PlaylistMusic = icon(
        "PlaylistMusic",
        "M15,6H3v2h12V6zm0,4H3v2h12V10zM3,16h8v-2H3V16zM17,6v8.18C16.69,14.07 16.08,14 15.5,14c-1.66,0-3,1.34-3,3s1.34,3 3,3 3,-1.34 3,-3V8h3V6h-4z"
    )
    val Favorite = icon(
        "Favorite",
        "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78-3.4,6.86-8.55,11.54L12,21.35z"
    )
    val Shuffle = icon(
        "Shuffle",
        "M10.59,9.38L6.5,5.5L8,4l6,6l-6,6l-1.5,-1.5l4.09,-4.09L4,13.5H2v-3h2L10.59,9.38zM14.5,4L16,5.5l-4.09,4.09L20,9.5h2v3h-2l-8.59,-0.12L16,17l-1.5,1.5L8,12L14.5,4z"
    )
    val Repeat = icon(
        "Repeat",
        "M7,7H5.5C4.12,7 3,8.12 3,9.5V14c0,1.38 1.12,2.5 2.5,2.5H7v3l4,-4l-4,-4v3H5.5V9.5H7V7zM19.5,11H17v-3l-4,4l4,4v-3h2.5c1.38,0 2.5,-1.12 2.5,-2.5V10C21,8.62 19.88,7.5 18.5,7.5h-2v3h2V11z"
    )

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.Black)
        ).build()
}
