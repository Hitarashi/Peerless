package org.shilpo.peerless.ui.screens

import kotlin.test.Test
import kotlin.test.assertContains

class LastFmLoginErrorTest {
    @Test
    fun authFailuresExplainCredentialRecovery() {
        val message = actionableLastFmError(IllegalStateException("Invalid credentials"))

        assertContains(message, "username and password")
        assertContains(message, "try again")
    }

    @Test
    fun networkFailuresExplainConnectionRecovery() {
        val message = actionableLastFmError(IllegalStateException("Connection timed out"))

        assertContains(message, "internet connection")
        assertContains(message, "retry")
    }
}
