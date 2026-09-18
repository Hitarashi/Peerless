package org.shilpo.peerless

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getGreetingAndDate(): Pair<String, String>
