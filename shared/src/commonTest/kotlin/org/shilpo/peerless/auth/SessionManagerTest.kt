package org.shilpo.peerless.auth

import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.shilpo.peerless.model.ExchangeResponse
import org.shilpo.peerless.model.LastFmIntegrationResponse
import org.shilpo.peerless.model.MeResponse
import org.shilpo.peerless.model.RefreshResponse
import org.shilpo.peerless.model.UserDto
import org.shilpo.peerless.network.ApiHttpException
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.network.createDefaultPeerlessHttpClient
import org.shilpo.peerless.preferences.InMemoryAppPreferences
import org.shilpo.peerless.preferences.LyricsPresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakePeerlessApiClient(
    var exchangeResult: Result<ExchangeResponse> = Result.success(
        ExchangeResponse(
            token = "test_token_abc123",
            user = UserDto(telegram_id = 987654321L, name = "PeerlessUser")
        )
    ),
    var meResult: Result<MeResponse> = Result.success(
        MeResponse(
            user = UserDto(telegram_id = 987654321L, name = "PeerlessUser")
        )
    ),
    var logoutCalled: Boolean = false,
    var lastFmStatusProvider: suspend () -> Result<LastFmIntegrationResponse> = {
        Result.success(LastFmIntegrationResponse(connected = false))
    },
    var refreshTokenProvider: suspend (String?) -> Result<RefreshResponse> = {
        Result.success(RefreshResponse(access_token = "refreshed_token"))
    }
) : PeerlessApiClient() {
    val exchangedCodes = mutableListOf<String>()
    var exchangeProvider: suspend (String) -> Result<ExchangeResponse> = { exchangeResult }

    override suspend fun exchangeOtp(
        code: String,
        deviceName: String?,
        platform: String?
    ): Result<ExchangeResponse> {
        exchangedCodes += code
        return exchangeProvider(code)
    }

    override suspend fun getMe(token: String?): Result<MeResponse> {
        return meResult
    }

    override suspend fun getLastFmStatus(token: String?): Result<LastFmIntegrationResponse> =
        lastFmStatusProvider()

    override suspend fun refreshToken(refreshToken: String?): Result<RefreshResponse> =
        refreshTokenProvider(refreshToken)

    override suspend fun logout(refreshToken: String?): Result<Unit> {
        logoutCalled = true
        return Result.success(Unit)
    }
}

class SessionManagerTest {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun signInUsesConnectingStateWhileRequestIsPending() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val pendingExchange = CompletableDeferred<Result<ExchangeResponse>>()
        val fakeClient = FakePeerlessApiClient().apply {
            exchangeProvider = {
                requestStarted.complete(Unit)
                pendingExchange.await()
            }
        }
        val sessionManager = RealSessionManager(
            apiClient = fakeClient,
            tokenStorage = InMemoryTokenStorage(),
            scope = backgroundScope
        )
        val connectJob = backgroundScope.launch {
            sessionManager.connectManual("http://peerless.test", "ABC-123")
        }

        runCurrent()

        assertTrue(requestStarted.isCompleted)
        assertEquals(SessionState.Connecting, sessionManager.sessionState.value)

