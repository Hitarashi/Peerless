package org.shilpo.peerless.ui.shell

/**
 * Contextual supporting pane types for Expanded (>= 840dp) viewports,
 * conforming to the Google Canonical Supporting Pane layout.
 */
enum class SupportingPaneType(val title: String) {
    QUEUE("Play Queue"),
    LYRICS("Synced Lyrics"),
    SIGNAL_PATH("Signal Path")
}
