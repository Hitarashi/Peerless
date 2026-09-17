package org.shilpo.peerless

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.shilpo.peerless.player.PlaybackCoordinator
import org.shilpo.peerless.theme.ExpressiveTheme
import org.shilpo.peerless.theme.rememberArtworkSeedColor
import org.shilpo.peerless.ui.shell.AdaptiveShell

@Composable
@Preview
fun App() {
    val coordinator = remember { PlaybackCoordinator() }
    val playerState by coordinator.state.collectAsState()

    val currentArtworkUrl = playerState.currentTrack?.let { track ->
        if (track.id > 0 && playerState.serverUrl.isNotBlank()) {
            "${playerState.serverUrl.trimEnd('/')}/api/v1/assets/tracks/${track.id}/artwork"
        } else {
            track.artwork_url
        }
    }

    val dynamicSeedColor = rememberArtworkSeedColor(artworkUrl = currentArtworkUrl)

    ExpressiveTheme(seedColor = dynamicSeedColor) {
        AdaptiveShell(coordinator = coordinator)
    }
}