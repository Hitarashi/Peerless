package org.shilpo.peerless

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getGreetingAndDate(): Pair<String, String> {
    val cal = java.util.Calendar.getInstance()
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val sdf = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.ENGLISH)
    val date = sdf.format(cal.time)
    return Pair(greeting, date)
}
