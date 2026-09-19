package org.shilpo.peerless.lastfm

/**
 * Pure Kotlin Multiplatform RFC 1321 MD5 message digest.
 * Used for Last.fm API signature (`api_sig`) generation across JVM, Android, iOS, and Desktop.
 */
object Md5 {
    private const val HEX_DIGITS = "0123456789abcdef"

    fun hex(input: String): String {
        val bytes = input.encodeToByteArray()
        val digest = digest(bytes)
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                val unsignedByte = byte.toInt() and 0xFF
                append(HEX_DIGITS[unsignedByte ushr 4])
                append(HEX_DIGITS[unsignedByte and 0x0F])
            }
        }
    }

    fun digest(input: ByteArray): ByteArray {
        val messageLen = input.size
        val numBlocks = ((messageLen + 8) ushr 6) + 1
        val totalLen = numBlocks shl 6
        val paddingBytes = ByteArray(totalLen - messageLen)
        paddingBytes[0] = 0x80.toByte()

        val messageLenBits = (messageLen.toLong()) shl 3
        for (i in 0 until 8) {
            paddingBytes[paddingBytes.size - 8 + i] = ((messageLenBits ushr (8 * i)) and 0xFF).toByte()
        }

        var a = 0x67452301
        var b = 0xEFCDAB89.toInt()
        var c = 0x98BADCFE.toInt()
        var d = 0x10325476

        val buffer = IntArray(16)
        for (i in 0 until numBlocks) {
            val offset = i shl 6
            for (j in 0 until 16) {
                val byteOffset = offset + (j shl 2)
                val b0 = getByte(input, paddingBytes, messageLen, byteOffset)
                val b1 = getByte(input, paddingBytes, messageLen, byteOffset + 1)
                val b2 = getByte(input, paddingBytes, messageLen, byteOffset + 2)
                val b3 = getByte(input, paddingBytes, messageLen, byteOffset + 3)
                buffer[j] = (b0 and 0xFF) or
                        ((b1 and 0xFF) shl 8) or
                        ((b2 and 0xFF) shl 16) or
                        ((b3 and 0xFF) shl 24)
            }

            var aa = a
            var bb = b
            var cc = c
            var dd = d

            // Round 1
            aa = ff(aa, bb, cc, dd, buffer[0], 7, 0xD76AA478.toInt())
            dd = ff(dd, aa, bb, cc, buffer[1], 12, 0xE8C7B756.toInt())
            cc = ff(cc, dd, aa, bb, buffer[2], 17, 0x242070DB)
            bb = ff(bb, cc, dd, aa, buffer[3], 22, 0xC1BDCEEE.toInt())
            aa = ff(aa, bb, cc, dd, buffer[4], 7, 0xF57C0FAF.toInt())
            dd = ff(dd, aa, bb, cc, buffer[5], 12, 0x4787C62A)
            cc = ff(cc, dd, aa, bb, buffer[6], 17, 0xA8304613.toInt())
            bb = ff(bb, cc, dd, aa, buffer[7], 22, 0xFD469501.toInt())
            aa = ff(aa, bb, cc, dd, buffer[8], 7, 0x698098D8)
            dd = ff(dd, aa, bb, cc, buffer[9], 12, 0x8B44F7AF.toInt())
            cc = ff(cc, dd, aa, bb, buffer[10], 17, 0xFFFF5BB1.toInt())
            bb = ff(bb, cc, dd, aa, buffer[11], 22, 0x895CD7BE.toInt())
            aa = ff(aa, bb, cc, dd, buffer[12], 7, 0x6B901122)
            dd = ff(dd, aa, bb, cc, buffer[13], 12, 0xFD987193.toInt())
            cc = ff(cc, dd, aa, bb, buffer[14], 17, 0xA679438E.toInt())
            bb = ff(bb, cc, dd, aa, buffer[15], 22, 0x49B40821)

            // Round 2
            aa = gg(aa, bb, cc, dd, buffer[1], 5, 0xF61E2562.toInt())
            dd = gg(dd, aa, bb, cc, buffer[6], 9, 0xC040B340.toInt())
            cc = gg(cc, dd, aa, bb, buffer[11], 14, 0x265E5A51)
            bb = gg(bb, cc, dd, aa, buffer[0], 20, 0xE9B6C7AA.toInt())
            aa = gg(aa, bb, cc, dd, buffer[5], 5, 0xD62F105D.toInt())
            dd = gg(dd, aa, bb, cc, buffer[10], 9, 0x02441453)
            cc = gg(cc, dd, aa, bb, buffer[15], 14, 0xD8A1E681.toInt())
            bb = gg(bb, cc, dd, aa, buffer[4], 20, 0xE7D3FBC8.toInt())
            aa = gg(aa, bb, cc, dd, buffer[9], 5, 0x21E1CDE6)
            dd = gg(dd, aa, bb, cc, buffer[14], 9, 0xC33707D6.toInt())
            cc = gg(cc, dd, aa, bb, buffer[3], 14, 0xF4D50D87.toInt())
            bb = gg(bb, cc, dd, aa, buffer[8], 20, 0x455A14ED)
            aa = gg(aa, bb, cc, dd, buffer[13], 5, 0xA9E3E905.toInt())
            dd = gg(dd, aa, bb, cc, buffer[2], 9, 0xFCEFA3F8.toInt())
            cc = gg(cc, dd, aa, bb, buffer[7], 14, 0x676F02D9)
            bb = gg(bb, cc, dd, aa, buffer[12], 20, 0x8D2A4C8A.toInt())

            // Round 3
            aa = hh(aa, bb, cc, dd, buffer[5], 4, 0xFFFA3942.toInt())
            dd = hh(dd, aa, bb, cc, buffer[8], 11, 0x8771F681.toInt())
            cc = hh(cc, dd, aa, bb, buffer[11], 16, 0x6D9D6122)
            bb = hh(bb, cc, dd, aa, buffer[14], 23, 0xFDE5380C.toInt())
            aa = hh(aa, bb, cc, dd, buffer[1], 4, 0xA4BEEA44.toInt())
            dd = hh(dd, aa, bb, cc, buffer[4], 11, 0x4BDECFA9)
            cc = hh(cc, dd, aa, bb, buffer[7], 16, 0xF6BB4B60.toInt())
            bb = hh(bb, cc, dd, aa, buffer[10], 23, 0xBEBFBC70.toInt())
            aa = hh(aa, bb, cc, dd, buffer[13], 4, 0x289B7EC6)
            dd = hh(dd, aa, bb, cc, buffer[0], 11, 0xEAA127FA.toInt())
            cc = hh(cc, dd, aa, bb, buffer[3], 16, 0xD4EF3085.toInt())
            bb = hh(bb, cc, dd, aa, buffer[6], 23, 0x04881D05)
            aa = hh(aa, bb, cc, dd, buffer[9], 4, 0xD9D4D039.toInt())
            dd = hh(dd, aa, bb, cc, buffer[12], 11, 0xE6DB99E5.toInt())
            cc = hh(cc, dd, aa, bb, buffer[15], 16, 0x1FA27CF8)
            bb = hh(bb, cc, dd, aa, buffer[2], 23, 0xC4AC5665.toInt())

            // Round 4
            aa = ii(aa, bb, cc, dd, buffer[0], 6, 0xF4292244.toInt())
            dd = ii(dd, aa, bb, cc, buffer[7], 10, 0x432AFF97)
            cc = ii(cc, dd, aa, bb, buffer[14], 15, 0xAB9423A7.toInt())
            bb = ii(bb, cc, dd, aa, buffer[5], 21, 0xFC93A039.toInt())
            aa = ii(aa, bb, cc, dd, buffer[12], 6, 0x655B59C3)
            dd = ii(dd, aa, bb, cc, buffer[3], 10, 0x8F0CCC92.toInt())
            cc = ii(cc, dd, aa, bb, buffer[10], 15, 0xFFEFF47D.toInt())
            bb = ii(bb, cc, dd, aa, buffer[1], 21, 0x85845DD1.toInt())
            aa = ii(aa, bb, cc, dd, buffer[8], 6, 0x6FA87E4F)
            dd = ii(dd, aa, bb, cc, buffer[15], 10, 0xFE2CE6E0.toInt())
            cc = ii(cc, dd, aa, bb, buffer[6], 15, 0xA3014314.toInt())
            bb = ii(bb, cc, dd, aa, buffer[13], 21, 0x4E0811A1)
            aa = ii(aa, bb, cc, dd, buffer[4], 6, 0xF7537E82.toInt())
            dd = ii(dd, aa, bb, cc, buffer[11], 10, 0xBD3AF235.toInt())
            cc = ii(cc, dd, aa, bb, buffer[2], 15, 0x2AD7D2BB)
            bb = ii(bb, cc, dd, aa, buffer[9], 21, 0xEB86D391.toInt())

            a += aa
            b += bb
            c += cc
            d += dd
        }

        val result = ByteArray(16)
        writeInt(result, 0, a)
        writeInt(result, 4, b)
        writeInt(result, 8, c)
        writeInt(result, 12, d)
        return result
    }

    private fun getByte(input: ByteArray, padding: ByteArray, messageLen: Int, offset: Int): Int {
        return if (offset < messageLen) {
            input[offset].toInt()
        } else {
            padding[offset - messageLen].toInt()
        }
    }

    private fun writeInt(out: ByteArray, offset: Int, value: Int) {
        out[offset] = (value and 0xFF).toByte()
        out[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        out[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        out[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun ff(a: Int, b: Int, c: Int, d: Int, x: Int, s: Int, ac: Int): Int {
        val f = (b and c) or (b.inv() and d)
        return rotateLeft(a + f + x + ac, s) + b
    }

    private fun gg(a: Int, b: Int, c: Int, d: Int, x: Int, s: Int, ac: Int): Int {
        val g = (b and d) or (c and d.inv())
        return rotateLeft(a + g + x + ac, s) + b
    }

    private fun hh(a: Int, b: Int, c: Int, d: Int, x: Int, s: Int, ac: Int): Int {
        val h = b xor c xor d
        return rotateLeft(a + h + x + ac, s) + b
    }

    private fun ii(a: Int, b: Int, c: Int, d: Int, x: Int, s: Int, ac: Int): Int {
        val i = c xor (b or d.inv())
        return rotateLeft(a + i + x + ac, s) + b
    }

    private fun rotateLeft(x: Int, n: Int): Int = (x shl n) or (x ushr (32 - n))
}
