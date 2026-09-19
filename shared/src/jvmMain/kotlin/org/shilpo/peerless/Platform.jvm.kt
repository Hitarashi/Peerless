package org.shilpo.peerless

import java.net.InetAddress

class JVMPlatform : Platform {
    override val name: String = System.getProperty("os.name") ?: "Desktop"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getDeviceDisplayName(): String = runCatching {
    InetAddress.getLocalHost().hostName
}.getOrNull()?.takeIf { it.isNotBlank() } ?: "This computer"

actual fun getGreetingAndDate(): Pair<String, String> {
    val now = java.time.LocalDateTime.now()
    val hour = now.hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.ENGLISH)
    val date = now.format(formatter)
    return Pair(greeting, date)
}
