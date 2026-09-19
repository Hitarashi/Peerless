package org.shilpo.peerless.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class IosTokenStorage : TokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val tokenKey = "peerless_session_token"
    private val serverUrlKey = "peerless_server_url"
    private val deviceIdKey = "peerless_playback_device_id"

    private val _tokenFlow = MutableStateFlow<String?>(defaults.stringForKey(tokenKey))
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _serverUrlFlow = MutableStateFlow<String?>(defaults.stringForKey(serverUrlKey))
    override val serverUrlFlow: StateFlow<String?> = _serverUrlFlow.asStateFlow()

    override fun getOrCreateDeviceId(): String {
        return defaults.stringForKey(deviceIdKey) ?: newDeviceId().also { generated ->
            defaults.setObject(generated, forKey = deviceIdKey)
        }
    }

    override suspend fun getToken(): String? {
        val stored = defaults.stringForKey(tokenKey)
        _tokenFlow.value = stored
        return stored
    }

    override suspend fun saveToken(token: String) {
        defaults.setObject(token.trim(), forKey = tokenKey)
        _tokenFlow.value = token.trim()
    }

    override suspend fun clearToken() {
        defaults.removeObjectForKey(tokenKey)
        _tokenFlow.value = null
    }

    override suspend fun getServerUrl(): String? {
        val stored = defaults.stringForKey(serverUrlKey)
        _serverUrlFlow.value = stored
        return stored
    }

    override suspend fun saveServerUrl(url: String) {
        defaults.setObject(url.trim(), forKey = serverUrlKey)
        _serverUrlFlow.value = url.trim()
    }

    override suspend fun clearServerUrl() {
        defaults.removeObjectForKey(serverUrlKey)
        _serverUrlFlow.value = null
    }

    override suspend fun clearAll() {
        defaults.removeObjectForKey(tokenKey)
        defaults.removeObjectForKey(serverUrlKey)
        _tokenFlow.value = null
        _serverUrlFlow.value = null
    }
}

actual fun createPlatformTokenStorage(): TokenStorage = IosTokenStorage()
