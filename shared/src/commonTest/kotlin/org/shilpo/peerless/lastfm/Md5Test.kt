package org.shilpo.peerless.lastfm

import kotlin.test.Test
import kotlin.test.assertEquals

class Md5Test {
    @Test
    fun hexMatchesRfc1321TestVectors() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Md5.hex(""))
        assertEquals("0cc175b9c0f1b6a831c399e269772661", Md5.hex("a"))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Md5.hex("abc"))
        assertEquals("f96b697d7cb7938d525a2f31aaf161d0", Md5.hex("message digest"))
        assertEquals(
            "c3fcd3d76192e4007dfb496cca67e13b",
            Md5.hex("abcdefghijklmnopqrstuvwxyz")
        )
    }
}
