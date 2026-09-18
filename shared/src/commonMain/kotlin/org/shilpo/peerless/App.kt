package org.shilpo.peerless

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import org.shilpo.peerless.auth.DeepLinkHandler
import org.shilpo.peerless.auth.LocalSessionManager
import org.shilpo.peerless.auth.RealSessionManager
import org.shilpo.peerless.auth.createPlatformTokenStorage
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
    val tokenStorage = remember { createPlatformTokenStorage() }
    val apiClient = remember(tokenStorage) { PeerlessApiClient(tokenStorage = tokenStorage) }
    val sessionManager = remember(apiClient, tokenStorage) {
        RealSessionManager(apiClient = apiClient, tokenStorage = tokenStorage)
    }
    val playerConnection = remember(apiClient) {
        RealPlayerConnection(apiClient = apiClient)
    }
    val favoritesManager = remember(apiClient) {
        org.shilpo.peerless.library.RealFavoritesManager(apiClient = apiClient)
    }

    LaunchedEffect(playerConnection) {
        platformSetup(playerConnection)
    }

    LaunchedEffect(sessionManager) {
        sessionManager.checkExistingSession()
    }

    LaunchedEffect(sessionManager) {
        sessionManager.sessionState.collect { state ->
            if (state is org.shilpo.peerless.auth.SessionState.Authenticated) {
                favoritesManager.refreshFavorites()
            }
        }
    }

    LaunchedEffect(sessionManager) {
        DeepLinkHandler.deepLinkEvents.collect { creds ->
            sessionManager.connectManual(creds.serverUrl, creds.code)
        }
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
            LocalSessionManager provides sessionManager,
            org.shilpo.peerless.library.LocalFavoritesManager provides favoritesManager
        ) {
            AdaptiveShell()
        }
    }
}
