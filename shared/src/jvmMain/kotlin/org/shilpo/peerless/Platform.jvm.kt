package org.shilpo.peerless

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

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
