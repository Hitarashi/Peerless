package org.shilpo.peerless

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.shilpo.peerless.theme.ExpressiveTheme
import org.shilpo.peerless.ui.shell.AdaptiveShell

@Composable
@Preview
fun App() {
    ExpressiveTheme {
        AdaptiveShell()
    }
}