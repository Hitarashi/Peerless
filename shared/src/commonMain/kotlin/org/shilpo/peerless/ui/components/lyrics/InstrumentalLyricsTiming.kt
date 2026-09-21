package org.shilpo.peerless.ui.components.lyrics

import org.shilpo.peerless.model.LyricsLineDto

internal fun isInstrumentalLineVisible(
    line: LyricsLineDto,
    fallbackEndMs: Long?,
    positionMs: Long
): Boolean {
    if (!line.is_instrumental) return false

    val endMs = when {
        line.end_ms > line.start_ms -> line.end_ms
        fallbackEndMs != null && fallbackEndMs > line.start_ms -> fallbackEndMs
        else -> return false
    }

    return positionMs >= line.start_ms && positionMs < endMs
}
