package org.shilpo.peerless

import android.os.Build
import androidx.compose.ui.platform.Clipboard

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getDeviceDisplayName(): String {
    val manufacturer = Build.MANUFACTURER.orEmpty().trim()
    val model = Build.MODEL.orEmpty().trim()
    return if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$manufacturer $model".trim()
    }.ifBlank { "Android device" }
}

internal actual suspend fun Clipboard.readPlainText(): String? {
    val clipData = getClipEntry()?.clipData ?: return null
    if (clipData.itemCount == 0) return null
    return clipData.getItemAt(0)?.text?.toString()
}
