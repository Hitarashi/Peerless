package org.shilpo.peerless

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import org.shilpo.peerless.player.LocalPlayerConnection
import org.shilpo.peerless.player.RealPlayerConnection
import org.shilpo.peerless.player.platformSetup
import org.shilpo.peerless.theme.ExpressiveTheme
import org.shilpo.peerless.theme.rememberArtworkSeedColor
import org.shilpo.peerless.ui.shell.AdaptiveShell

@Composable
@Preview
fun App() {
    val playerConnection = remember { RealPlayerConnection() }

    LaunchedEffect(playerConnection) {
        platformSetup(playerConnection)
    }
    SideEffect {
        platformSetup(playerConnection)
    }

    val currentTrack by playerConnection.currentTrack.collectAsState()

    val currentArtworkUrl = currentTrack?.let { track ->
        playerConnection.apiClient.getArtworkUrl(track.toSummaryDto(), 300)
    }

    val dynamicSeedColor = rememberArtworkSeedColor(artworkUrl = currentArtworkUrl)

    ExpressiveTheme(seedColor = dynamicSeedColor) {
        CompositionLocalProvider(LocalPlayerConnection provides playerConnection) {
            AdaptiveShell()
        }
    }
}
