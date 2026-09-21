package org.shilpo.peerless

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.shilpo.peerless.auth.LocalSessionManager
import org.shilpo.peerless.auth.RealSessionManager
import org.shilpo.peerless.auth.collectDeepLinkConnections
import org.shilpo.peerless.auth.createPlatformTokenStorage
import org.shilpo.peerless.config.AppConfig
import org.shilpo.peerless.home.HomeFeedRepository
import org.shilpo.peerless.lastfm.LastFmClient
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.LocalPlayerConnection
import org.shilpo.peerless.player.RealPlayerConnection
import org.shilpo.peerless.player.platformSetup
import org.shilpo.peerless.preferences.AppPreferences
import org.shilpo.peerless.preferences.LocalAppPreferences
import org.shilpo.peerless.preferences.createPlatformAppPreferences
import org.shilpo.peerless.sync.LocalPlaybackSyncManager
import org.shilpo.peerless.sync.PlaybackSyncManager
import org.shilpo.peerless.theme.ExpressiveTheme
import org.shilpo.peerless.theme.rememberArtworkSeedColor
import org.shilpo.peerless.ui.posture.FlatWindowPostureProvider
import org.shilpo.peerless.ui.posture.LocalWindowPostureProvider
import org.shilpo.peerless.ui.posture.WindowPostureProvider
import org.shilpo.peerless.ui.shell.AdaptiveShell

@Composable
@Preview
fun App(
    appPreferences: AppPreferences? = null,
    windowPostureProvider: WindowPostureProvider = FlatWindowPostureProvider()
) {
    val preferences = remember(appPreferences) {
        appPreferences ?: createPlatformAppPreferences()
    }
    val tokenStorage = remember { createPlatformTokenStorage() }
    val apiClient = remember(tokenStorage) { PeerlessApiClient(tokenStorage = tokenStorage) }
    val sessionManager = remember(apiClient, tokenStorage) {
        RealSessionManager(apiClient = apiClient, tokenStorage = tokenStorage)
    }
    val lastFmConfig by sessionManager.lastFmConfig.collectAsState()
    val syncManager = remember(tokenStorage) { PlaybackSyncManager(tokenStorage = tokenStorage) }
    val playerConnection = remember(apiClient, syncManager) {
        RealPlayerConnection(apiClient = apiClient).apply {
            attachSync(syncManager)
        }
    }
    val homeFeedRepository = remember(apiClient, lastFmConfig?.apiKey) {
        HomeFeedRepository(
            lastFmClient = LastFmClient(
                apiKey = lastFmConfig?.apiKey ?: AppConfig.DEFAULT_LASTFM_API_KEY,
                httpClient = apiClient.httpClient,
                enableFallback = false
            ),
            apiClient = apiClient
        )
    }
    val favoritesManager = remember(apiClient) {
        org.shilpo.peerless.library.RealFavoritesManager(apiClient = apiClient)
    }
    val scrobbler = remember(playerConnection) {
        org.shilpo.peerless.lastfm.LastFmScrobbler().apply {
            attachToPlayer(playerConnection)
        }
    }

    LaunchedEffect(playerConnection, syncManager) {
        playerConnection.attachSync(syncManager)
    }

    LaunchedEffect(playerConnection, homeFeedRepository) {
        playerConnection.configureRadioRecommendations(homeFeedRepository)
    }

    LaunchedEffect(playerConnection) {
        platformSetup(playerConnection)
    }

    LaunchedEffect(syncManager) {
        syncManager.isSelfActiveDevice.collect { isActive ->
            scrobbler.isSelfActivePlaybackDevice = isActive
        }
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
        sessionManager.sessionState.collect { state ->
            if (state is org.shilpo.peerless.auth.SessionState.Authenticated) {
                val token = tokenStorage.getToken()
                if (!token.isNullOrBlank()) {
                    syncManager.start(state.serverUrl, token)
                }
            } else {
                syncManager.stop()
            }
        }
    }

    LaunchedEffect(sessionManager) {
        sessionManager.lastFmConfig.collect { config ->
            scrobbler.configure(config)
        }
    }

    LaunchedEffect(sessionManager) {
        sessionManager.collectDeepLinkConnections()
    }

    val currentTrack by playerConnection.currentTrack.collectAsState()

    val currentArtworkUrl = currentTrack?.let { track ->
        apiClient.getArtworkUrl(track.toSummaryDto(), 300)
    }

    val dynamicSeedColor = rememberArtworkSeedColor(artworkUrl = currentArtworkUrl)

    ExpressiveTheme(seedColor = dynamicSeedColor) {
        CompositionLocalProvider(
            LocalPlayerConnection provides playerConnection,
            LocalPlaybackSyncManager provides syncManager,
            LocalPeerlessApiClient provides apiClient,
            LocalSessionManager provides sessionManager,
            org.shilpo.peerless.home.LocalHomeFeedRepository provides homeFeedRepository,
            org.shilpo.peerless.library.LocalFavoritesManager provides favoritesManager,
            LocalAppPreferences provides preferences,
            LocalWindowPostureProvider provides windowPostureProvider
        ) {
            AdaptiveShell()
        }
    }
}
