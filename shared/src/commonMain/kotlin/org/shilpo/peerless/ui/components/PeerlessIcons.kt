package org.shilpo.peerless.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import peerless.shared.generated.resources.*

object PeerlessIcons {
    val Play: DrawableResource get() = Res.drawable.peerless_play_arrow
    val SkipNext: DrawableResource get() = Res.drawable.peerless_skip_next
    val SkipPrevious: DrawableResource get() = Res.drawable.peerless_skip_previous
    val Shuffle: DrawableResource get() = Res.drawable.peerless_shuffle
    val Repeat: DrawableResource get() = Res.drawable.peerless_repeat
    val RepeatOne: DrawableResource get() = Res.drawable.peerless_repeat_one
    val Search: DrawableResource get() = Res.drawable.peerless_search
    val Close: DrawableResource get() = Res.drawable.peerless_close
    val Settings: DrawableResource get() = Res.drawable.peerless_settings
    val ExpandMore: DrawableResource get() = Res.drawable.peerless_expand_more
    val MusicNote: DrawableResource get() = Res.drawable.peerless_music_note
    val LosslessWave: DrawableResource get() = Res.drawable.peerless_graphic_eq
    val CloudDone: DrawableResource get() = Res.drawable.peerless_cloud_done
    val Home: DrawableResource get() = Res.drawable.peerless_explore
    val HomeFilled: DrawableResource get() = Res.drawable.peerless_explore_filled
    val Library: DrawableResource get() = Res.drawable.peerless_library_music
    val LibraryFilled: DrawableResource get() = Res.drawable.peerless_library_music_filled
    val Queue: DrawableResource get() = Res.drawable.peerless_queue_music
    val Lyrics: DrawableResource get() = Res.drawable.peerless_lyrics
    val SignalPath: DrawableResource get() = Res.drawable.peerless_tune
    val MoreVert: DrawableResource get() = Res.drawable.peerless_more_vert
    val Heart: DrawableResource get() = Res.drawable.peerless_favorite_filled
    val HeartBorder: DrawableResource get() = Res.drawable.peerless_favorite
    val Sparkle: DrawableResource get() = Res.drawable.peerless_stars
    val Compass: DrawableResource get() = Res.drawable.peerless_explore
    val Person: DrawableResource get() = Res.drawable.peerless_person
    val Speed: DrawableResource get() = Res.drawable.peerless_speed
    val Dns: DrawableResource get() = Res.drawable.peerless_dns
    val Memory: DrawableResource get() = Res.drawable.peerless_memory
    val Devices: DrawableResource get() = Res.drawable.peerless_devices
    val CheckCircle: DrawableResource get() = Res.drawable.peerless_check_circle
    val Warning: DrawableResource get() = Res.drawable.peerless_warning
    val ContentPaste: DrawableResource get() = Res.drawable.peerless_content_paste
    val Key: DrawableResource get() = Res.drawable.peerless_vpn_key
    val Logout: DrawableResource get() = Res.drawable.peerless_logout
    val OpenInNew: DrawableResource get() = Res.drawable.peerless_open_in_new
    val Lock: DrawableResource get() = Res.drawable.peerless_lock
    val Visibility: DrawableResource get() = Res.drawable.peerless_visibility
    val VisibilityOff: DrawableResource get() = Res.drawable.peerless_visibility_off

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


@Composable
fun PeerlessIcon(
    icon: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val needsRtlMirror = LocalLayoutDirection.current == LayoutDirection.Rtl && icon.isAutoMirrored()
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = if (needsRtlMirror) modifier.graphicsLayer(scaleX = -1f) else modifier,
        tint = tint,
    )
}

@Composable
fun PeerlessIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

private fun DrawableResource.isAutoMirrored(): Boolean =
    this == PeerlessIcons.Queue ||
            this == PeerlessIcons.Logout ||
            this == PeerlessIcons.OpenInNew
