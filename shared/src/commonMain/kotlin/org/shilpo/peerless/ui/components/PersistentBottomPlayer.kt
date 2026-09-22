package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import coil3.compose.AsyncImage
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.theme.ExpressiveMotion
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.LocalLiquidGlassState
import org.shilpo.peerless.theme.SpecBadgeTypography
import org.shilpo.peerless.theme.SquircleShapeSmall
import org.shilpo.peerless.theme.extractArtworkSeedColor
import org.shilpo.peerless.theme.liquidGlass
import org.shilpo.peerless.theme.rememberLiquidGlassTint
import org.shilpo.peerless.ui.shell.SupportingPaneType

@Composable
fun PersistentBottomPlayer(
    track: TrackSummaryDto?,
    playbackInfo: PlaybackInfo?,
    status: PlaybackStatus,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long = 0L,
    artworkUrl: String,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    activeSupportingPane: SupportingPaneType? = null,
    onToggleSupportingPane: ((SupportingPaneType) -> Unit)? = null,
    modifier: Modifier = Modifier,
    volume: Float = 0.8f,
    onVolumeChange: (Float) -> Unit = {},
    isShuffle: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    isRepeat: Boolean = false,
    onToggleRepeat: () -> Unit = {},
    onOpenNowPlaying: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val isPlaying = status == PlaybackStatus.PLAYING

    var showAudioDetails by remember { mutableStateOf(false) }

    if (showAudioDetails && track != null) {
        val detailTrack = track.copy(
            codec = playbackInfo?.codec ?: track.codec,
            bit_depth = playbackInfo?.bit_depth ?: track.bit_depth,
            sample_rate = playbackInfo?.sample_rate ?: track.sample_rate
        )
        AudioDetailsModal(
            track = detailTrack,
            artworkUrl = artworkUrl,
            onDismiss = { showAudioDetails = false }
        )
    }

    val topEndCorner by animateDpAsState(
        targetValue = if (activeSupportingPane != null) 8.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = ExpressiveMotion.SpringDefaultSpatialDamping,
            stiffness = ExpressiveMotion.SpringDefaultSpatialStiffness
        ),
        label = "BottomPlayerTopEndCorner"
    )

    val containerShape = RoundedCornerShape(
        topStart = 8.dp,
        topEnd = topEndCorner,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )

    // Artwork-tinted glass: seed extracted from the artwork AsyncImage below
    // (avoids rememberArtworkSeedColor's stray 1.dp Image in the player).
    var extractedArtworkColor by remember(artworkUrl) { mutableStateOf<Color?>(null) }
    val liquidGlassEnabled = LocalLiquidGlassState.current?.isEnabled == true
    val glassTint = rememberLiquidGlassTint(artworkColor = extractedArtworkColor)


    val playButtonInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp)
            .shadow(
                elevation = if (liquidGlassEnabled) 10.dp else 4.dp,
                shape = containerShape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .then(
                if (liquidGlassEnabled) {
                    Modifier.liquidGlass(shape = containerShape, tint = glassTint)
                } else {
                    Modifier.background(colorScheme.surfaceContainerLow)
                }
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(containerShape)
                .padding(start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenNowPlaying
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(SquircleShapeSmall)
                        .background(colorScheme.surfaceContainer)
                        .border(
                            1.dp,
                            colorScheme.outlineVariant.copy(alpha = 0.6f),
                            SquircleShapeSmall
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (track != null) {
                        AsyncImage(
                            model = artworkUrl,
                            contentDescription = "${track.title} artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onSuccess = { state ->
                                extractedArtworkColor = extractArtworkSeedColor(
                                    painter = state.painter,
                                    fallback = colorScheme.primary
                                )
                            }
                        )
                    } else {
                        PeerlessIcon(
                            icon = PeerlessIcons.MusicNote,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
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
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = track?.artist ?: "Select a lossless track to begin",
                            style = ExpressiveTypography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (track != null) {
                            val effectiveCodec = playbackInfo?.codec ?: track.codec
                            val effectiveProvider = track.provider
                            val hasApple = effectiveProvider.contains("apple", ignoreCase = true)
                            val hasQobuz = effectiveProvider.contains("qobuz", ignoreCase = true)
                            val hasDolby = effectiveCodec.contains("ec-3", ignoreCase = true) ||
                                    effectiveCodec.contains("ec3", ignoreCase = true) ||
                                    effectiveCodec.contains("atmos", ignoreCase = true)
                            val effectiveBitDepth = playbackInfo?.bit_depth ?: track.bit_depth ?: 16
                            val effectiveSampleRate =
                                playbackInfo?.sample_rate ?: track.sample_rate ?: 44100
                            val hasHiRes = effectiveBitDepth >= 24 || effectiveSampleRate >= 88200
                            val hasAnyIcon =
                                hasApple || hasQobuz || effectiveProvider.isNotBlank() || hasDolby || hasHiRes

                            if (hasAnyIcon) {
                                Text(
                                    text = "•",
                                    style = ExpressiveTypography.bodySmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )

                                val iconInteractionSource = remember { MutableInteractionSource() }
                                val isIconHovered by iconInteractionSource.collectIsHoveredAsState()

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isIconHovered) colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.12f
                                            ) else Color.Transparent
                                        )
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable(
                                            interactionSource = iconInteractionSource,
                                            indication = ripple(bounded = true),
                                            onClick = { showAudioDetails = true }
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (hasApple) {
                                        PeerlessIcon(
                                            icon = PeerlessIcons.AppleLogo,
                                            contentDescription = "Apple Music",
                                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    if (hasQobuz) {
                                        PeerlessIcon(
                                            icon = PeerlessIcons.QobuzLogo,
                                            contentDescription = "Qobuz",
                                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            modifier = Modifier.height(11.dp).width(28.dp)
                                        )
                                    }
                                    if (!hasApple && !hasQobuz && effectiveProvider.isNotBlank()) {
                                        Text(
                                            text = formatProviderLabel(effectiveProvider),
                                            style = SpecBadgeTypography.copy(
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.3.sp
                                            ),
                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (hasDolby) {
                                        PeerlessIcon(
                                            icon = PeerlessIcons.DolbyAtmos,
                                            contentDescription = "Dolby Atmos",
                                            tint = colorScheme.tertiary,
                                            modifier = Modifier.height(10.dp).width(15.dp)
                                        )
                                    }
                                    if (hasHiRes) {
                                        PeerlessIcon(
                                            icon = PeerlessIcons.HiRes,
                                            contentDescription = "Hi-Res Audio",
                                            tint = colorScheme.tertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(48.dp)
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) colorScheme.secondary else colorScheme.onSurfaceVariant.copy(
                                alpha = 0.5f
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onPlayPrevious,
                            modifier = Modifier.size(48.dp)
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = playButtonInteractionSource,
                                    indication = ripple(),
                                    onClick = onTogglePlayPause
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = if (isPlaying) "Pause" else "Play"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                PlaybackButtonFace(
                                    isPlaying = isPlaying,
                                    color = colorScheme.primary,
                                    modifier = Modifier.fillMaxSize()
                                )
                                PlayPauseMorphIcon(
                                    isPlaying = isPlaying,
                                    tint = colorScheme.onPrimary,
                                    size = 24.dp
                                )
                            }
                        }

                        IconButton(
                            onClick = onPlayNext,
                            modifier = Modifier.size(48.dp)
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.SkipNext,
                                contentDescription = "Next Track",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(48.dp)
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) colorScheme.secondary else colorScheme.onSurfaceVariant.copy(
                                alpha = 0.5f
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                ExpressiveWavySeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    bufferedPositionMs = if (track?.is_cached == true && durationMs > 0L) durationMs else bufferedPositionMs,
                    isPlaying = isPlaying,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveVolumeSlider(
                    volume = volume,
                    onVolumeChange = onVolumeChange,
                    modifier = Modifier.width(160.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                PlaybackDeviceIndicator(
                    showLabel = false,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                if (onToggleSupportingPane != null) {
                    val isPaneOpen = activeSupportingPane != null
                    IconButton(
                        onClick = {
                            if (isPaneOpen) {
                                onToggleSupportingPane(activeSupportingPane)
                            } else {
                                onToggleSupportingPane(SupportingPaneType.QUEUE)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        NavToggleMorphIcon(
                            isExpanded = isPaneOpen,
                            size = 20.dp,
                            tint = if (isPaneOpen) colorScheme.primary else colorScheme.onSurfaceVariant,
                            contentDescription = if (isPaneOpen) "Close supporting pane" else "Open supporting pane",
                            flipHorizontal = true
                        )
                    }
                }
            }
        }
    }
}

internal const val PLAYBACK_COOKIE_ROTATION_DURATION_MILLIS = 60_000

internal fun playbackCookieRotationDegrees(progress: Float): Float =
    progress.coerceIn(0f, 1f) * 360f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaybackButtonFace(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val shapeProgress = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(
            dampingRatio = ExpressiveMotion.SpringDefaultSpatialDamping,
            stiffness = ExpressiveMotion.SpringDefaultSpatialStiffness,
            visibilityThreshold = 0.001f
        ),
        label = "PlaybackButtonShapeMorph"
    )
    val rotationProgress = if (isPlaying) {
        rememberInfiniteTransition(label = "PlaybackCookieRotation").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = PLAYBACK_COOKIE_ROTATION_DURATION_MILLIS,
                    easing = LinearEasing
                )
            ),
            label = "PlaybackCookieRotationProgress"
        )
    } else {
        null
    }
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie12Sided) }
    val path = remember { Path() }
    val scaleMatrix = remember { Matrix() }

    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = rotationProgress?.value?.let(::playbackCookieRotationDegrees) ?: 0f
        }
    ) {
        val morphPath = morph.toPath(
            progress = shapeProgress.value.coerceIn(0f, 1f),
            path = path
        )
        scaleMatrix.reset()
        scaleMatrix.scale(x = size.width, y = size.height)
        morphPath.transform(scaleMatrix)
        val pathCenter = morphPath.getBounds().center
        morphPath.translate(
            Offset(
                x = size.width / 2f - pathCenter.x,
                y = size.height / 2f - pathCenter.y
            )
        )
        drawPath(morphPath, color)
    }
}

@Composable
private fun ExpressiveWavySeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long = 0L,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var scrubPositionMs by remember { mutableStateOf<Long?>(null) }

    val (smoothProgressFraction, displayedPosition) = rememberSmoothProgress(
        isPlayingProvider = { isPlaying },
        currentPositionProvider = { scrubPositionMs ?: positionMs },
        totalDuration = durationMs.coerceAtLeast(0L),
        isVisible = true
    )

    val currentPosition = scrubPositionMs ?: displayedPosition.value
    val elapsedText = formatDuration((currentPosition / 1000).toInt())
    val totalText = formatDuration((durationMs / 1000).toInt())
    val visualContentOffsetY = (-4).dp

    val effectiveBufferedMs = maxOf(bufferedPositionMs, positionMs)
    val bufferedFraction = if (durationMs > 0L) {
        (effectiveBufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = elapsedText,
            style = SpecBadgeTypography.copy(fontSize = 11.sp),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.offset(y = visualContentOffsetY)
        )

        WavySliderExpressive(
            value = { smoothProgressFraction.value },
            bufferedValue = { bufferedFraction },
            onValueChange = { fraction ->
                scrubPositionMs = (fraction * durationMs.coerceAtLeast(0L)).toLong()
            },
            onValueCommit = { fraction ->
                val targetMs = (fraction * durationMs.coerceAtLeast(0L)).toLong()
                onSeekTo(targetMs)
                scrubPositionMs = null
            },
            enabled = durationMs > 0L,
            activeTrackColor = colorScheme.primary,
            inactiveTrackColor = colorScheme.surfaceContainerHighest,
            bufferedTrackColor = colorScheme.onSurface.copy(alpha = 0.42f),
            thumbColor = colorScheme.primary,
            visualContentOffsetY = visualContentOffsetY,
            isPlaying = isPlaying,
            isVisible = true,
            strokeWidth = 5.dp,
            thumbRadius = 8.dp,
            idleGap = 3.5.dp,
            thumbLineHeightWhenInteracting = 24.dp,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        )

        Text(
            text = totalText,
            style = SpecBadgeTypography.copy(fontSize = 11.sp),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.offset(y = visualContentOffsetY)
        )
    }
}
