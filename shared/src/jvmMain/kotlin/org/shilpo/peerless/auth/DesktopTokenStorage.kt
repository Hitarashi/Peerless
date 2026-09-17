package org.shilpo.peerless.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DesktopTokenStorage : TokenStorage {
    private val tokenFile: File by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        val configDir = File(userHome, ".config/peerless")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        File(configDir, "session.token")
    }

    private val _tokenFlow = MutableStateFlow(loadTokenSync())
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private fun loadTokenSync(): String? {
        return try {
            if (tokenFile.exists()) {
                val content = tokenFile.readText().trim()
                content.ifBlank { null }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getToken(): String? = _tokenFlow.value

    override suspend fun saveToken(token: String) {
        try {
            tokenFile.writeText(token.trim())
            try {
                tokenFile.setReadable(false, false)
                tokenFile.setReadable(true, true)
                tokenFile.setWritable(true, true)
            } catch (_: Exception) {
            }
            _tokenFlow.value = token.trim()
        } catch (_: Exception) {
        }
    }

    override suspend fun clearToken() {
        try {
            if (tokenFile.exists()) {
                tokenFile.delete()
            }
        } catch (_: Exception) {
        }
        _tokenFlow.value = null
    }
}

actual fun createPlatformTokenStorage(): TokenStorage = DesktopTokenStorage()
