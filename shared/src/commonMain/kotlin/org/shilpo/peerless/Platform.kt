package org.shilpo.peerless

import androidx.compose.ui.platform.Clipboard

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getDeviceDisplayName(): String

internal expect suspend fun Clipboard.readPlainText(): String?
