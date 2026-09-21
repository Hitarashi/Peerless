package org.shilpo.peerless.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeHeaderTest {
    @Test
    fun avatarInitialsHandleNamesAndTelegramFallbacks() {
        assertEquals("SH", avatarInitials("Sayeed Hitarashi"))
        assertEquals("S", avatarInitials("@sayeeddev"))
        assertEquals("U", avatarInitials("User #123"))
        assertEquals("U", avatarInitials(" "))
    }
}
