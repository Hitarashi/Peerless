package org.shilpo.peerless.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Volume Slider (Size 3 Medium):
 * - 40dp track height with 12dp rounded corners and 6dp thumb-to-track gap.
 * - 4dp x 52dp vertical pill handle.
 * - Inset 22dp VolumeMorphIcon at start of track that smoothly morphs geometry
 *   and toggles mute when tapped.
 * - Dynamic contrast: Adapts tint from onSurfaceVariant (inactive track) to onPrimary
 *   (active track) once the slider thumb passes the icon.
 * - Generous, elongated hit-target footprint (~180dp to 200dp) for desktop and tablet playbars.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveVolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier.width(190.dp),
    enabled: Boolean = true,
    isMuted: Boolean? = null,
    onToggleMute: (() -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme

    var internalIsMuted by remember { mutableStateOf(false) }
    var lastNonZeroVolume by remember { mutableFloatStateOf(if (volume > 0.05f) volume else 0.5f) }

    val effectiveMuted = isMuted ?: internalIsMuted
    val displayVolume = if (effectiveMuted) 0f else volume.coerceIn(0f, 1f)

    val handleToggleMute: () -> Unit = {
        if (onToggleMute != null) {
            onToggleMute()
        } else {
            if (internalIsMuted) {
                internalIsMuted = false
                val restored = if (lastNonZeroVolume > 0.05f) lastNonZeroVolume else 0.5f
                onVolumeChange(restored)
            } else {
                if (volume > 0.05f) {
                    lastNonZeroVolume = volume
                }
                internalIsMuted = true
                onVolumeChange(0f)
            }
        }
    }

    val handleVolumeChange: (Float) -> Unit = { newVol ->
        if (effectiveMuted && newVol > 0.01f) {
            if (isMuted == null) {
                internalIsMuted = false
            }
        }
        if (newVol > 0.05f) {
            lastNonZeroVolume = newVol
        }
        onVolumeChange(newVol)
    }

    val isCoveredByActiveTrack = !effectiveMuted && displayVolume >= 0.20f

    val iconTint by animateColorAsState(
        targetValue = if (isCoveredByActiveTrack) {
            colorScheme.onPrimary
        } else {
            colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "VolumeSliderIconTint"
    )

    val cutoutColor = if (isCoveredByActiveTrack) {
        colorScheme.primary
    } else {
        colorScheme.surfaceContainerHighest
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = colorScheme.primary,
        activeTrackColor = colorScheme.primary,
        inactiveTrackColor = colorScheme.surfaceContainerHighest
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.height(52.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Slider(
            value = displayVolume,
            onValueChange = handleVolumeChange,
            valueRange = 0f..1f,
            enabled = enabled,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = sliderColors,
                    enabled = enabled,
                    thumbSize = DpSize(4.dp, 52.dp)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(40.dp),
                    colors = sliderColors,
                    enabled = enabled,
                    thumbTrackGapSize = 6.dp,
                    trackInsideCornerSize = 12.dp,
                    trackCornerSize = 12.dp
                )
            },
            colors = sliderColors,
            modifier = Modifier.fillMaxWidth()
        )

        // Inset VolumeMorphIcon (size 22dp with 10dp start padding, tapping toggles mute)
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(26.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 16.dp),
                    enabled = enabled,
                    onClick = handleToggleMute
                ),
            contentAlignment = Alignment.Center
        ) {
            VolumeMorphIcon(
                volume = displayVolume,
                isMuted = effectiveMuted,
                tint = iconTint,
                cutoutColor = cutoutColor,
                size = 22.dp
            )
        }
    }
}
