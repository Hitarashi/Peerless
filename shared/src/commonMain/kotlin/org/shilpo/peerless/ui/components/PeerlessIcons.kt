package org.shilpo.peerless.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object PeerlessIcons {
    val Play: ImageVector get() = Icons.Rounded.PlayArrow
    val Pause: ImageVector get() = Icons.Rounded.Pause
    val SkipNext: ImageVector get() = Icons.Rounded.SkipNext
    val SkipPrevious: ImageVector get() = Icons.Rounded.SkipPrevious
    val Shuffle: ImageVector get() = Icons.Rounded.Shuffle
    val Repeat: ImageVector get() = Icons.Rounded.Repeat
    val RepeatOne: ImageVector get() = Icons.Rounded.RepeatOne
    val Search: ImageVector get() = Icons.Rounded.Search
    val Close: ImageVector get() = Icons.Rounded.Close
    val Settings: ImageVector get() = Icons.Rounded.Settings
    val ExpandMore: ImageVector get() = Icons.Rounded.ExpandMore
    val MusicNote: ImageVector get() = Icons.Rounded.MusicNote
    val LosslessWave: ImageVector get() = Icons.Rounded.GraphicEq
    val CloudDone: ImageVector get() = Icons.Rounded.CloudDone

    private var _home: ImageVector? = null
    val Home: ImageVector
        get() {
            if (_home != null) return _home!!
            _home = ImageVector.Builder(
                name = "KomikkuBrowseOutlined",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString(
                        "M 12 2 C 6.48 2 2 6.48 2 12 s 4.48 10 10 10 10 -4.48 10 -10 S 17.52 2 12 2 Z " +
                                "M 12 4 C 16.42 4 20 7.58 20 12 s -3.58 8 -8 8 -8 -3.58 -8 -8 3.58 -8 8 -8 Z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
                addPath(
                    pathData = PathParser().parsePathString(
                        "M 6.5 17.5 L 14.01 14.01 L 17.5 6.5 L 9.99 9.99 L 6.5 17.5 Z M 12 10.9 C 12.61 10.9 13.1 11.39 13.1 12 C 13.1 12.61 12.61 13.1 12 13.1 C 11.39 13.1 10.9 12.61 10.9 12 C 10.9 11.39 11.39 10.9 12 10.9 Z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
                addPath(
                    pathData = PathParser().parsePathString(
                        "M 12 10.9 C 12.61 10.9 13.1 11.39 13.1 12 C 13.1 12.61 12.61 13.1 12 13.1 C 11.39 13.1 10.9 12.61 10.9 12 C 10.9 11.39 11.39 10.9 12 10.9 Z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
            }.build()
            return _home!!
        }

    private var _homeFilled: ImageVector? = null
    val HomeFilled: ImageVector
        get() {
            if (_homeFilled != null) return _homeFilled!!
            _homeFilled = ImageVector.Builder(
                name = "KomikkuBrowseFilled",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString(
                        "M12,10.9c-0.61,0 -1.1,0.49 -1.1,1.1s0.49,1.1 1.1,1.1c0.61,0 1.1,-0.49 1.1,-1.1s-0.49,-1.1 -1.1,-1.1zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM14.19,14.19L6,18l3.81,-8.19L18,6l-3.81,8.19z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
            }.build()
            return _homeFilled!!
        }

    private var _library: ImageVector? = null
    val Library: ImageVector
        get() {
            if (_library != null) return _library!!
            _library = ImageVector.Builder(
                name = "KomikkuLibraryOutlined",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 1080f,
                viewportHeight = 1080f,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString(
                        "M 634.454 90.84 C 543.069 90.84 451.685 90.84 360.3 90.84 C 310.84 90.84 270.38 131.3 270.38 180.76 C 270.38 360.61 270.38 540.46 270.38 720.31 C 270.38 769.76 310.84 810.23 360.3 810.23 C 540.147 810.23 719.993 810.23 899.84 810.23 C 949.3 810.23 989.77 769.76 989.77 720.31 C 989.77 540.46 989.77 360.61 989.77 180.76 C 989.77 131.3 949.3 90.84 899.84 90.84 C 811.378 90.84 722.916 90.84 634.454 90.84 M 899.84 720.31 C 834.876 720.31 769.913 720.31 704.949 720.31 C 590.066 720.31 475.183 720.31 360.3 720.31 C 360.3 540.46 360.3 360.61 360.3 180.76 C 435.237 180.76 510.173 180.76 585.11 180.76 L 585.11 180.76 C 585.11 315.647 585.11 450.533 585.11 585.42 C 630.073 551.7 675.037 517.98 720 484.26 C 764.96 517.98 809.92 551.7 854.88 585.42 C 854.88 450.533 854.88 315.647 854.88 180.76 C 869.867 180.76 884.853 180.76 899.84 180.76 C 899.84 360.61 899.84 540.46 899.84 720.31"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
                addPath(
                    pathData = PathParser().parsePathString(
                        "M 180.45 270.69 C 180.45 270.69 90.53 270.69 90.53 270.69 C 90.53 270.69 90.53 900.15 90.53 900.15 C 90.53 949.61 131 990.08 180.45 990.08 C 180.45 990.08 809.92 990.08 809.92 990.08 C 809.92 990.08 809.92 900.15 809.92 900.15 C 809.92 900.15 180.45 900.15 180.45 900.15 C 180.45 900.15 180.45 270.69 180.45 270.69"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
            }.build()
            return _library!!
        }

    private var _libraryFilled: ImageVector? = null
    val LibraryFilled: ImageVector
        get() {
            if (_libraryFilled != null) return _libraryFilled!!
            _libraryFilled = ImageVector.Builder(
                name = "KomikkuLibraryFilled",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 1080f,
                viewportHeight = 1080f,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString(
                        "M 900 90 C 720 90 540 90 360 90 C 310.5 90 270 130.5 270 180 C 270 360 270 540 270 720 C 270 769.5 310.5 810 360 810 C 540 810 720 810 900 810 C 949.5 810 990 769.5 990 720 C 990 540 990 360 990 180 C 990 130.5 949.5 90 900 90 C 900 90 900 90 900 90 M 900 540 C 862.5 517.5 825 495 787.5 472.5 C 750 495 712.5 517.5 675 540 C 675 540 675 540 675 540 C 675 471.351 675 402.701 675 334.052 L 675 180 C 675 180 675 180 675 180 C 713.708 180 752.416 180 791.124 180 C 827.416 180 863.708 180 900 180 C 900 233.194 900 286.387 900 339.581 C 900 406.387 900 473.194 900 540 C 900 540 900 540 900 540"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
                addPath(
                    pathData = PathParser().parsePathString(
                        "M 180.45 270.69 C 180.45 270.69 90.53 270.69 90.53 270.69 C 90.53 270.69 90.53 900.15 90.53 900.15 C 90.53 949.61 131 990.08 180.45 990.08 C 180.45 990.08 809.92 990.08 809.92 990.08 C 809.92 990.08 809.92 900.15 809.92 900.15 C 809.92 900.15 180.45 900.15 180.45 900.15 C 180.45 900.15 180.45 270.69 180.45 270.69"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
            }.build()
            return _libraryFilled!!
        }
    val Queue: ImageVector get() = Icons.AutoMirrored.Rounded.QueueMusic
    val Lyrics: ImageVector get() = Icons.Rounded.Lyrics
    val SignalPath: ImageVector get() = Icons.Rounded.Tune
    val VolumeUp: ImageVector get() = Icons.AutoMirrored.Rounded.VolumeUp
    val VolumeMute: ImageVector get() = Icons.AutoMirrored.Rounded.VolumeMute
    val MoreVert: ImageVector get() = Icons.Rounded.MoreVert
    val Heart: ImageVector get() = Icons.Rounded.Favorite
    val Sparkle: ImageVector get() = Icons.Rounded.AutoAwesome
    val Compass: ImageVector get() = Icons.Rounded.Explore
    val BarChart: ImageVector get() = Icons.Rounded.BarChart
    val Database: ImageVector get() = Icons.Rounded.Storage
}
