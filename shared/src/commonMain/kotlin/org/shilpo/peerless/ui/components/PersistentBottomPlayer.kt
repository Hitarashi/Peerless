package org.shilpo.peerless.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.theme.*
import org.shilpo.peerless.ui.shell.SupportingPaneType
import kotlin.math.PI
import kotlin.math.sin

/**
 * Full-width persistent bottom playback bar for Expanded (>= 840dp Desktop/Tablet) viewports.
 * Conforms to Google Large Screen Guidelines & ADR 0002.
 *
 * Left: Artwork thumbnail + Track Title + Artist + Poweramp-style Lossless Badge
 * Center: Playback controls (Shuffle, Previous, Play/Pause, Next, Repeat) + Wavy progress bar + timestamps
 * Right: Volume slider + Supporting Pane toggles (Queue, Lyrics, Signal Path)
 */
@Composable
fun PersistentBottomPlayer(
    track: TrackSummaryDto?,
    playbackInfo: PlaybackInfo?,
    status: PlaybackStatus,
    positionMs: Long,
    durationMs: Long,
    artworkUrl: String,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    activeSupportingPane: SupportingPaneType?,
    onToggleSupportingPane: (SupportingPaneType) -> Unit,
    modifier: Modifier = Modifier,
    volume: Float = 0.8f,
    onVolumeChange: (Float) -> Unit = {},
    isShuffle: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    isRepeat: Boolean = false,
    onToggleRepeat: () -> Unit = {},
    onOpenNowPlaying: () -> Unit = {}
) {
    val isPlaying = status == PlaybackStatus.PLAYING

    // Play button press animation
    val playButtonInteractionSource = remember { MutableInteractionSource() }
    val isPlayPressed by playButtonInteractionSource.collectIsPressedAsState()
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.92f else 1f,
        label = "PersistentPlayScale"
    )

    // Liquid glass frosted bottom container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(LiquidGlassDefaults.ElevatedContainerColor)
            .border(
                width = 1.dp,
                brush = LiquidGlassDefaults.BorderBrush,
                shape = RectangleShape
            )
    ) {
        // Specular top highlight line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ==========================================
            // LEFT: Artwork, Title, Artist, Lossless Badge
            // ==========================================
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenNowPlaying
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Miniature artwork
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(SquircleShapeSmall)
                        .background(SurfaceContainerDark)
                        .border(1.dp, OutlineVariantDark.copy(alpha = 0.6f), SquircleShapeSmall),
                    contentAlignment = Alignment.Center
                ) {
                    if (track != null) {
                        AsyncImage(
                            model = artworkUrl,
                            contentDescription = "${track.title} artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = PeerlessIcons.MusicNote,
                            contentDescription = null,
                            tint = OnSurfaceVariantDark.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = track?.title ?: "No track playing",
                        style = ExpressiveTypography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = track?.artist ?: "Select a lossless track to begin",
                            style = ExpressiveTypography.bodySmall,
                            color = OnSurfaceVariantDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (track != null) {
                            LosslessBadge(
                                bitDepth = playbackInfo?.bit_depth ?: track.bit_depth,
                                sampleRate = playbackInfo?.sample_rate ?: track.sample_rate,
                                codec = playbackInfo?.codec ?: track.codec,
                                compact = true
                            )
                        }
                    }
                }
            }

            // ==========================================
            // CENTER: Controls + Expressive Wavy Progress
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Transport control buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Shuffle button
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) SecondaryDark else OnSurfaceVariantDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Previous button
                    IconButton(
                        onClick = onPlayPrevious,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = OnSurfaceDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Play / Pause central squircle
                    Box(
                        modifier = Modifier
                            .scale(playButtonScale)
                            .size(42.dp)
                            .clip(SquircleShapeSmall)
                            .background(PrimaryDark)
                            .clickable(
                                interactionSource = playButtonInteractionSource,
                                indication = ripple(),
                                onClick = onTogglePlayPause
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "BottomPlayPauseAnim"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) PeerlessIcons.Pause else PeerlessIcons.Play,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = OnPrimaryDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Next button
                    IconButton(
                        onClick = onPlayNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.SkipNext,
                            contentDescription = "Next Track",
                            tint = OnSurfaceDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Repeat button
                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = PeerlessIcons.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) SecondaryDark else OnSurfaceVariantDark.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Wavy Scrubber & Timestamps
                ExpressiveWavySeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isPlaying = isPlaying,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ==========================================
            // RIGHT: Volume + Supporting Pane Toggles
            // ==========================================
            Row(
                modifier = Modifier.weight(1.1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Volume controls
                var isMuted by remember { mutableStateOf(false) }
                var lastVolume by remember { mutableFloatStateOf(volume) }

                IconButton(
                    onClick = {
                        if (isMuted) {
                            isMuted = false
                            onVolumeChange(if (lastVolume > 0f) lastVolume else 0.5f)
                        } else {
                            lastVolume = volume
                            isMuted = true
                            onVolumeChange(0f)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (volume <= 0.01f || isMuted) PeerlessIcons.VolumeMute else PeerlessIcons.VolumeUp,
                        contentDescription = "Volume",
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Slider(
                    value = if (isMuted) 0f else volume,
                    onValueChange = {
                        isMuted = false
                        onVolumeChange(it)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryDark,
                        activeTrackColor = PrimaryDark,
                        inactiveTrackColor = SurfaceContainerHighestDark
                    ),
                    modifier = Modifier
                        .width(85.dp)
                        .height(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Subtle vertical separator
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(OutlineVariantDark.copy(alpha = 0.6f))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Supporting Pane Action Toggles
                SupportingPaneToggleButton(
                    icon = PeerlessIcons.Queue,
                    contentDescription = "Queue Pane",
                    isActive = activeSupportingPane == SupportingPaneType.QUEUE,
                    onClick = { onToggleSupportingPane(SupportingPaneType.QUEUE) }
                )

                SupportingPaneToggleButton(
                    icon = PeerlessIcons.Lyrics,
                    contentDescription = "Lyrics Pane",
                    isActive = activeSupportingPane == SupportingPaneType.LYRICS,
                    onClick = { onToggleSupportingPane(SupportingPaneType.LYRICS) }
                )

                SupportingPaneToggleButton(
                    icon = PeerlessIcons.SignalPath,
                    contentDescription = "Signal Path Inspector",
                    isActive = activeSupportingPane == SupportingPaneType.SIGNAL_PATH,
                    onClick = { onToggleSupportingPane(SupportingPaneType.SIGNAL_PATH) }
                )
            }
        }
    }
}

/**
 * Expressive Wavy Scrubber that animates an authentic sinusoidal wave along the active track
 * when playback is active, morphing to a clean flat bar when paused.
 */
@Composable
fun ExpressiveWavySeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val displayFraction = if (isDragging) dragFraction else currentFraction
    val displayPositionMs = (displayFraction * durationMs).toLong()

    val elapsedText = formatDuration((displayPositionMs / 1000).toInt())
    val totalText = formatDuration((durationMs / 1000).toInt())

    // Infinite wave phase animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "WavySeekBarTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // Animated wave amplitude (flattens out when paused or dragging)
    val waveAmplitude by animateFloatAsState(
        targetValue = if (isPlaying && !isDragging) 2.2f else 0f,
        animationSpec = tween(durationMillis = 350, easing = ExpressiveMotion.EmphasizedEasing),
        label = "WaveAmplitude"
    )

    val density = LocalDensity.current
    val waveWavelengthPx = with(density) { 28.dp.toPx() }
    val waveAmplitudePx = with(density) { waveAmplitude.dp.toPx() }
    val strokeWidthPx = with(density) { 3.dp.toPx() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = elapsedText,
            style = SpecBadgeTypography.copy(fontSize = 10.sp),
            color = OnSurfaceVariantDark
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0L) {
                            val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeekTo((newFraction * durationMs).toLong())
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeekTo((dragFraction * durationMs).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                val totalWidth = size.width
                val centerY = size.height / 2f
                val activeWidth = totalWidth * displayFraction

                // 1. Draw inactive background track
                if (activeWidth < totalWidth) {
                    drawLine(
                        color = SurfaceContainerHighestDark,
                        start = Offset(activeWidth, centerY),
                        end = Offset(totalWidth, centerY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }

                // 2. Draw active wavy track
                if (activeWidth > 0f) {
                    val path = Path()
                    path.moveTo(0f, centerY)

                    var x = 0f
                    val step = 3f
                    while (x <= activeWidth) {
                        val angle = (x / waveWavelengthPx) * (2 * PI).toFloat() + wavePhase
                        val y = centerY + sin(angle) * waveAmplitudePx
                        path.lineTo(x, y)
                        x += step
                    }

                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            listOf(PrimaryDark, SecondaryDark)
                        ),
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // 3. Draw seeker thumb
                    val currentY =
                        centerY + sin((activeWidth / waveWavelengthPx) * (2 * PI).toFloat() + wavePhase) * waveAmplitudePx
                    val thumbRadius = if (isDragging) 6.dp.toPx() else 4.5.dp.toPx()

                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius,
                        center = Offset(activeWidth, currentY)
                    )
                }
            }
        }

        Text(
            text = totalText,
            style = SpecBadgeTypography.copy(fontSize = 10.sp),
            color = OnSurfaceVariantDark
        )
    }
}

/**
 * Supporting Pane action toggle button with active pill highlight.
 */
@Composable
private fun SupportingPaneToggleButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) PrimaryDark.copy(alpha = 0.22f) else Color.Transparent
    val contentColor = if (isActive) PrimaryDark else OnSurfaceVariantDark.copy(alpha = 0.75f)
    val borderColor = if (isActive) PrimaryDark.copy(alpha = 0.40f) else Color.Transparent

    Box(
        modifier = modifier
            .clip(PillShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
