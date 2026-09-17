package org.shilpo.peerless.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Unified deep seam for persisting the 256-bit opaque session token across platforms.
 */
interface TokenStorage {
    val tokenFlow: StateFlow<String?>

    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}

/**
 * In-memory implementation of TokenStorage for unit tests and Compose previews.
 */
class InMemoryTokenStorage(initialToken: String? = null) : TokenStorage {
    private val _tokenFlow = MutableStateFlow(initialToken)
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    override suspend fun getToken(): String? = _tokenFlow.value

    override suspend fun saveToken(token: String) {
        _tokenFlow.value = token
    }

    override suspend fun clearToken() {
        _tokenFlow.value = null
    }
}

/**
 * Factory function providing platform-specific secure storage.
 */
expect fun createPlatformTokenStorage(): TokenStorage
