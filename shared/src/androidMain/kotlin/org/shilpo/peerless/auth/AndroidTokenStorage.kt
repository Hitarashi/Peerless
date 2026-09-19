package org.shilpo.peerless.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AndroidContextProvider {
    var context: Context? = null
}

class AndroidTokenStorage(
    private val contextProvider: () -> Context? = { AndroidContextProvider.context }
) : TokenStorage {

    private val deviceIdKey = "playback_device_id"

    private val prefs: SharedPreferences?
        get() = contextProvider()?.getSharedPreferences("peerless_auth", Context.MODE_PRIVATE)

    private val _tokenFlow = MutableStateFlow<String?>(null)
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _serverUrlFlow = MutableStateFlow<String?>(null)
    override val serverUrlFlow: StateFlow<String?> = _serverUrlFlow.asStateFlow()

    override fun getOrCreateDeviceId(): String = synchronized(deviceIdLock) {
        prefs?.getString(deviceIdKey, null) ?: newDeviceId().also { generated ->
            prefs?.edit()?.putString(deviceIdKey, generated)?.commit()
        }
    }

    init {
        _tokenFlow.value = prefs?.getString("session_token", null)
        _serverUrlFlow.value = prefs?.getString("server_url", null)
    }

    override suspend fun getToken(): String? {
        val stored = prefs?.getString("session_token", null)
        if (stored != null) {
            _tokenFlow.value = stored
        }
        return _tokenFlow.value
    }

    override suspend fun saveToken(token: String) {
        prefs?.edit()?.putString("session_token", token.trim())?.apply()
        _tokenFlow.value = token.trim()
    }

    override suspend fun clearToken() {
        prefs?.edit()?.remove("session_token")?.apply()
        _tokenFlow.value = null
    }

    override suspend fun getServerUrl(): String? {
        val stored = prefs?.getString("server_url", null)
        if (stored != null) {
            _serverUrlFlow.value = stored
        }
        return _serverUrlFlow.value
    }

    override suspend fun saveServerUrl(url: String) {
        prefs?.edit()?.putString("server_url", url.trim())?.apply()
        _serverUrlFlow.value = url.trim()
    }

    override suspend fun clearServerUrl() {
        prefs?.edit()?.remove("server_url")?.apply()
        _serverUrlFlow.value = null
    }

    override suspend fun clearAll() {
        prefs?.edit()?.remove("session_token")?.remove("server_url")?.apply()
        _tokenFlow.value = null
        _serverUrlFlow.value = null
    }

    private companion object {
        val deviceIdLock = Any()
    }
}

actual fun createPlatformTokenStorage(): TokenStorage = AndroidTokenStorage()
