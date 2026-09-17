package org.shilpo.peerless.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PeerlessIcons {
    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(8f, 5.14f)
                verticalLineTo(18.86f)
                curveTo(8f, 19.66f, 8.89f, 20.14f, 9.56f, 19.7f)
                lineTo(20.08f, 12.84f)
                curveTo(20.71f, 12.43f, 20.71f, 11.57f, 20.08f, 11.16f)
                lineTo(9.56f, 4.3f)
                curveTo(8.89f, 3.86f, 8f, 4.34f, 8f, 5.14f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6.75f, 5f)
                curveTo(6.06f, 5f, 5.5f, 5.56f, 5.5f, 6.25f)
                verticalLineTo(17.75f)
                curveTo(5.5f, 18.44f, 6.06f, 19f, 6.75f, 19f)
                horizontalLineTo(9.25f)
                curveTo(9.94f, 19f, 10.5f, 18.44f, 10.5f, 17.75f)
                verticalLineTo(6.25f)
                curveTo(10.5f, 5.56f, 9.94f, 5f, 9.25f, 5f)
                horizontalLineTo(6.75f)
                close()
                moveTo(14.75f, 5f)
                curveTo(14.06f, 5f, 13.5f, 5.56f, 13.5f, 6.25f)
                verticalLineTo(17.75f)
                curveTo(13.5f, 18.44f, 14.06f, 19f, 14.75f, 19f)
                horizontalLineTo(17.25f)
                curveTo(17.94f, 19f, 18.5f, 18.44f, 18.5f, 17.75f)
                verticalLineTo(6.25f)
                curveTo(18.5f, 5.56f, 17.94f, 5f, 17.25f, 5f)
                horizontalLineTo(14.75f)
                close()
            }
        }.build()
    }

    val SkipNext: ImageVector by lazy {
        ImageVector.Builder(
            name = "SkipNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6.8f, 5.48f)
                verticalLineTo(18.52f)
                curveTo(6.8f, 19.32f, 7.69f, 19.8f, 8.36f, 19.36f)
                lineTo(16.5f, 13.2f)
                verticalLineTo(18.25f)
                curveTo(16.5f, 18.66f, 16.84f, 19f, 17.25f, 19f)
                horizontalLineTo(18.75f)
                curveTo(19.16f, 19f, 19.5f, 18.66f, 19.5f, 18.25f)
                verticalLineTo(5.75f)
                curveTo(19.5f, 5.34f, 19.16f, 5f, 18.75f, 5f)
                horizontalLineTo(17.25f)
                curveTo(16.84f, 5f, 16.5f, 5.34f, 16.5f, 5.75f)
                verticalLineTo(10.8f)
                lineTo(8.36f, 4.64f)
                curveTo(7.69f, 4.2f, 6.8f, 4.68f, 6.8f, 5.48f)
                close()
            }
        }.build()
    }

    val SkipPrevious: ImageVector by lazy {
        ImageVector.Builder(
            name = "SkipPrevious",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(17.2f, 18.52f)
                verticalLineTo(5.48f)
                curveTo(17.2f, 4.68f, 16.31f, 4.2f, 15.64f, 4.64f)
                lineTo(7.5f, 10.8f)
                verticalLineTo(5.75f)
                curveTo(7.5f, 5.34f, 7.16f, 5f, 6.75f, 5f)
                horizontalLineTo(5.25f)
                curveTo(4.84f, 5f, 4.5f, 5.34f, 4.5f, 5.75f)
                verticalLineTo(18.25f)
                curveTo(4.5f, 18.66f, 4.84f, 19f, 5.25f, 19f)
                horizontalLineTo(6.75f)
                curveTo(7.16f, 19f, 7.5f, 18.66f, 7.5f, 18.25f)
                verticalLineTo(13.2f)
                lineTo(15.64f, 19.36f)
                curveTo(16.31f, 19.8f, 17.2f, 19.32f, 17.2f, 18.52f)
                close()
            }
        }.build()
    }

    val Shuffle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shuffle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10.59f, 9.17f)
                lineTo(5.41f, 4f)
                lineTo(4f, 5.41f)
                lineTo(9.17f, 10.59f)
                lineTo(10.59f, 9.17f)
                close()
                moveTo(14.5f, 4f)
                lineTo(16.54f, 6.04f)
                lineTo(4f, 18.59f)
                lineTo(5.41f, 20f)
                lineTo(17.96f, 7.46f)
                lineTo(20f, 9.5f)
                verticalLineTo(4f)
                horizontalLineTo(14.5f)
                close()
                moveTo(14.83f, 13.41f)
                lineTo(13.42f, 14.83f)
                lineTo(16.55f, 17.95f)
                lineTo(14.5f, 20f)
                horizontalLineTo(20f)
                verticalLineTo(14.5f)
                lineTo(17.96f, 16.54f)
                lineTo(14.83f, 13.41f)
                close()
            }
        }.build()
    }

    val Repeat: ImageVector by lazy {
        ImageVector.Builder(
            name = "Repeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 7f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                lineTo(21f, 6f)
                lineTo(17f, 2f)
                verticalLineTo(5f)
                horizontalLineTo(5f)
                curveTo(3.9f, 5f, 3f, 5.9f, 3f, 7f)
                verticalLineTo(13f)
                horizontalLineTo(5f)
                verticalLineTo(7f)
                close()
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                verticalLineTo(14f)
                lineTo(3f, 18f)
                lineTo(7f, 22f)
                verticalLineTo(19f)
                horizontalLineTo(19f)
                curveTo(20.1f, 19f, 21f, 18.1f, 21f, 17f)
                verticalLineTo(11f)
                horizontalLineTo(19f)
                verticalLineTo(17f)
                close()
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(15.5f, 14f)
                horizontalLineTo(14.71f)
                lineTo(14.43f, 13.73f)
                curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
                curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
                curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
                curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
                curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
                lineTo(14f, 14.71f)
                verticalLineTo(15.5f)
                lineTo(19f, 20.49f)
                lineTo(20.49f, 19f)
                lineTo(15.5f, 14f)
                close()
                moveTo(9.5f, 14f)
                curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
                curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
                curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
                curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
                close()
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19f, 6.41f)
                lineTo(17.59f, 5f)
                lineTo(12f, 10.59f)
                lineTo(6.41f, 5f)
                lineTo(5f, 6.41f)
                lineTo(10.59f, 12f)
                lineTo(5f, 17.59f)
                lineTo(6.41f, 19f)
                lineTo(12f, 13.41f)
                lineTo(17.59f, 19f)
                lineTo(19f, 17.59f)
                lineTo(13.41f, 12f)
                close()
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19.43f, 12.98f)
                curveTo(19.47f, 12.66f, 19.5f, 12.34f, 19.5f, 12f)
                curveTo(19.5f, 11.66f, 19.47f, 11.34f, 19.43f, 11.02f)
                lineTo(21.54f, 9.37f)
                curveTo(21.73f, 9.22f, 21.78f, 8.95f, 21.66f, 8.73f)
                lineTo(19.66f, 5.27f)
                curveTo(19.54f, 5.05f, 19.27f, 4.97f, 19.05f, 5.05f)
                lineTo(16.56f, 6.05f)
                curveTo(16.04f, 5.65f, 15.48f, 5.32f, 14.87f, 5.07f)
                lineTo(14.49f, 2.42f)
                curveTo(14.46f, 2.18f, 14.25f, 2f, 14f, 2f)
                horizontalLineTo(10f)
                curveTo(9.75f, 2f, 9.54f, 2.18f, 9.51f, 2.42f)
                lineTo(9.13f, 5.07f)
                curveTo(8.52f, 5.32f, 7.96f, 5.66f, 7.44f, 6.05f)
                lineTo(4.95f, 5.05f)
                curveTo(4.72f, 4.96f, 4.46f, 5.05f, 4.34f, 5.27f)
                lineTo(2.34f, 8.73f)
                curveTo(2.21f, 8.95f, 2.27f, 9.22f, 2.46f, 9.37f)
                lineTo(4.57f, 11.02f)
                curveTo(4.53f, 11.34f, 4.5f, 11.67f, 4.5f, 12f)
                curveTo(4.5f, 12.33f, 4.53f, 12.66f, 4.57f, 12.98f)
                lineTo(2.46f, 14.63f)
                curveTo(2.26f, 14.78f, 2.21f, 15.05f, 2.34f, 15.27f)
                lineTo(4.34f, 18.73f)
                curveTo(4.46f, 18.95f, 4.73f, 19.03f, 4.95f, 18.95f)
                lineTo(7.44f, 17.95f)
                curveTo(7.96f, 18.35f, 8.52f, 18.68f, 9.13f, 18.93f)
                lineTo(9.51f, 21.58f)
                curveTo(9.54f, 21.82f, 9.75f, 22f, 10f, 22f)
                horizontalLineTo(14f)
                curveTo(14.25f, 22f, 14.46f, 21.82f, 14.49f, 21.58f)
                lineTo(14.87f, 18.93f)
                curveTo(15.48f, 18.68f, 16.04f, 18.34f, 16.56f, 17.95f)
                lineTo(19.05f, 18.95f)
                curveTo(19.28f, 19.04f, 19.54f, 18.95f, 19.66f, 18.73f)
                lineTo(21.66f, 15.27f)
                curveTo(21.78f, 15.05f, 21.73f, 14.78f, 21.54f, 14.63f)
                lineTo(19.43f, 12.98f)
                close()
                moveTo(12f, 15.5f)
                curveTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
                curveTo(8.5f, 10.07f, 10.07f, 8.5f, 12f, 8.5f)
                curveTo(13.93f, 8.5f, 15.5f, 10.07f, 15.5f, 12f)
                curveTo(15.5f, 13.93f, 13.93f, 15.5f, 12f, 15.5f)
                close()
            }
        }.build()
    }

    val ExpandMore: ImageVector by lazy {
        ImageVector.Builder(
            name = "ExpandMore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(16.59f, 8.59f)
                lineTo(12f, 13.17f)
                lineTo(7.41f, 8.59f)
                lineTo(6f, 10f)
                lineTo(12f, 16f)
                lineTo(18f, 10f)
                close()
            }
        }.build()
    }

    val MusicNote: ImageVector by lazy {
        ImageVector.Builder(
            name = "MusicNote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 3f)
                verticalLineTo(13.55f)
                curveTo(11.41f, 13.21f, 10.73f, 13f, 10f, 13f)
                curveTo(7.79f, 13f, 6f, 14.79f, 6f, 17f)
                curveTo(6f, 19.21f, 7.79f, 21f, 10f, 21f)
                curveTo(12.21f, 21f, 14f, 19.21f, 14f, 17f)
                verticalLineTo(7f)
                horizontalLineTo(18f)
                verticalLineTo(3f)
                horizontalLineTo(12f)
                close()
            }
        }.build()
    }

    val LosslessWave: ImageVector by lazy {
        ImageVector.Builder(
            name = "LosslessWave",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Waveform bars
                moveTo(3f, 10f)
                curveTo(2.45f, 10f, 2f, 10.45f, 2f, 11f)
                verticalLineTo(13f)
                curveTo(2f, 13.55f, 2.45f, 14f, 3f, 14f)
                curveTo(3.55f, 14f, 4f, 13.55f, 4f, 13f)
                verticalLineTo(11f)
                curveTo(4f, 10.45f, 3.55f, 10f, 3f, 10f)
                close()

                moveTo(7f, 7f)
                curveTo(6.45f, 7f, 6f, 7.45f, 6f, 8f)
                verticalLineTo(16f)
                curveTo(6f, 16.55f, 6.45f, 17f, 7f, 17f)
                curveTo(7.55f, 17f, 8f, 16.55f, 8f, 16f)
                verticalLineTo(8f)
                curveTo(8f, 7.45f, 7.55f, 7f, 7f, 7f)
                close()

                moveTo(11f, 4f)
                curveTo(10.45f, 4f, 10f, 4.45f, 10f, 5f)
                verticalLineTo(19f)
                curveTo(10f, 19.55f, 10.45f, 20f, 11f, 20f)
                curveTo(11.55f, 20f, 12f, 19.55f, 12f, 19f)
                verticalLineTo(5f)
                curveTo(12f, 4.45f, 11.55f, 4f, 11f, 4f)
                close()

                moveTo(15f, 6f)
                curveTo(14.45f, 6f, 14f, 6.45f, 14f, 7f)
                verticalLineTo(17f)
                curveTo(14f, 17.55f, 14.45f, 18f, 15f, 18f)
                curveTo(15.55f, 18f, 16f, 17.55f, 16f, 17f)
                verticalLineTo(7f)
                curveTo(16f, 6.45f, 15.55f, 6f, 15f, 6f)
                close()

                moveTo(19f, 9f)
                curveTo(18.45f, 9f, 18f, 9.45f, 18f, 10f)
                verticalLineTo(14f)
                curveTo(18f, 14.55f, 18.45f, 15f, 19f, 15f)
                curveTo(19.55f, 15f, 20f, 14.55f, 20f, 14f)
                verticalLineTo(10f)
                curveTo(20f, 9.45f, 19.55f, 9f, 19f, 9f)
                close()
            }
        }.build()
    }

    val CloudDone: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudDone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19.35f, 10.04f)
                curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
                curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
                curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
                curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
                horizontalLineTo(19f)
                curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
                curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
                close()
                moveTo(10f, 17f)
                lineTo(5f, 12f)
                lineTo(6.41f, 10.59f)
                lineTo(10f, 14.17f)
                lineTo(17.59f, 6.58f)
                lineTo(19f, 8f)
                lineTo(10f, 17f)
                close()
            }
        }.build()
    }

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10f, 20f)
                verticalLineTo(14f)
                horizontalLineTo(14f)
                verticalLineTo(20f)
                horizontalLineTo(19f)
                verticalLineTo(12f)
                horizontalLineTo(22f)
                lineTo(12f, 3f)
                lineTo(2f, 12f)
                horizontalLineTo(5f)
                verticalLineTo(20f)
                horizontalLineTo(10f)
                close()
            }
        }.build()
    }

    val Library: ImageVector by lazy {
        ImageVector.Builder(
            name = "Library",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 2f)
                horizontalLineTo(8f)
                curveTo(6.9f, 2f, 6f, 2.9f, 6f, 4f)
                verticalLineTo(16f)
                curveTo(6f, 17.1f, 6.9f, 18f, 8f, 18f)
                horizontalLineTo(20f)
                curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
                verticalLineTo(4f)
                curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
                close()
                moveTo(18f, 7f)
                horizontalLineTo(14f)
                verticalLineTo(12.55f)
                curveTo(13.56f, 12.21f, 13.01f, 12f, 12.4f, 12f)
                curveTo(10.74f, 12f, 9.4f, 13.34f, 9.4f, 15f)
                curveTo(9.4f, 16.66f, 10.74f, 18f, 12.4f, 18f)
                curveTo(14.06f, 18f, 15.4f, 16.66f, 15.4f, 15f)
                verticalLineTo(9f)
                horizontalLineTo(18f)
                verticalLineTo(7f)
                close()
                moveTo(2f, 6f)
                verticalLineTo(20f)
                curveTo(2f, 21.1f, 2.9f, 22f, 4f, 22f)
                horizontalLineTo(18f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                horizontalLineTo(2f)
                close()
            }
        }.build()
    }

    val Queue: ImageVector by lazy {
        ImageVector.Builder(
            name = "Queue",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(15f, 6f)
                horizontalLineTo(3f)
                verticalLineTo(8f)
                horizontalLineTo(15f)
                verticalLineTo(6f)
                close()
                moveTo(15f, 10f)
                horizontalLineTo(3f)
                verticalLineTo(12f)
                horizontalLineTo(15f)
                verticalLineTo(10f)
                close()
                moveTo(11f, 14f)
                horizontalLineTo(3f)
                verticalLineTo(16f)
                horizontalLineTo(11f)
                verticalLineTo(14f)
                close()
                moveTo(19f, 6f)
                verticalLineTo(12.18f)
                curveTo(18.53f, 12.07f, 18.04f, 12f, 17.5f, 12f)
                curveTo(15.01f, 12f, 13f, 14.01f, 13f, 16.5f)
                curveTo(13f, 18.99f, 15.01f, 21f, 17.5f, 21f)
                curveTo(19.99f, 21f, 22f, 18.99f, 22f, 16.5f)
                verticalLineTo(8f)
                horizontalLineTo(24f)
                verticalLineTo(6f)
                horizontalLineTo(19f)
                close()
            }
        }.build()
    }

    val Lyrics: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lyrics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                verticalLineTo(18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(6f)
                curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
                close()
                moveTo(18f, 9f)
                horizontalLineTo(6f)
                verticalLineTo(7f)
                horizontalLineTo(18f)
                verticalLineTo(9f)
                close()
                moveTo(15f, 13f)
                horizontalLineTo(6f)
                verticalLineTo(11f)
                horizontalLineTo(15f)
                verticalLineTo(13f)
                close()
                moveTo(12f, 17f)
                horizontalLineTo(6f)
                verticalLineTo(15f)
                horizontalLineTo(12f)
                verticalLineTo(17f)
                close()
            }
        }.build()
    }

    val SignalPath: ImageVector by lazy {
        ImageVector.Builder(
            name = "SignalPath",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(3f, 17f)
                verticalLineTo(19f)
                horizontalLineTo(9f)
                verticalLineTo(17f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 5f)
                verticalLineTo(7f)
                horizontalLineTo(13f)
                verticalLineTo(5f)
                horizontalLineTo(3f)
                close()
                moveTo(13f, 21f)
                verticalLineTo(19f)
                horizontalLineTo(21f)
                verticalLineTo(17f)
                horizontalLineTo(13f)
                verticalLineTo(15f)
                horizontalLineTo(11f)
                verticalLineTo(21f)
                horizontalLineTo(13f)
                close()
                moveTo(7f, 9f)
                verticalLineTo(11f)
                horizontalLineTo(3f)
                verticalLineTo(13f)
                horizontalLineTo(7f)
                verticalLineTo(15f)
                horizontalLineTo(9f)
                verticalLineTo(9f)
                horizontalLineTo(7f)
                close()
                moveTo(21f, 13f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(21f)
                close()
                moveTo(17f, 9f)
                horizontalLineTo(19f)
                verticalLineTo(7f)
                horizontalLineTo(21f)
                verticalLineTo(5f)
                horizontalLineTo(19f)
                verticalLineTo(3f)
                horizontalLineTo(17f)
                verticalLineTo(9f)
                close()
            }
        }.build()
    }

    val VolumeUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "VolumeUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(3f, 9f)
                verticalLineTo(15f)
                horizontalLineTo(7f)
                lineTo(12f, 20f)
                verticalLineTo(4f)
                lineTo(7f, 9f)
                horizontalLineTo(3f)
                close()
                moveTo(16.5f, 12f)
                curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 7.97f)
                verticalLineTo(16.02f)
                curveTo(15.48f, 15.29f, 16.5f, 13.77f, 16.5f, 12f)
                close()
                moveTo(14f, 3.23f)
                verticalLineTo(5.29f)
                curveTo(16.89f, 6.15f, 19f, 8.83f, 19f, 12f)
                curveTo(19f, 15.17f, 16.89f, 17.85f, 14f, 18.71f)
                verticalLineTo(20.77f)
                curveTo(18.01f, 19.86f, 21f, 16.28f, 21f, 12f)
                curveTo(21f, 7.72f, 18.01f, 4.14f, 14f, 3.23f)
                close()
            }
        }.build()
    }

    val VolumeMute: ImageVector by lazy {
        ImageVector.Builder(
            name = "VolumeMute",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 9f)
                verticalLineTo(15f)
                horizontalLineTo(11f)
                lineTo(16f, 20f)
                verticalLineTo(4f)
                lineTo(11f, 9f)
                horizontalLineTo(7f)
                close()
            }
        }.build()
    }

    val MoreVert: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 8f)
                curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f)
                curveTo(14f, 4.9f, 13.1f, 4f, 12f, 4f)
                curveTo(10.9f, 4f, 10f, 4.9f, 10f, 6f)
                curveTo(10f, 7.1f, 10.9f, 8f, 12f, 8f)
                close()
                moveTo(12f, 10f)
                curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
                curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
                curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
                curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
                close()
                moveTo(12f, 16f)
                curveTo(10.9f, 16f, 10f, 16.9f, 10f, 18f)
                curveTo(10f, 19.1f, 10.9f, 20f, 12f, 20f)
                curveTo(13.1f, 20f, 14f, 19.1f, 14f, 18f)
                curveTo(14f, 16.9f, 13.1f, 16f, 12f, 16f)
                close()
            }
        }.build()
    }

    val Heart: ImageVector by lazy {
        ImageVector.Builder(
            name = "Heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 21.35f)
                lineTo(10.55f, 20.03f)
                curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
                curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
                curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
                curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
                curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
                curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
                lineTo(12f, 21.35f)
                close()
            }
        }.build()
    }

    val Sparkle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sparkle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10.5f, 1.5f)
                lineTo(8.2f, 6.7f)
                lineTo(3f, 9f)
                lineTo(8.2f, 11.3f)
                lineTo(10.5f, 16.5f)
                lineTo(12.8f, 11.3f)
                lineTo(18f, 9f)
                lineTo(12.8f, 6.7f)
                close()
                moveTo(19f, 14f)
                lineTo(17.7f, 16.7f)
                lineTo(15f, 18f)
                lineTo(17.7f, 19.3f)
                lineTo(19f, 22f)
                lineTo(20.3f, 19.3f)
                lineTo(23f, 18f)
                lineTo(20.3f, 16.7f)
                close()
            }
        }.build()
    }

    val Compass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Compass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(14.19f, 14.19f)
                lineTo(6f, 18f)
                lineTo(9.81f, 9.81f)
                lineTo(18f, 6f)
                lineTo(14.19f, 14.19f)
                close()
            }
        }.build()
    }

    val BarChart: ImageVector by lazy {
        ImageVector.Builder(
            name = "BarChart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 9f)
                horizontalLineTo(7f)
                verticalLineTo(19f)
                horizontalLineTo(4f)
                close()
                moveTo(10.5f, 4f)
                horizontalLineTo(13.5f)
                verticalLineTo(19f)
                horizontalLineTo(10.5f)
                close()
                moveTo(17f, 13f)
                horizontalLineTo(20f)
                verticalLineTo(19f)
                horizontalLineTo(17f)
                close()
            }
        }.build()
    }

    val Database: ImageVector by lazy {
        ImageVector.Builder(
            name = "Database",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(7.58f, 2f, 4f, 3.79f, 4f, 6f)
                verticalLineTo(18f)
                curveTo(4f, 20.21f, 7.58f, 22f, 12f, 22f)
                curveTo(16.42f, 22f, 20f, 20.21f, 20f, 18f)
                verticalLineTo(6f)
                curveTo(20f, 3.79f, 16.42f, 2f, 12f, 2f)
                close()
                moveTo(18f, 18f)
                curveTo(18f, 18.78f, 15.31f, 20f, 12f, 20f)
                curveTo(8.69f, 20f, 6f, 18.78f, 6f, 18f)
                verticalLineTo(16.14f)
                curveTo(7.53f, 17.29f, 9.64f, 18f, 12f, 18f)
                curveTo(14.36f, 18f, 16.47f, 17.29f, 18f, 16.14f)
                verticalLineTo(18f)
                close()
                moveTo(18f, 13f)
                curveTo(18f, 13.78f, 15.31f, 15f, 12f, 15f)
                curveTo(8.69f, 15f, 6f, 13.78f, 6f, 13f)
                verticalLineTo(11.14f)
                curveTo(7.53f, 12.29f, 9.64f, 13f, 12f, 13f)
                curveTo(14.36f, 13f, 16.47f, 12.29f, 18f, 11.14f)
                verticalLineTo(13f)
                close()
                moveTo(12f, 4f)
                curveTo(15.31f, 4f, 18f, 5.22f, 18f, 6f)
                curveTo(18f, 6.78f, 15.31f, 8f, 12f, 8f)
                curveTo(8.69f, 8f, 6f, 6.78f, 6f, 6f)
                curveTo(6f, 5.22f, 8.69f, 4f, 12f, 4f)
                close()
            }
        }.build()
    }
}

