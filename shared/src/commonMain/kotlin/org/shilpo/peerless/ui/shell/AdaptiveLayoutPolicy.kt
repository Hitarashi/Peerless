package org.shilpo.peerless.ui.shell

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class SupportingPaneLayout(
    val isAvailable: Boolean,
    val width: Dp
)

internal fun supportingPaneLayout(
    totalWidth: Dp,
    requestedPaneWidth: Dp
): SupportingPaneLayout {
    val maxPaneWidth =
        totalWidth - EXPANDED_RAIL_WIDTH - PRIMARY_PANE_MIN_WIDTH - PANE_SPLITTER_WIDTH
    if (maxPaneWidth < SUPPORTING_PANE_MIN_WIDTH) {
        return SupportingPaneLayout(isAvailable = false, width = 0.dp)
    }
    return SupportingPaneLayout(
        isAvailable = true,
        width = requestedPaneWidth.coerceIn(SUPPORTING_PANE_MIN_WIDTH, SUPPORTING_PANE_MAX_WIDTH)
            .coerceAtMost(maxPaneWidth)
    )
}

private val EXPANDED_RAIL_WIDTH = 224.dp
private val PRIMARY_PANE_MIN_WIDTH = 560.dp
private val SUPPORTING_PANE_MIN_WIDTH = 280.dp
private val SUPPORTING_PANE_MAX_WIDTH = 560.dp
private val PANE_SPLITTER_WIDTH = 48.dp
