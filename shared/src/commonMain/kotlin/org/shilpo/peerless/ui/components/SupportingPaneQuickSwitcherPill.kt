package org.shilpo.peerless.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.shilpo.peerless.ui.shell.SupportingPaneType
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Quick switcher capsule pill with Shilpo's asymmetric stretch-and-snap indicator physics.
 * Adapted from `shilpo/crates/surfaces/src/bar/element.rs` (`calculate_stretching_geometry`).
 */
@Composable
fun SupportingPaneQuickSwitcherPill(
    activeSupportingPane: SupportingPaneType?,
    onToggleSupportingPane: (SupportingPaneType) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    val panes = remember {
        listOf(
            SupportingPaneType.QUEUE to PeerlessIcons.Queue,
            SupportingPaneType.LYRICS to PeerlessIcons.Lyrics,
            SupportingPaneType.TRACK_CONTEXT to PeerlessIcons.InfoOutline
        )
    }

    val targetIndex = when (activeSupportingPane) {
        SupportingPaneType.QUEUE -> 0
        SupportingPaneType.LYRICS -> 1
        SupportingPaneType.TRACK_CONTEXT -> 2
        null -> null
    }

    var fromIndex by remember { mutableIntStateOf(targetIndex ?: 0) }
    var toIndex by remember { mutableIntStateOf(targetIndex ?: 0) }
    var previousTargetIndex by remember { mutableStateOf(targetIndex) }

    val motionProgress = remember { Animatable(1f) }
    val visibilityProgress = remember { Animatable(if (targetIndex != null) 1f else 0f) }

    LaunchedEffect(targetIndex) {
        val prev = previousTargetIndex
        previousTargetIndex = targetIndex

        if (targetIndex != null && prev != null && targetIndex != prev) {
            // Switching between tabs: Shilpo asymmetric stretch-and-snap physics!
            fromIndex = prev
            toIndex = targetIndex
            visibilityProgress.snapTo(1f)
            motionProgress.snapTo(0f)
            motionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = LinearEasing)
            )
            fromIndex = targetIndex
        } else if (targetIndex != null && prev == null) {
            // Opening from closed: smooth spring pop-in
            fromIndex = targetIndex
            toIndex = targetIndex
            motionProgress.snapTo(1f)
            visibilityProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
            )
        } else if (targetIndex == null && prev != null) {
            // Closing: shrink cleanly right where it currently sits (prev)
            fromIndex = prev
            toIndex = prev
            motionProgress.snapTo(1f)
            visibilityProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        }
    }

    val hitTargetSizeDp = 48.dp
    val iconSlotSizeDp = 36.dp
    val slotGapDp = 2.dp
    val hitTargetSizePx = with(density) { hitTargetSizeDp.toPx() }
    val iconSlotSizePx = with(density) { iconSlotSizeDp.toPx() }
    val slotGapPx = with(density) { slotGapDp.toPx() }
    val slotStepPx = hitTargetSizePx + slotGapPx

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.75f))
            .padding(2.dp)
    ) {
        // Shilpo Elastic Stretching Pill Indicator
        if (visibilityProgress.value > 0.01f) {
            val visualInset = (hitTargetSizePx - iconSlotSizePx) / 2f
            val fromPos = fromIndex * slotStepPx + visualInset
            val toPos = toIndex * slotStepPx + visualInset
            val p = motionProgress.value.coerceIn(0f, 1f)

            // Sine ease-out progress curve (out_sine in shilpo)
            fun outSine(progress: Float): Float {
                return sin(progress.coerceIn(0f, 1f) * (PI / 2).toFloat())
            }

            val headProgress = outSine((p * 1.5f).coerceAtMost(1f))
            val tailProgress = outSine(((p - 0.2f) / 0.8f).coerceAtLeast(0f))

            val movingForward = toPos >= fromPos
            val indicatorPos: Float
            val indicatorWidth: Float
            if (movingForward) {
                val head = fromPos + iconSlotSizePx + (toPos - fromPos) * headProgress
                val tail = fromPos + (toPos - fromPos) * tailProgress
                indicatorPos = tail
                indicatorWidth = maxOf(head - tail, iconSlotSizePx)
            } else {
                val head = fromPos + (toPos - fromPos) * headProgress
                val tail = fromPos + iconSlotSizePx + (toPos - fromPos) * tailProgress
                indicatorPos = head
                indicatorWidth = maxOf(tail - head, iconSlotSizePx)
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = indicatorPos.roundToInt(),
                            y = ((hitTargetSizePx - iconSlotSizePx) / 2f).roundToInt()
                        )
                    }
                    .size(
                        width = with(density) { indicatorWidth.toDp() },
                        height = iconSlotSizeDp
                    )
                    .scale(visibilityProgress.value)
                    .clip(CircleShape)
                    .background(colorScheme.primaryContainer)
            )
        }

        // Foreground Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(slotGapDp)
        ) {
            panes.forEachIndexed { _, (type, icon) ->
                val isSelected = activeSupportingPane == type
                val interactionSource = remember(type) { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val isPressed by interactionSource.collectIsPressedAsState()
                val glowAlpha by animateFloatAsState(
                    targetValue = if (isHovered && !isSelected) 0.12f else 0f,
                    animationSpec = tween(160),
                    label = "SwitcherIconGlow"
                )
                val pressScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = tween(100),
                    label = "SwitcherIconPress"
                )
                val iconTint by animateColorAsState(
                    targetValue = when {
                        isSelected -> colorScheme.onPrimaryContainer
                        isHovered -> colorScheme.primary
                        else -> colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    },
                    animationSpec = tween(180),
                    label = "SwitcherIconTint"
                )

                Box(
                    modifier = Modifier
                        .size(hitTargetSizeDp)
                        .hoverable(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onToggleSupportingPane(type) }
                        )
                        .pointerHoverIcon(PointerIcon.Hand)
                        .semantics {
                            role = Role.Button
                            selected = isSelected
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconSlotSizeDp)
                            .clip(CircleShape)
                            .drawBehind {
                                if (glowAlpha > 0f) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                colorScheme.primary.copy(alpha = glowAlpha),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = size.minDimension * 0.62f
                                        )
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (type == SupportingPaneType.LYRICS) {
                            LyricsMorphIcon(
                                selected = isSelected,
                                modifier = Modifier.scale(pressScale),
                                tint = iconTint,
                                size = 18.dp,
                                contentDescription = type.title
                            )
                        } else {
                            PeerlessIcon(
                                icon = if (type == SupportingPaneType.TRACK_CONTEXT && isSelected) {
                                    PeerlessIcons.InfoFilled
                                } else {
                                    icon
                                },
                                contentDescription = type.title,
                                tint = iconTint,
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(pressScale)
                            )
                        }
                    }
                }
            }
        }
    }
}
