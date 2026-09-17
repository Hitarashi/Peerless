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

    private val prefs: SharedPreferences?
        get() = contextProvider()?.getSharedPreferences("peerless_auth", Context.MODE_PRIVATE)

    private val _tokenFlow = MutableStateFlow<String?>(null)
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    init {
        _tokenFlow.value = prefs?.getString("session_token", null)
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
}

actual fun createPlatformTokenStorage(): TokenStorage = AndroidTokenStorage()
