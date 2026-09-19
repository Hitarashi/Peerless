package org.shilpo.peerless.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface TokenStorage {
    val tokenFlow: StateFlow<String?>
    val serverUrlFlow: StateFlow<String?>

    /** Stable for this app installation; intentionally survives sign-out. */
    fun getOrCreateDeviceId(): String

    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
    suspend fun getServerUrl(): String?
    suspend fun saveServerUrl(url: String)
    suspend fun clearServerUrl()
    suspend fun clearAll()
}

class InMemoryTokenStorage(
    initialToken: String? = null,
    initialServerUrl: String? = null,
    private val deviceId: String = newDeviceId()
) : TokenStorage {
    private val _tokenFlow = MutableStateFlow(initialToken)
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _serverUrlFlow = MutableStateFlow(initialServerUrl)
    override val serverUrlFlow: StateFlow<String?> = _serverUrlFlow.asStateFlow()

    override fun getOrCreateDeviceId(): String = deviceId

    override suspend fun getToken(): String? = _tokenFlow.value

    override suspend fun saveToken(token: String) {
        _tokenFlow.value = token.trim()
    }

    override suspend fun clearToken() {
        _tokenFlow.value = null
    }

    override suspend fun getServerUrl(): String? = _serverUrlFlow.value

    override suspend fun saveServerUrl(url: String) {
        _serverUrlFlow.value = url.trim()
    }

    override suspend fun clearServerUrl() {
        _serverUrlFlow.value = null
    }

    override suspend fun clearAll() {
        _tokenFlow.value = null
        _serverUrlFlow.value = null
    }
}

internal fun newDeviceId(): String =
    "device_" + kotlin.random.Random.nextLong().toULong().toString(16)

expect fun createPlatformTokenStorage(): TokenStorage
