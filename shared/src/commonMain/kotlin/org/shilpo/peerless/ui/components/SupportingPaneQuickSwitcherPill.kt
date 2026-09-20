package org.shilpo.peerless.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
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
            SupportingPaneType.SIGNAL_PATH to PeerlessIcons.SignalPath
        )
    }

    val targetIndex = when (activeSupportingPane) {
        SupportingPaneType.QUEUE -> 0
        SupportingPaneType.LYRICS -> 1
        SupportingPaneType.SIGNAL_PATH -> 2
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

    val slotSizeDp = 36.dp
    val slotGapDp = 2.dp
    val slotSizePx = with(density) { slotSizeDp.toPx() }
    val slotGapPx = with(density) { slotGapDp.toPx() }
    val slotStepPx = slotSizePx + slotGapPx

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.75f))
            .padding(2.dp)
    ) {
        // Shilpo Elastic Stretching Pill Indicator
        if (visibilityProgress.value > 0.01f) {
            val fromPos = fromIndex * slotStepPx
            val toPos = toIndex * slotStepPx
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
                val head = fromPos + slotSizePx + (toPos - fromPos) * headProgress
                val tail = fromPos + (toPos - fromPos) * tailProgress
                indicatorPos = tail
                indicatorWidth = maxOf(head - tail, slotSizePx)
            } else {
                val head = fromPos + (toPos - fromPos) * headProgress
                val tail = fromPos + slotSizePx + (toPos - fromPos) * tailProgress
                indicatorPos = head
                indicatorWidth = maxOf(tail - head, slotSizePx)
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = indicatorPos.roundToInt(),
                            y = 0
                        )
                    }
                    .size(
                        width = with(density) { indicatorWidth.toDp() },
                        height = slotSizeDp
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
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant.copy(
                        alpha = 0.8f
                    ),
                    animationSpec = tween(180),
                    label = "SwitcherIconTint"
                )

                Box(
                    modifier = Modifier
                        .size(slotSizeDp)
                        .clip(CircleShape)
                        .clickable { onToggleSupportingPane(type) }
                        .pointerHoverIcon(PointerIcon.Hand),
                    contentAlignment = Alignment.Center
                ) {
                    if (type == SupportingPaneType.LYRICS) {
                        LyricsMorphIcon(
                            selected = isSelected,
                            tint = iconTint,
                            size = 18.dp,
                            contentDescription = type.title
                        )
                    } else {
                        PeerlessIcon(
                            icon = icon,
                            contentDescription = type.title,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
