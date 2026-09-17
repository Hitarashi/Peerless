package org.shilpo.peerless.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class IosTokenStorage : TokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val key = "peerless_session_token"

    private val _tokenFlow = MutableStateFlow<String?>(defaults.stringForKey(key))
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    override suspend fun getToken(): String? {
        val stored = defaults.stringForKey(key)
        _tokenFlow.value = stored
        return stored
    }

    override suspend fun saveToken(token: String) {
        defaults.setObject(token.trim(), forKey = key)
        _tokenFlow.value = token.trim()
    }

    override suspend fun clearToken() {
        defaults.removeObjectForKey(key)
        _tokenFlow.value = null
    }
}

actual fun createPlatformTokenStorage(): TokenStorage = IosTokenStorage()
