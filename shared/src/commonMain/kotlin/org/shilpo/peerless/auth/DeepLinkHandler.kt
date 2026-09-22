package org.shilpo.peerless.auth

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ConnectionCredentials(
    val serverUrl: String,
    val code: String
)

object DeepLinkHandler {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val deepLinkEventsChannel = Channel<ConnectionCredentials>(Channel.BUFFERED)
    val deepLinkEvents: Flow<ConnectionCredentials> = deepLinkEventsChannel.receiveAsFlow()

    fun handleUri(uriString: String): Boolean {
        val creds = parseDeepLink(uriString) ?: parsePayload(uriString) ?: return false
        return deepLinkEventsChannel.trySend(creds).isSuccess
    }

    fun handlePayload(payloadString: String): Boolean {
        val creds = parsePayload(payloadString) ?: return false
        return deepLinkEventsChannel.trySend(creds).isSuccess
    }

    fun parseDeepLink(uriString: String): ConnectionCredentials? {
        val trimmed = uriString.trim()
        if (!trimmed.startsWith("peerless://", ignoreCase = true)) {
            return null
        }

        val queryStart = trimmed.indexOf('?')
        if (queryStart < 0) return null

        val query = trimmed.substring(queryStart + 1)
        val params = parseQueryParams(query)

        val dataParam = params["data"]
        if (!dataParam.isNullOrBlank()) {
            return parsePayload(dataParam)
        }

        val serverParam = params["s"] ?: params["server"] ?: params["server_url"]
        val codeParam = params["c"] ?: params["code"] ?: params["otp"]
        if (!serverParam.isNullOrBlank() && !codeParam.isNullOrBlank()) {
            return ConnectionCredentials(
                serverUrl = sanitizeServerUrl(serverParam),
                code = codeParam.trim()
            )
        }

        return null
    }

    fun parsePayload(encodedPayload: String): ConnectionCredentials? {
        val trimmed = encodedPayload.trim()
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("peerless://", ignoreCase = true)) {
            return parseDeepLink(trimmed)
        }

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val creds = extractCredentialsFromJson(trimmed)
            if (creds != null) return creds
        }

        val unescaped = urlDecodeSafe(trimmed)

        if (unescaped.startsWith("{") && unescaped.endsWith("}")) {
            val creds = extractCredentialsFromJson(unescaped)
            if (creds != null) return creds
        }

        val decodedJson = decodeBase64Safe(unescaped) ?: decodeBase64Safe(trimmed)
        if (decodedJson != null) {
            val creds = extractCredentialsFromJson(decodedJson)
            if (creds != null) return creds
        }

        return null
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    fun encodePayload(serverUrl: String, code: String): String {
        val sanitized = sanitizeServerUrl(serverUrl)
        val jsonString = """{"s":"$sanitized","c":"${code.trim()}"}"""
        return kotlin.io.encoding.Base64.encode(jsonString.encodeToByteArray())
    }

    private fun extractCredentialsFromJson(jsonStr: String): ConnectionCredentials? {
        return try {
            val element = json.parseToJsonElement(jsonStr).jsonObject
            val server = element["s"]?.jsonPrimitive?.contentOrNull
                ?: element["server"]?.jsonPrimitive?.contentOrNull
                ?: element["server_url"]?.jsonPrimitive?.contentOrNull
                ?: return null

            val code = element["c"]?.jsonPrimitive?.contentOrNull
                ?: element["code"]?.jsonPrimitive?.contentOrNull
                ?: element["otp"]?.jsonPrimitive?.contentOrNull
                ?: return null

            if (server.isBlank() || code.isBlank()) return null
            ConnectionCredentials(serverUrl = sanitizeServerUrl(server), code = code.trim())
        } catch (_: Exception) {
            null
        }
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun decodeBase64Safe(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        try {
            return kotlin.io.encoding.Base64.decode(trimmed).decodeToString()
        } catch (_: Exception) {
        }

        try {
            return kotlin.io.encoding.Base64.UrlSafe.decode(trimmed).decodeToString()
        } catch (_: Exception) {
        }

        val padLen = (4 - (trimmed.length % 4)) % 4
        if (padLen > 0) {
            val padded = trimmed + "=".repeat(padLen)
            try {
                return kotlin.io.encoding.Base64.decode(padded).decodeToString()
            } catch (_: Exception) {
            }
            try {
                return kotlin.io.encoding.Base64.UrlSafe.decode(padded).decodeToString()
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val pairs = query.split('&')
        for (pair in pairs) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = urlDecodeSafe(pair.substring(0, idx))
                val value = urlDecodeSafe(pair.substring(idx + 1))
                map[key] = value
            } else if (pair.isNotEmpty()) {
                map[urlDecodeSafe(pair)] = ""
            }
        }
        return map
    }

    private fun urlDecodeSafe(str: String): String {
        if (!str.contains('%') && !str.contains('+')) return str
        val result = StringBuilder()
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (c == '+') {
                result.append(' ')
                i++
            } else if (c == '%' && i + 2 < str.length) {
                val hex = str.substring(i + 1, i + 3)
                val code = hex.toIntOrNull(16)
                if (code != null) {
                    result.append(code.toChar())
                    i += 3
                } else {
                    result.append(c)
                    i++
                }
            } else {
                result.append(c)
                i++
            }
        }
        return result.toString()
    }

    private fun sanitizeServerUrl(url: String): String {
        var clean = url.trim().trimEnd('/')
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith(
                "https://",
                ignoreCase = true
            )
        ) {
            clean = "http://$clean"
        }
        return clean
    }
}

internal suspend fun SessionManager.collectDeepLinkConnections() {
    DeepLinkHandler.deepLinkEvents.collect { credentials ->
        connectManual(credentials.serverUrl, credentials.code)
    }
}
