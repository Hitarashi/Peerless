package org.shilpo.peerless

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getGreetingAndDate(): Pair<String, String> {
    val date = platform.Foundation.NSDate()
    val calendar = platform.Foundation.NSCalendar.currentCalendar
    val hour = calendar.component(platform.Foundation.NSCalendarUnitHour, fromDate = date).toInt()
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val formatter = platform.Foundation.NSDateFormatter().apply {
        dateFormat = "EEEE, MMMM d"
    }
    val dateStr = formatter.stringFromDate(date)
    return Pair(greeting, dateStr)
}
