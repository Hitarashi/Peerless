package org.shilpo.peerless.auth

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.shilpo.peerless.lastfm.LastFmConfig
import org.shilpo.peerless.model.UserDto
import org.shilpo.peerless.network.PeerlessApiClient
import kotlin.time.Duration.Companion.hours

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data class Authenticated(val user: UserDto, val serverUrl: String) : SessionState
    data object Loading : SessionState
}

interface SessionManager {
    val sessionState: StateFlow<SessionState>
    val isLastFmConnected: StateFlow<Boolean>
    val lastFmUsername: StateFlow<String?>
    val lastFmConfig: StateFlow<LastFmConfig?>
    fun hasSavedCredentials(): Boolean = false
    suspend fun connectWithPayload(encodedPayload: String): Result<UserDto>
    suspend fun connectManual(serverUrl: String, code: String): Result<UserDto>
    suspend fun checkExistingSession(): Boolean
    suspend fun refreshLastFmStatus(): Boolean = false
    suspend fun loginLastFm(username: String, password: String): Result<Unit> = Result.success(Unit)
    suspend fun disconnectLastFm(): Result<Unit> = Result.success(Unit)
    suspend fun logout()
}

class RealSessionManager(
    private val apiClient: PeerlessApiClient,
    private val tokenStorage: TokenStorage,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : SessionManager {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _isLastFmConnected = MutableStateFlow(false)
    override val isLastFmConnected: StateFlow<Boolean> = _isLastFmConnected.asStateFlow()

    private val _lastFmUsername = MutableStateFlow<String?>(null)
    override val lastFmUsername: StateFlow<String?> = _lastFmUsername.asStateFlow()

    private val _lastFmConfig = MutableStateFlow<LastFmConfig?>(null)
    override val lastFmConfig: StateFlow<LastFmConfig?> = _lastFmConfig.asStateFlow()

    override fun hasSavedCredentials(): Boolean {
        return !tokenStorage.tokenFlow.value.isNullOrBlank() && !tokenStorage.serverUrlFlow.value.isNullOrBlank()
    }

    private var slidingRefreshJob: Job? = null

    init {
        scope.launch {
            DeepLinkHandler.deepLinkEvents.collect { creds ->
                if (_sessionState.value !is SessionState.Authenticated) {
                    connectManual(creds.serverUrl, creds.code)
                }
            }
        }
    }

    override suspend fun connectWithPayload(encodedPayload: String): Result<UserDto> = runCatching {
        val creds = DeepLinkHandler.parsePayload(encodedPayload)
            ?: error("Invalid connection payload: unable to extract server URL and OTP code")
        connectManual(creds.serverUrl, creds.code).getOrThrow()
    }

    override suspend fun connectManual(serverUrl: String, code: String): Result<UserDto> = runCatching {
        _sessionState.value = SessionState.Loading

        val sanitizedUrl = serverUrl.trim().trimEnd('/')
        apiClient.baseUrl = sanitizedUrl

        val exchangeResult = apiClient.exchangeOtp(code.trim())
        if (exchangeResult.isFailure) {
            _sessionState.value = SessionState.Unauthenticated
            throw exchangeResult.exceptionOrNull() ?: Exception("OTP exchange failed")
        }

        val exchangeResponse = exchangeResult.getOrThrow()
        tokenStorage.saveToken(exchangeResponse.token)
        tokenStorage.saveServerUrl(sanitizedUrl)

        val meResult = apiClient.getMe(exchangeResponse.token)
        val user = if (meResult.isSuccess) {
            meResult.getOrThrow().user
        } else {
            exchangeResponse.user
        }

        _sessionState.value = SessionState.Authenticated(user = user, serverUrl = sanitizedUrl)
        startSlidingRefresh()
        refreshLastFmStatus()
        user
    }.onFailure {
        _sessionState.value = SessionState.Unauthenticated
    }

    override suspend fun checkExistingSession(): Boolean {
        _sessionState.value = SessionState.Loading

        val savedServerUrl = tokenStorage.getServerUrl()
        val savedToken = tokenStorage.getToken()

        if (savedServerUrl.isNullOrBlank() || savedToken.isNullOrBlank()) {
            _sessionState.value = SessionState.Unauthenticated
            return false
        }

        val sanitizedUrl = savedServerUrl.trim().trimEnd('/')
        apiClient.baseUrl = sanitizedUrl

        val meResult = apiClient.getMe(savedToken)
        return if (meResult.isSuccess) {
            val user = meResult.getOrThrow().user
            _sessionState.value = SessionState.Authenticated(user = user, serverUrl = sanitizedUrl)
            startSlidingRefresh()
            refreshLastFmStatus()
            true
        } else {
            logout()
            false
        }
    }

    override suspend fun refreshLastFmStatus(): Boolean {
        return try {
            val result = apiClient.getLastFmStatus()
            if (result.isSuccess) {
                val status = result.getOrThrow()
                _isLastFmConnected.value = status.connected
                _lastFmUsername.value = status.username
                if (status.connected && status.session_key != null && status.api_key != null && status.api_secret != null) {
                    _lastFmConfig.value = LastFmConfig(
                        apiKey = status.api_key,
                        apiSecret = status.api_secret,
                        sessionKey = status.session_key,
                        username = status.username ?: ""
                    )
                } else {
                    _lastFmConfig.value = null
                }
                status.connected
            } else {
                _isLastFmConnected.value = false
                _lastFmUsername.value = null
                _lastFmConfig.value = null
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun loginLastFm(username: String, password: String): Result<Unit> = runCatching {
        val response = apiClient.loginLastFm(username.trim(), password).getOrThrow()
        _isLastFmConnected.value = response.connected
        _lastFmUsername.value = response.username
        if (response.connected && response.session_key != null && response.api_key != null && response.api_secret != null) {
            _lastFmConfig.value = LastFmConfig(
                apiKey = response.api_key,
                apiSecret = response.api_secret,
                sessionKey = response.session_key,
                username = response.username ?: ""
            )
        }
    }

    override suspend fun disconnectLastFm(): Result<Unit> = runCatching {
        apiClient.disconnectLastFm().getOrThrow()
        _isLastFmConnected.value = false
        _lastFmUsername.value = null
        _lastFmConfig.value = null
    }

    override suspend fun logout() {
        stopSlidingRefresh()
        try {
            apiClient.logout()
        } catch (_: Exception) {
        }

        tokenStorage.clearAll()
        apiClient.baseUrl = ""
        _isLastFmConnected.value = false
        _lastFmUsername.value = null
        _lastFmConfig.value = null
        _sessionState.value = SessionState.Unauthenticated
    }

    private fun startSlidingRefresh() {
        slidingRefreshJob?.cancel()
        slidingRefreshJob = scope.launch {
            while (isActive) {
                delay(24.hours)
                try {
                    val currentToken = tokenStorage.getToken()
                    if (!currentToken.isNullOrBlank()) {
                        val refreshResult = apiClient.refreshToken(currentToken)
                        if (refreshResult.isSuccess) {
                            val newAccessToken = refreshResult.getOrThrow().access_token
                            tokenStorage.saveToken(newAccessToken)
                        } else {
                            logout()
                            break
                        }
                    }
                } catch (_: Exception) {
                    // Transient failure, keep session active
                }
            }
        }
    }

    private fun stopSlidingRefresh() {
        slidingRefreshJob?.cancel()
        slidingRefreshJob = null
    }
}

val LocalSessionManager = staticCompositionLocalOf<SessionManager> {
    error("No SessionManager provided")
}
