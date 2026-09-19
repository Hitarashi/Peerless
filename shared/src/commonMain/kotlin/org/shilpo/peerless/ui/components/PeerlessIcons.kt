package org.shilpo.peerless.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
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
    val Menu: ImageVector get() = Icons.Rounded.Menu
    val MenuOpen: ImageVector get() = Icons.AutoMirrored.Rounded.MenuOpen
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
    val HeartBorder: ImageVector get() = Icons.Rounded.FavoriteBorder
    val Sparkle: ImageVector get() = Icons.Rounded.AutoAwesome
    val Compass: ImageVector get() = Icons.Rounded.Explore
    val BarChart: ImageVector get() = Icons.Rounded.BarChart
    val Database: ImageVector get() = Icons.Rounded.Storage
    val Person: ImageVector get() = Icons.Rounded.Person
    val Speed: ImageVector get() = Icons.Rounded.Speed
    val Dns: ImageVector get() = Icons.Rounded.Dns
    val Memory: ImageVector get() = Icons.Rounded.Memory
    val Devices: ImageVector get() = Icons.Rounded.Devices
    val CheckCircle: ImageVector get() = Icons.Rounded.CheckCircle
    val Warning: ImageVector get() = Icons.Rounded.Warning
    val ContentPaste: ImageVector get() = Icons.Rounded.ContentPaste
    val Key: ImageVector get() = Icons.Rounded.VpnKey
    val Logout: ImageVector get() = Icons.AutoMirrored.Rounded.Logout
    val OpenInNew: ImageVector get() = Icons.AutoMirrored.Rounded.OpenInNew

    private var _dolbyAtmos: ImageVector? = null
    val DolbyAtmos: ImageVector
        get() {
            if (_dolbyAtmos != null) return _dolbyAtmos!!
            _dolbyAtmos = ImageVector.Builder(
                name = "DolbyAtmos",
                defaultWidth = 20.dp,
                defaultHeight = 14.dp,
                viewportWidth = 269.2f,
                viewportHeight = 189.3f,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString(
                        "M269.2,0v189.3h-27.9c-52.1,0-94.6-42.5-94.6-94.6S189.2,0,241.3,0H269.2z M27.9,0c52.1,0,94.6,42.5,94.6,94.6s-42.5,94.6-94.6,94.6H0V0H27.9z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
            }.build()
            return _dolbyAtmos!!
        }

    private var _appleLogo: ImageVector? = null
    val AppleLogo: ImageVector
        get() {
            if (_appleLogo != null) return _appleLogo!!
            _appleLogo = ImageVector.Builder(
                name = "AppleLogo",
                defaultWidth = 11.dp,
                defaultHeight = 13.5.dp,
                viewportWidth = 41.5f,
                viewportHeight = 51f,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString(
                        "M40.2,17.4c-3.4,2.1-5.5,5.7-5.5,9.7c0,4.5,2.7,8.6,6.8,10.3c-0.8,2.6-2,5-3.5,7.2c-2.2,3.1-4.5,6.3-7.9,6.3s-4.4-2-8.4-2c-3.9,0-5.3,2.1-8.5,2.1s-5.4-2.9-7.9-6.5C2,39.5,0.1,33.7,0,27.6c0-9.9,6.4-15.2,12.8-15.2c3.4,0,6.2,2.2,8.3,2.2c2,0,5.2-2.3,9-2.3C34.1,12.2,37.9,14.1,40.2,17.4z M28.3,8.1C30,6.1,30.9,3.6,31,1c0-0.3,0-0.7-0.1-1c-2.9,0.3-5.6,1.7-7.5,3.9c-1.7,1.9-2.7,4.3-2.8,6.9c0,0.3,0,0.6,0.1,0.9c0.2,0,0.5,0.1,0.7,0.1C24.1,11.6,26.6,10.2,28.3,8.1z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
            }.build()
            return _appleLogo!!
        }

    private var _qobuzLogo: ImageVector? = null
    val QobuzLogo: ImageVector
        get() {
            if (_qobuzLogo != null) return _qobuzLogo!!
            _qobuzLogo = ImageVector.Builder(
                name = "QobuzLogo",
                defaultWidth = 35.dp,
                defaultHeight = 14.dp,
                viewportWidth = 725f,
                viewportHeight = 288f,
            ).apply {
                // z
                addPath(
                    pathData = PathParser().parsePathString(
                        "m 713.1,192.21 h -60.21 l 68.42,-91.49 A 18.18,18.18 0 0 0 707,71.84 v 0 h -73.38 c -7,0 -13.09,5.66 -13.09,12.65 a 12.64,12.64 0 0 0 12.65,12.64 2.81,2.81 0 0 0 0.29,0 h 59 l -68.27,91.21 a 18.22,18.22 0 0 0 14.53,29.18 h 74.37 c 6.43,0 11.64,-5.67 11.64,-12.66 0,-6.99 -5.21,-12.65 -11.64,-12.65 z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
                // u
                addPath(
                    pathData = PathParser().parsePathString(
                        "m 604.46,83.71 a 0.61,0.61 0 0 0 0,-0.14 12.65,12.65 0 1 0 -25.3,0 c 0,0.05 0,0.1 0,0.15 0,4.21 0,67.65 0,76.46 0,20.57 -12.46,31.69 -34.43,31.69 C 522.76,191.87 509,180.6 509,160 V 83.92 c 0,0 0,-0.1 0,-0.15 0,-0.05 0,-0.09 0,-0.14 v -0.14 0 a 12.64,12.64 0 0 0 -25.27,0 v 0 0.14 c 0,0.05 0,0.1 0,0.14 0,0.04 0,0.1 0,0.15 v 76.26 c 0,31.48 27.48,57 61.1,57 33.62,0 59.75,-25.51 59.75,-57 -0.06,-8.27 -0.11,-72.28 -0.12,-76.47 z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
                // b
                addPath(
                    pathData = PathParser().parsePathString(
                        "M 394.7,70.56 A 72.64,72.64 0 0 0 356.53,81.33 V 115 A 47.69,47.69 0 1 1 347,143.54 v 0 -130.89 a 12.65,12.65 0 1 0 -25.3,0 c 0,0 0,0.1 0,0.15 v 130.74 a 73,73 0 1 0 73,-73 z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
                // o
                addPath(
                    pathData = PathParser().parsePathString(
                        "m 234,70.65 a 73,73 0 1 0 73,73 73,73 0 0 0 -73,-73 z m 0,120.67 A 47.69,47.69 0 1 1 281.67,143.64 47.68,47.68 0 0 1 234,191.32 Z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
                // q
                addPath(
                    pathData = PathParser().parsePathString(
                        "m 73,216.59 a 72.59,72.59 0 0 0 38.16,-10.78 v -33.62 a 47.69,47.69 0 1 1 9.51,-28.58 v 0 130.74 c 0,0 0,0.09 0,0.14 a 12.66,12.66 0 1 0 25.31,0 c 0,-0.05 0,-0.09 0,-0.14 V 143.61 a 73,73 0 1 0 -73,73 z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
            }.build()
            return _qobuzLogo!!
        }

    private var _hiRes: ImageVector? = null
    val HiRes: ImageVector
        get() {
            if (_hiRes != null) return _hiRes!!
            _hiRes = ImageVector.Builder(
                name = "HiRes",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString(
                        "M240-300h24l29 64q3 8 10 12t15 4q15 0 23.5-12t2.5-26l-20-46q15-9 25.5-23.5T360-360v-40q0-25-17.5-42.5T300-460h-80q-17 0-28.5 11.5T180-420v170q0 13 8.5 21.5T210-220q13 0 21.5-8.5T240-250v-50Z" +
                                "m290 80q13 0 21.5-8.5T560-250q0-13-8.5-21.5T530-280h-70v-30h50q13 0 21.5-8.5T540-340q0-13-8.5-21.5T510-370h-50v-30h70q13 0 21.5-8.5T560-430q0-13-8.5-21.5T530-460h-90q-17 0-28.5 11.5T400-420v160q0 17 11.5 28.5T440-220h90Z" +
                                "m190-60h-90q-13 0-21.5 8.5T600-250q0 13 8.5 21.5T630-220h110q17 0 28.5-11.5T780-260v-60q0-17-11.5-28.5T740-360h-80v-40h90q13 0 21.5-8.5T780-430q0-13-8.5-21.5T750-460H640q-17 0-28.5 11.5T600-420v60q0 17 11.5 28.5T640-320h80v40Z" +
                                "m-480-80v-40h60v40h-60Z" +
                                "m120-230h60v60q0 13 8.5 21.5T450-500q13 0 21.5-8.5T480-530v-180q0-13-8.5-21.5T450-740q-13 0-21.5 8.5T420-710v60h-60v-60q0-13-8.5-21.5T330-740q-13 0-21.5 8.5T300-710v180q0 13 8.5 21.5T330-500q13 0 21.5-8.5T360-530v-60Z" +
                                "m220-120v180q0 13 8.5 21.5T610-500q13 0 21.5-8.5T640-530v-180q0-13-8.5-21.5T610-740q-13 0-21.5 8.5T580-710Z" +
                                "M120-120q-33 0-56.5-23.5T40-200v-560q0-33 23.5-56.5T120-840h720q33 0 56.5 23.5T920-760v560q0 33-23.5 56.5T840-120H120Z"
                    ).toNodes(),
                    fill = SolidColor(Color.White)
                )
            }.build()
            return _hiRes!!
        }
}
