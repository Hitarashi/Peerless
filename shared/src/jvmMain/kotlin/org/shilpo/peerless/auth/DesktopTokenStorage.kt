package org.shilpo.peerless.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DesktopTokenStorage : TokenStorage {
    private val configDir: File by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = File(userHome, ".config/peerless")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val tokenFile: File by lazy { File(configDir, "session.token") }
    private val serverUrlFile: File by lazy { File(configDir, "server.url") }

    private val _tokenFlow = MutableStateFlow(loadFile(tokenFile))
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val _serverUrlFlow = MutableStateFlow(loadFile(serverUrlFile))
    override val serverUrlFlow: StateFlow<String?> = _serverUrlFlow.asStateFlow()

    private fun loadFile(file: File): String? {
        return try {
            if (file.exists()) {
                val content = file.readText().trim()
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

    override suspend fun getServerUrl(): String? = _serverUrlFlow.value

    override suspend fun saveServerUrl(url: String) {
        try {
            serverUrlFile.writeText(url.trim())
            _serverUrlFlow.value = url.trim()
        } catch (_: Exception) {
        }
    }

    override suspend fun clearServerUrl() {
        try {
            if (serverUrlFile.exists()) {
                serverUrlFile.delete()
            }
        } catch (_: Exception) {
        }
        _serverUrlFlow.value = null
    }

    override suspend fun clearAll() {
        clearToken()
        clearServerUrl()
    }
}

actual fun createPlatformTokenStorage(): TokenStorage = DesktopTokenStorage()
