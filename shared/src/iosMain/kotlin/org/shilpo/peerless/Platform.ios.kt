package org.shilpo.peerless

import androidx.compose.ui.platform.Clipboard
import platform.UIKit.UIDevice
import platform.UIKit.UIPasteboard

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getDeviceDisplayName(): String =
    UIDevice.currentDevice.name.takeIf { it.isNotBlank() } ?: "Apple device"

internal actual suspend fun Clipboard.readPlainText(): String? {
    getClipEntry() ?: return null
    return UIPasteboard.generalPasteboard.string
}
