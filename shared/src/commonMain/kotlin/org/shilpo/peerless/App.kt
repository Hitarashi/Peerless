package org.shilpo.peerless

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import org.shilpo.peerless.config.AppConfig
import org.shilpo.peerless.network.LocalDevMode
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.LocalPlayerConnection
import org.shilpo.peerless.player.RealPlayerConnection
import org.shilpo.peerless.player.platformSetup
import org.shilpo.peerless.theme.ExpressiveTheme
import org.shilpo.peerless.theme.rememberArtworkSeedColor
import org.shilpo.peerless.ui.shell.AdaptiveShell

@Composable
@Preview
fun App() {
    val apiClient = remember { PeerlessApiClient() }
    val devModeState = remember { mutableStateOf(AppConfig.IS_DEV_MODE) }
    val playerConnection = remember(apiClient) {
        RealPlayerConnection(
            apiClient = apiClient,
            isDevMode = devModeState.value
        )
    }

    LaunchedEffect(playerConnection) {
        platformSetup(playerConnection)
    }

    val currentTrack by playerConnection.currentTrack.collectAsState()

    val currentArtworkUrl = currentTrack?.let { track ->
        apiClient.getArtworkUrl(track.toSummaryDto(), 300)
    }

    val dynamicSeedColor = rememberArtworkSeedColor(artworkUrl = currentArtworkUrl)

    ExpressiveTheme(seedColor = dynamicSeedColor) {
        CompositionLocalProvider(
            LocalPlayerConnection provides playerConnection,
            LocalPeerlessApiClient provides apiClient,
            LocalDevMode provides devModeState
        ) {
            AdaptiveShell()
        }
    }
}
