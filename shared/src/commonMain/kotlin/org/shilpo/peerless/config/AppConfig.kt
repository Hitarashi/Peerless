package org.shilpo.peerless.config

object AppConfig {
    /**
     * When true, direct stream URL resolution (`/api/v1/stream?track_id=...`) is used
     * without requiring ticket negotiation or Telegram OTP authentication.
     */
    const val IS_DEV_MODE: Boolean = true

    /**
     * Default server base URL for local development and testing.
     */
    const val DEFAULT_SERVER_URL: String = "http://192.168.0.6:4444"

    const val APP_NAME: String = "Peerless"
    const val APP_VERSION: String = "1.0.0"
    const val DEFAULT_STOREFRONT: String = "in"

    const val LASTFM_API_BASE_URL: String = "https://ws.audioscrobbler.com/2.0/"
    const val DEFAULT_LASTFM_API_KEY: String = "4a9f5581a9dac49a64613ab946a41885" // public read-only fallback key
}
