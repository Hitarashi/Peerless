package org.shilpo.peerless.model

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRequest(
    val code: String,
    val device_name: String? = null,
    val platform: String? = null
)

@Serializable
data class LastFmLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LastFmIntegrationResponse(
    val connected: Boolean,
    val username: String? = null,
    val session_key: String? = null,
    val api_key: String? = null,
    val api_secret: String? = null
)

@Serializable
data class UserDto(
    val telegram_id: Long,
    val name: String? = null,
    val username: String? = null,
    val first_name: String? = null,
    val last_name: String? = null
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(first_name, last_name).joinToString(" ").takeIf { it.isNotBlank() }
            ?: username?.let { "@$it" }
            ?: "User #$telegram_id"
}

@Serializable
data class SessionDto(
    val id: String? = null,
    val session_id: String? = null,
    val device_name: String? = null,
    val platform: String? = null,
    val ip: String? = null,
    val ip_address: String? = null,
    val user_agent: String? = null,
    val created_at: String? = null,
    val last_active_at: String? = null,
    val expires_at: String? = null
) {
    val effectiveId: String get() = id ?: session_id ?: "session"
    val effectiveDevice: String
        get() = device_name?.takeIf { it.isNotBlank() } ?: (platform?.replaceFirstChar { it.uppercase() } ?: "Device")
    val effectivePlatform: String get() = platform?.takeIf { it.isNotBlank() } ?: "Native Client"
    val effectiveIp: String? get() = ip ?: ip_address
    val effectiveTimestamp: String? get() = last_active_at ?: created_at
}

@Serializable
data class ExchangeResponse(
    val token_type: String = "Bearer",
    val token: String,
    val access_token: String = token,
    val refresh_token: String = token,
    val expires_in: Long = 259200L,
    val expires_at: String = "",
    val expires_at_unix: Long = 0L,
    val user: UserDto
)

@Serializable
data class RefreshRequest(
    val refresh_token: String
)

@Serializable
data class RefreshResponse(
    val access_token: String,
    val refresh_token: String = access_token,
    val expires_in: Long = 259200L,
    val expires_at: String = "",
    val expires_at_unix: Long = 0L
)

@Serializable
data class MeResponse(
    val user: UserDto,
    val sessions: List<SessionDto> = emptyList()
)

@Serializable
data class ServerHealthDto(
    val status: String,
    val workers_total: Int = 0,
    val workers_available: Int = 0,
    val cache_entries: Long = 0,
    val cache_bytes: Long = 0,
    val uptime_seconds: Long = 0
)