        connectJob.cancel()
        runCurrent()
        assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
    }

    @Test
    fun stalledSignInRequestTimesOutAndEndsLoadingState() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val apiClient = PeerlessApiClient(
            baseUrl = "http://peerless.test",
            httpClient = createDefaultPeerlessHttpClient(
                engine = MockEngine {
                    requestStarted.complete(Unit)
                    awaitCancellation()
                },
                requestTimeoutMillis = 25L
            )
        )
        val sessionManager = RealSessionManager(
            apiClient = apiClient,
            tokenStorage = InMemoryTokenStorage(),
            scope = backgroundScope
        )

        try {
            val result = sessionManager.connectManual("http://peerless.test", "ABC-123")

            assertTrue(requestStarted.isCompleted)
            assertTrue(
                result.exceptionOrNull()?.message?.contains(
                    "server took too long",
                    ignoreCase = true
                ) == true
            )
            assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
        } finally {
            apiClient.httpClient.close()
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun deepLinkPayloadIsExchangedOnce() = runTest {
        val code = "A1B-2CD"
        val apiClients = List(2) { FakePeerlessApiClient() }
        val sessionManagers = apiClients.map { apiClient ->
            RealSessionManager(
                apiClient = apiClient,
                tokenStorage = InMemoryTokenStorage(),
                scope = backgroundScope
            )
        }

        sessionManagers.forEach { sessionManager ->
            backgroundScope.launch { sessionManager.collectDeepLinkConnections() }
        }
        DeepLinkHandler.handlePayload(DeepLinkHandler.encodePayload("http://peerless.test", code))
        runCurrent()

        assertEquals(
            expected = 1,
            actual = apiClients.sumOf { apiClient -> apiClient.exchangedCodes.count { it == code } },
            message = "Deep-link event should submit its one-time code exactly once"
        )
    }

    @Test
    fun rawOtpInConnectionKeyFieldExplainsManualSetup() = runTest {
        val result = RealSessionManager(
            apiClient = FakePeerlessApiClient(),
            tokenStorage = InMemoryTokenStorage(),
            scope = backgroundScope
        ).connectWithPayload("ABC-123")

        assertTrue(result.exceptionOrNull()?.message?.contains("Manual server setup") == true)
    }

    @Test
    fun testDeepLinkHandlerPayloadParsing() {
        // 1. Compact payload: {"s": "http://192.168.1.100:4444", "c": "123456"}
        val compactEncoded = DeepLinkHandler.encodePayload("http://192.168.1.100:4444", "123456")
        val parsedCompact = DeepLinkHandler.parsePayload(compactEncoded)
        assertNotNull(parsedCompact)
        assertEquals("http://192.168.1.100:4444", parsedCompact.serverUrl)
        assertEquals("123456", parsedCompact.code)

        // 2. Full deep link URI: peerless://auth?data=<base64>
        val uri = "peerless://auth?data=$compactEncoded"
        val parsedUri = DeepLinkHandler.parseDeepLink(uri)
        assertNotNull(parsedUri)
        assertEquals("http://192.168.1.100:4444", parsedUri.serverUrl)
        assertEquals("123456", parsedUri.code)

        // 3. Verbose JSON payload: {"server": "https://music.peerless.io", "code": "ABCDEF"}
        val verboseJson = """{"server":"https://music.peerless.io/","code":"ABCDEF"}"""
        val parsedJson = DeepLinkHandler.parsePayload(verboseJson)
        assertNotNull(parsedJson)
        assertEquals("https://music.peerless.io", parsedJson.serverUrl)
        assertEquals("ABCDEF", parsedJson.code)

        // 4. Query params directly in URI: peerless://auth?s=http://myhost:4444&c=7890
        val directUri = "peerless://auth?s=http://myhost:4444&c=7890"
        val parsedDirect = DeepLinkHandler.parseDeepLink(directUri)
        assertNotNull(parsedDirect)
        assertEquals("http://myhost:4444", parsedDirect.serverUrl)
        assertEquals("7890", parsedDirect.code)

        // 5. Invalid payloads
        assertNull(DeepLinkHandler.parsePayload("not_a_valid_payload"))
        assertNull(DeepLinkHandler.parsePayload(""))
        assertNull(DeepLinkHandler.parseDeepLink("https://notpeerless.com/"))
    }

    @Test
    fun testTokenStorageLifecycleAndZeroKnowledgePurge() = runTest {
        val storage = InMemoryTokenStorage()

        assertNull(storage.getToken())
        assertNull(storage.getServerUrl())
        assertNull(storage.tokenFlow.value)
        assertNull(storage.serverUrlFlow.value)

        // Save token and server url
        storage.saveToken("token_xyz")
        storage.saveServerUrl("http://10.0.0.1:4444/")

        assertEquals("token_xyz", storage.getToken())
        assertEquals("token_xyz", storage.tokenFlow.value)
        assertEquals("http://10.0.0.1:4444/", storage.getServerUrl())
        assertEquals("http://10.0.0.1:4444/", storage.serverUrlFlow.value)

        // Clear token individually
        storage.clearToken()
        assertNull(storage.getToken())
        assertNull(storage.tokenFlow.value)
        assertEquals("http://10.0.0.1:4444/", storage.getServerUrl())

        // Save again and test Zero-Knowledge clearAll()
        storage.saveToken("new_token")
        assertEquals("new_token", storage.getToken())

        storage.clearAll()
        assertNull(storage.getToken())
        assertNull(storage.getServerUrl())
        assertNull(storage.tokenFlow.value)
        assertNull(storage.serverUrlFlow.value)
    }

    @Test
    fun testLyricsPresentationSurvivesLogout() = runTest {
        val preferences = InMemoryAppPreferences()
        val sessionManager = RealSessionManager(
            apiClient = FakePeerlessApiClient(),
            tokenStorage = InMemoryTokenStorage(),
            scope = backgroundScope
        )
        preferences.setLyricsPresentation(LyricsPresentation.READABLE)

        sessionManager.logout()

        assertEquals(LyricsPresentation.READABLE, preferences.lyricsPresentation.value)
    }

    @Test
    fun testConnectManualAndConnectWithPayload() = runTest {
        val storage = InMemoryTokenStorage()
        val fakeClient = FakePeerlessApiClient()
        val sessionManager = RealSessionManager(fakeClient, storage, scope = backgroundScope)

        assertEquals(SessionState.Loading, sessionManager.sessionState.value)

        val result = sessionManager.connectManual("http://127.0.0.1:4444", "test_otp_code")
        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals(987654321L, user.telegram_id)
        assertEquals("PeerlessUser", user.name)

        // Verify storage persisted
        assertEquals("test_token_abc123", storage.getToken())
        assertEquals("http://127.0.0.1:4444", storage.getServerUrl())
        assertEquals("http://127.0.0.1:4444", fakeClient.baseUrl)

        // Verify SessionState.Authenticated
        val state = sessionManager.sessionState.value
        assertTrue(state is SessionState.Authenticated)
        assertEquals(user, state.user)
        assertEquals("http://127.0.0.1:4444", state.serverUrl)

        // Test Zero-Knowledge logout
        sessionManager.logout()
        assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
        assertTrue(fakeClient.logoutCalled)
        assertNull(storage.getToken())
        assertNull(storage.getServerUrl())
        assertEquals("", fakeClient.baseUrl)

        // Now test connectWithPayload
        val payload = DeepLinkHandler.encodePayload("http://192.168.0.2:4444", "payload_code_555")
        val payloadResult = sessionManager.connectWithPayload(payload)
        assertTrue(payloadResult.isSuccess)
        assertEquals("http://192.168.0.2:4444", fakeClient.baseUrl)
        assertEquals("test_token_abc123", storage.getToken())
        assertEquals("http://192.168.0.2:4444", storage.getServerUrl())
        assertTrue(sessionManager.sessionState.value is SessionState.Authenticated)
    }

    @Test
    fun testCheckExistingSessionLifecycle() = runTest {
        val storage = InMemoryTokenStorage()
        val fakeClient = FakePeerlessApiClient()
        val sessionManager = RealSessionManager(fakeClient, storage, scope = backgroundScope)

        // 1. Initial launch with no session
        val hasSession = sessionManager.checkExistingSession()
        assertFalse(hasSession)
        assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)

        // 2. Launch with valid session saved in storage
        storage.saveServerUrl("http://saved-server:4444")
        storage.saveToken("saved_token_valid")

        val restored = sessionManager.checkExistingSession()
        assertTrue(restored)
        assertEquals("http://saved-server:4444", fakeClient.baseUrl)
        val state = sessionManager.sessionState.value
        assertTrue(state is SessionState.Authenticated)
        assertEquals("http://saved-server:4444", state.serverUrl)

        // 3. Saved session that fails authentication (e.g. expired / invalid) -> Zero-Knowledge logout
        val failingClient = FakePeerlessApiClient(
            meResult = Result.failure(ApiHttpException(401, "Token expired"))
        )
        val failingManager = RealSessionManager(failingClient, storage, scope = backgroundScope)
        val invalidSessionRestored = failingManager.checkExistingSession()
        assertFalse(invalidSessionRestored)
        assertEquals(SessionState.Unauthenticated, failingManager.sessionState.value)
        assertNull(storage.getToken())
        assertNull(storage.getServerUrl())
        assertEquals("", failingClient.baseUrl)
    }

    @Test
    fun startupKeepsCredentialsWhenServerCannotBeReached() = runTest {
        val storage = InMemoryTokenStorage(
            initialToken = "saved_token_valid",
            initialServerUrl = "http://saved-server:4444"
        )
        val fakeClient = FakePeerlessApiClient(
            meResult = Result.failure(Exception("server unavailable"))
        )
        val sessionManager = RealSessionManager(fakeClient, storage, scope = backgroundScope)

        assertFalse(sessionManager.checkExistingSession())

        assertEquals(SessionState.VerificationFailed, sessionManager.sessionState.value)
        assertEquals("saved_token_valid", storage.getToken())
        assertEquals("http://saved-server:4444", storage.getServerUrl())
        assertFalse(fakeClient.logoutCalled)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun refreshClearsCredentialsWhenServerRejectsToken() = runTest {
        val storage = InMemoryTokenStorage()
        val fakeClient = FakePeerlessApiClient().apply {
            refreshTokenProvider = { Result.failure(ApiHttpException(401, "Token expired")) }
        }
        val sessionManager = RealSessionManager(fakeClient, storage, scope = backgroundScope)

        assertTrue(sessionManager.connectManual("http://server:4444", "connection_code").isSuccess)
        runCurrent()
        advanceTimeBy(24 * 60 * 60 * 1000L)
        runCurrent()

        assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
        assertNull(storage.getToken())
        assertNull(storage.getServerUrl())
        assertTrue(fakeClient.logoutCalled)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun refreshKeepsCredentialsWhenServerCannotBeReached() = runTest {
        val storage = InMemoryTokenStorage()
        val fakeClient = FakePeerlessApiClient().apply {
            refreshTokenProvider = { Result.failure(Exception("server unavailable")) }
        }
        val sessionManager = RealSessionManager(fakeClient, storage, scope = backgroundScope)

        assertTrue(sessionManager.connectManual("http://server:4444", "connection_code").isSuccess)
        runCurrent()
        advanceTimeBy(24 * 60 * 60 * 1000L)
        runCurrent()

        assertTrue(sessionManager.sessionState.value is SessionState.Authenticated)
        assertEquals("test_token_abc123", storage.getToken())
        assertEquals("http://server:4444", storage.getServerUrl())
        assertFalse(fakeClient.logoutCalled)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun sessionRestorationWaitsForLastFmStatus() = runTest {
        val statusStarted = CompletableDeferred<Unit>()
        val pendingStatus = CompletableDeferred<Result<LastFmIntegrationResponse>>()
        val storage = InMemoryTokenStorage().apply {
            saveServerUrl("http://saved-server:4444")
            saveToken("saved_token_valid")
        }
        val fakeClient = FakePeerlessApiClient().apply {
            lastFmStatusProvider = {
                statusStarted.complete(Unit)
                pendingStatus.await()
            }
        }
        val sessionManager = RealSessionManager(fakeClient, storage, scope = backgroundScope)

        val checkJob = backgroundScope.launch { sessionManager.checkExistingSession() }
        runCurrent()

        assertTrue(statusStarted.isCompleted)
        assertEquals(SessionState.Loading, sessionManager.sessionState.value)

        pendingStatus.complete(Result.success(LastFmIntegrationResponse(connected = false)))
        checkJob.join()

        assertTrue(sessionManager.sessionState.value is SessionState.Authenticated)
        assertFalse(sessionManager.isLastFmConnected.value)
    }

    @Test
    fun testResolveStreamUrlRequiresTicket() {
        val client = PeerlessApiClient(baseUrl = "http://localhost:4444")

        // Passing null or blank ticket must throw IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            client.resolveStreamUrl(101, null)
        }
        assertFailsWith<IllegalArgumentException> {
            client.resolveStreamUrl(101, "")
        }
        assertFailsWith<IllegalArgumentException> {
            client.resolveStreamUrl(101, "   ")
        }

        // Passing ticket must return URL with ticket query param
        val streamUrl = client.resolveStreamUrl(101, "ticket_secret_token_123")
        assertEquals(
            "http://localhost:4444/api/v1/stream?ticket=ticket_secret_token_123",
            streamUrl
        )
    }
}
