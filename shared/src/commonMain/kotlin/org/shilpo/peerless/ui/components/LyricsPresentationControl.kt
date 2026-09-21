package org.shilpo.peerless.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.shilpo.peerless.preferences.LyricsPresentation

@Composable
internal fun LyricsPresentationControl(
    presentation: LyricsPresentation,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onToggle,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text(if (presentation == LyricsPresentation.VISUAL) "Reading mode" else "Visual mode")
    }
}
