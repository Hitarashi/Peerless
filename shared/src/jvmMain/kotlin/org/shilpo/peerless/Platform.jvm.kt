package org.shilpo.peerless

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.asAwtTransferable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.net.InetAddress

class JVMPlatform : Platform {
    override val name: String = System.getProperty("os.name") ?: "Desktop"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getDeviceDisplayName(): String = runCatching {
    InetAddress.getLocalHost().hostName
}.getOrNull()?.takeIf { it.isNotBlank() } ?: "This computer"

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun Clipboard.readPlainText(): String? {
    val transferable = getClipEntry()?.asAwtTransferable ?: return null
    if (!transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) return null

    return withContext(Dispatchers.IO) {
        try {
            transferable.getTransferData(DataFlavor.stringFlavor) as? String
        } catch (_: UnsupportedFlavorException) {
            null
        } catch (_: IOException) {
            null
        }
    }
}
