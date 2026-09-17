package org.shilpo.peerless

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform