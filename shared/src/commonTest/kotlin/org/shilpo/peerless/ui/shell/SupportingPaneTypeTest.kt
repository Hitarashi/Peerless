package org.shilpo.peerless.ui.shell

import kotlin.test.Test
import kotlin.test.assertEquals

class SupportingPaneTypeTest {
    @Test
    fun `queue lyrics and track context are available as supporting panes`() {
        assertEquals(
            listOf(
                SupportingPaneType.QUEUE,
                SupportingPaneType.LYRICS,
                SupportingPaneType.TRACK_CONTEXT,
                SupportingPaneType.TASKS
            ),
            SupportingPaneType.entries.toList()
        )
    }
}
