package org.shilpo.peerless.player

import kotlinx.coroutines.*
import org.shilpo.peerless.model.RepeatMode
import org.shilpo.peerless.model.Track
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.StandardProtocolFamily
import java.net.URI
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class MprisServer(
    private val playerConnection: PlayerConnection,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val serverBaseUrl: String? = null
) {
    @Volatile
    private var isRunning = false
    private var socketChannel: SocketChannel? = null
    private var outputStream: OutputStream? = null
    private var nextSerial = 1

    private val artDir = File(System.getProperty("user.home") + "/.cache/peerless/art").apply { mkdirs() }
    private val downloadingTracks = ConcurrentHashMap.newKeySet<Int>()

    fun start() {
        val osName = System.getProperty("os.name")?.lowercase() ?: ""
        if (!osName.contains("linux")) return

        val uid = runCatching {
            ProcessHandle.current().info().user().orElse("1000")
        }.getOrDefault("1000").filter { it.isDigit() }.ifEmpty { "1000" }

        val socketPaths = listOfNotNull(
            System.getenv("DBUS_SESSION_BUS_ADDRESS")?.let { addr ->
                if (addr.startsWith("unix:path=")) addr.substring(10).split(",").firstOrNull() else null
            },
            "/run/user/$uid/bus",
            "/tmp/dbus-session-$uid"
        )
        val socketPath = socketPaths.firstOrNull { File(it).exists() } ?: return

        isRunning = true
        scope.launch {
            while (isRunning) {
                try {
                    connectAndRun(socketPath)
                } catch (e: Exception) {
                    if (isRunning) {
                        delay(1000)
                    }
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            socketChannel?.close()
        } catch (_: Exception) {
        }
        socketChannel = null
        outputStream = null
    }

    private suspend fun connectAndRun(path: String) = withContext(Dispatchers.IO) {
        val channel = SocketChannel.open(StandardProtocolFamily.UNIX)
        channel.connect(UnixDomainSocketAddress.of(path))
        socketChannel = channel

        val os = Channels.newOutputStream(channel)
        val `is` = Channels.newInputStream(channel)
        outputStream = os

        try {
            val uid = runCatching {
                ProcessHandle.current().info().user().orElse("1000")
            }.getOrDefault("1000").filter { it.isDigit() }.ifEmpty { "1000" }

            val hexUid = buildString {
                for (b in uid.toByteArray(StandardCharsets.US_ASCII)) {
                    append(b.toInt().and(0xFF).toString(16).padStart(2, '0'))
                }
            }

            os.write(0)
            os.write("AUTH EXTERNAL $hexUid\r\n".toByteArray(StandardCharsets.US_ASCII))
            os.flush()

            val lineBuf = ByteArrayOutputStream()
            var b: Int
            while (`is`.read().also { b = it } != -1) {
                if (b == '\n'.code) break
                if (b != '\r'.code) lineBuf.write(b)
            }
            val authResp = lineBuf.toString(StandardCharsets.US_ASCII.name())
            if (!authResp.startsWith("OK")) {
                channel.close()
                return@withContext
            }

            os.write("BEGIN\r\n".toByteArray(StandardCharsets.US_ASCII))
            os.flush()

            sendHello(os)
            val helloResp = readMessage(`is`)
            if (helloResp == null) {
                channel.close()
                return@withContext
            }

            sendRequestName(os, "org.mpris.MediaPlayer2.peerless")
            readMessage(`is`)

            val observers = startStateObservers()

            val headerBuf = ByteArray(16)
            try {
                while (isRunning && channel.isOpen) {
                    val bytesRead = `is`.readNBytes(headerBuf, 0, 16)
                    if (bytesRead < 16) break

                    val hdr = ByteBuffer.wrap(headerBuf).order(ByteOrder.LITTLE_ENDIAN)
                    hdr.get()
                    val msgType = hdr.get().toInt()
                    val flags = hdr.get().toInt()
                    hdr.get()
                    val bodyLen = hdr.getInt()
                    val serial = hdr.getInt()
                    val fieldsLen = hdr.getInt()

                    val fieldsPad = (8 - (fieldsLen % 8)) % 8
                    val fieldsBytes = ByteArray(fieldsLen + fieldsPad)
                    `is`.readNBytes(fieldsBytes, 0, fieldsBytes.size)

                    val bodyBytes = ByteArray(bodyLen)
                    if (bodyLen > 0) {
                        `is`.readNBytes(bodyBytes, 0, bodyLen)
                    }

                    if (msgType == 1) {
                        try {
                            handleMethodCall(serial, fieldsBytes, fieldsLen, bodyBytes, os)
                        } catch (_: Exception) {
                        }
                    }
                }
            } finally {
                observers.forEach { it.cancel() }
            }
        } finally {
            try {
                channel.close()
            } catch (_: Exception) {
            }
            socketChannel = null
            outputStream = null
        }
    }

    private fun startStateObservers(): List<Job> {
        val j1 = scope.launch {
            playerConnection.isPlaying.collect {
                notifyPropertiesChanged()
            }
        }
        val j2 = scope.launch {
            playerConnection.currentTrack.collect {
                notifyPropertiesChanged()
            }
        }
        val j3 = scope.launch {
            playerConnection.status.collect {
                notifyPropertiesChanged()
            }
        }
        val j4 = scope.launch {
            playerConnection.repeatMode.collect {
                notifyPropertiesChanged()
            }
        }
        val j5 = scope.launch {
            playerConnection.shuffleMode.collect {
                notifyPropertiesChanged()
            }
        }
        return listOf(j1, j2, j3, j4, j5)
    }

    private fun getOrFetchLocalArtUrl(track: Track): String? {
        val cachedFile = File(artDir, "${track.id}.jpg")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return "file://${cachedFile.absolutePath}"
        }

        var url = track.artworkUrl
        if (url.isNullOrBlank() && track.id > 0) {
            val serverUrl = serverBaseUrl
                ?: System.getenv("PEERLESS_SERVER_URL")
                ?: "http://127.0.0.1:4444"
            url = "$serverUrl/api/v1/assets/tracks/${track.id}/artwork?size=600"
        }
        if (url.isNullOrBlank()) return null

        if (url.startsWith("file://")) return url

        if (url.startsWith("http://") || url.startsWith("https://")) {
            if (downloadingTracks.add(track.id)) {
                scope.launch(Dispatchers.IO) {
                    try {
                        downloadArt(url, cachedFile)
                        if (cachedFile.exists() && cachedFile.length() > 0) {
                            val current = playerConnection.currentTrack.value
                            if (current != null && current.id == track.id) {
                                notifyPropertiesChanged()
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        downloadingTracks.remove(track.id)
                    }
                }
            }
            return url
        }

        return url
    }

    private fun downloadArt(urlStr: String, targetFile: File) {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 5) {
            val conn = (URI.create(currentUrl).toURL().openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "Peerless/1.0")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location") ?: break
                currentUrl = if (loc.startsWith("http")) loc else URI.create(currentUrl).resolve(loc).toString()
                redirects++
                conn.disconnect()
                continue
            }
            if (code == 200) {
                val tmp = File(targetFile.parentFile, "${targetFile.name}.tmp")
                conn.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tmp.renameTo(targetFile)
            }
            conn.disconnect()
            break
        }
    }

    private fun notifyPropertiesChanged() {
        val os = outputStream ?: return
        try {
            val body = ByteArrayOutputStream()
            writeDbusString(body, "org.mpris.MediaPlayer2.Player")

            val dictEntries = ByteArrayOutputStream()
            writePropertyEntry(dictEntries, "PlaybackStatus", "s") { out ->
                val statusStr = when (playerConnection.status.value) {
                    PlaybackStatus.PLAYING -> "Playing"
                    PlaybackStatus.PAUSED -> "Paused"
                    else -> "Stopped"
                }
                writeDbusString(out, statusStr)
            }

            val track = playerConnection.currentTrack.value
            if (track != null) {
                writePropertyEntry(dictEntries, "Metadata", "a{sv}") { out ->
                    writeMetadataDict(out, track)
                }
            }

            writePropertyEntry(dictEntries, "CanGoNext", "b") { out ->
                val can = if (playerConnection.canSkipNext.value) 1 else 0
                val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(can)
                out.write(bb.array())
            }

            writePropertyEntry(dictEntries, "CanGoPrevious", "b") { out ->
                val can = if (playerConnection.canSkipPrevious.value) 1 else 0
                val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(can)
                out.write(bb.array())
            }

            val loopStr = when (playerConnection.repeatMode.value) {
                RepeatMode.ONE -> "Track"
                RepeatMode.ALL -> "Playlist"
                else -> "None"
            }
            writePropertyEntry(dictEntries, "LoopStatus", "s") { out ->
                writeDbusString(out, loopStr)
            }

            writePropertyEntry(dictEntries, "Shuffle", "b") { out ->
                val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(if (playerConnection.shuffleMode.value) 1 else 0)
                out.write(bb.array())
            }

            val dictBytes = dictEntries.toByteArray()
            val dictPad = (4 - (body.size() % 4)) % 4
            for (i in 0 until dictPad) body.write(0)
            val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dictBytes.size)
            body.write(lenBuf.array())

            val structPad = (8 - (body.size() % 8)) % 8
            for (i in 0 until structPad) body.write(0)
            body.write(dictBytes)

            val emptyInvalidated = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array()
            val invPad = (4 - (body.size() % 4)) % 4
            for (i in 0 until invPad) body.write(0)
            body.write(emptyInvalidated)

            val bodyBytes = body.toByteArray()
            val fields = ByteArrayOutputStream()
            writeField(fields, 1, "o", "/org/mpris/MediaPlayer2")
            writeField(fields, 2, "s", "org.freedesktop.DBus.Properties")
            writeField(fields, 3, "s", "PropertiesChanged")
            writeField(fields, 8, "g", "sa{sv}as")

            sendDbusMessage(os, 4, 0, fields.toByteArray(), bodyBytes)
        } catch (_: Exception) {
        }
    }

    private fun handleMethodCall(
        serial: Int,
        fieldsBytes: ByteArray,
        fieldsLen: Int,
        bodyBytes: ByteArray,
        os: OutputStream
    ) {
        var member = ""
        var iface = ""
        var path = ""
        var sender = ""

        var offset = 0
        while (offset < fieldsLen) {
            val pad = (8 - (offset % 8)) % 8
            offset += pad
            if (offset >= fieldsLen) break
            val code = fieldsBytes[offset].toInt() and 0xFF
            offset += 1
            if (offset >= fieldsLen) break
            val sigLen = fieldsBytes[offset].toInt() and 0xFF
            offset += 1
            if (offset + sigLen + 1 > fieldsLen) break
            val sig = String(fieldsBytes, offset, sigLen, StandardCharsets.US_ASCII)
            offset += sigLen + 1

            when (sig) {
                "s", "o" -> {
                    val strPad = (4 - (offset % 4)) % 4
                    offset += strPad
                    if (offset + 4 > fieldsLen) break
                    val strLen = ByteBuffer.wrap(fieldsBytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    offset += 4
                    if (strLen < 0 || offset + strLen + 1 > fieldsLen) break
                    val str = String(fieldsBytes, offset, strLen, StandardCharsets.UTF_8)
                    offset += strLen + 1

                    when (code) {
                        1 -> path = str
                        2 -> iface = str
                        3 -> member = str
                        7 -> sender = str
                    }
                }

                "g" -> {
                    if (offset >= fieldsLen) break
                    val gLen = fieldsBytes[offset].toInt() and 0xFF
                    offset += 1
                    if (offset + gLen + 1 > fieldsLen) break
                    val gStr = String(fieldsBytes, offset, gLen, StandardCharsets.US_ASCII)
                    offset += gLen + 1
                }

                "u" -> {
                    val up = (4 - (offset % 4)) % 4
                    offset += up
                    if (offset + 4 > fieldsLen) break
                    val uVal = ByteBuffer.wrap(fieldsBytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    offset += 4
                }

                else -> {
                    break
                }
            }
        }

        when (member) {
            "Play" -> {
                scope.launch { playerConnection.play() }
                sendEmptyReturn(os, sender, serial)
            }

            "Pause" -> {
                scope.launch { playerConnection.pause() }
                sendEmptyReturn(os, sender, serial)
            }

            "PlayPause" -> {
                scope.launch { playerConnection.togglePlayPause() }
                sendEmptyReturn(os, sender, serial)
            }

            "Next" -> {
                scope.launch { playerConnection.playNext() }
                sendEmptyReturn(os, sender, serial)
            }

            "Previous" -> {
                scope.launch { playerConnection.playPrevious() }
                sendEmptyReturn(os, sender, serial)
            }

            "Stop" -> {
                scope.launch { playerConnection.stopAndDismiss() }
                sendEmptyReturn(os, sender, serial)
            }

            "Seek" -> {
                if (bodyBytes.size >= 8) {
                    val offsetUs = ByteBuffer.wrap(bodyBytes, 0, 8).order(ByteOrder.LITTLE_ENDIAN).long
                    val newPos = playerConnection.currentPositionMs + (offsetUs / 1000L)
                    scope.launch { playerConnection.seekTo(newPos.coerceAtLeast(0L)) }
                }
                sendEmptyReturn(os, sender, serial)
            }

            "SetPosition" -> {
                if (bodyBytes.size >= 16) {
                    val posUs = ByteBuffer.wrap(bodyBytes, 8, 8).order(ByteOrder.LITTLE_ENDIAN).long
                    scope.launch { playerConnection.seekTo((posUs / 1000L).coerceAtLeast(0L)) }
                }
                sendEmptyReturn(os, sender, serial)
            }

            "Introspect" -> {
                val xml = buildIntrospectionXml()
                val body = ByteArrayOutputStream()
                writeDbusString(body, xml)
                sendMethodReturn(os, sender, serial, "s", body.toByteArray())
            }

            "Get" -> {
                val (targetIface, nextOff) = readDbusString(bodyBytes, 0)
                val (propName, _) = readDbusString(bodyBytes, nextOff)
                handleGetProperty(os, sender, serial, targetIface, propName)
            }

            "GetAll" -> {
                val (targetIface, _) = readDbusString(bodyBytes, 0)
                val body = ByteArrayOutputStream()
                val dict = ByteArrayOutputStream()
                if (targetIface == "org.mpris.MediaPlayer2") {
                    writeRootProperties(dict)
                } else {
                    writePlayerProperties(dict)
                }

                val dictBytes = dict.toByteArray()
                val dictPad = (4 - (body.size() % 4)) % 4
                for (i in 0 until dictPad) body.write(0)
                val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dictBytes.size)
                body.write(lenBuf.array())

                val structPad = (8 - (body.size() % 8)) % 8
                for (i in 0 until structPad) body.write(0)
                body.write(dictBytes)
                sendMethodReturn(os, sender, serial, "a{sv}", body.toByteArray())
            }

            else -> {
                sendEmptyReturn(os, sender, serial)
            }
        }
    }

    private fun handleGetProperty(
        os: OutputStream,
        destination: String,
        replySerial: Int,
        iface: String,
        prop: String
    ) {
        when (iface) {
            "org.mpris.MediaPlayer2" -> {
                when (prop) {
                    "Identity" -> sendMethodReturnVariantString(os, destination, replySerial, "Peerless")
                    "DesktopEntry" -> sendMethodReturnVariantString(os, destination, replySerial, "peerless")
                    "CanQuit" -> sendMethodReturnVariantBoolean(os, destination, replySerial, true)
                    "CanRaise" -> sendMethodReturnVariantBoolean(os, destination, replySerial, false)
                    "HasTrackList" -> sendMethodReturnVariantBoolean(os, destination, replySerial, false)
                    "SupportedUriSchemes" -> sendMethodReturnVariantStringArray(
                        os,
                        destination,
                        replySerial,
                        emptyList()
                    )

                    "SupportedMimeTypes" -> sendMethodReturnVariantStringArray(
                        os,
                        destination,
                        replySerial,
                        emptyList()
                    )

                    else -> sendEmptyReturn(os, destination, replySerial)
                }
            }

            "org.mpris.MediaPlayer2.Player" -> {
                when (prop) {
                    "PlaybackStatus" -> {
                        val status = when (playerConnection.status.value) {
                            PlaybackStatus.PLAYING -> "Playing"
                            PlaybackStatus.PAUSED -> "Paused"
                            else -> "Stopped"
                        }
                        sendMethodReturnVariantString(os, destination, replySerial, status)
                    }

                    "LoopStatus" -> {
                        val loop = when (playerConnection.repeatMode.value) {
                            RepeatMode.ONE -> "Track"
                            RepeatMode.ALL -> "Playlist"
                            else -> "None"
                        }
                        sendMethodReturnVariantString(os, destination, replySerial, loop)
                    }

                    "Rate" -> sendMethodReturnVariantDouble(os, destination, replySerial, 1.0)
                    "Shuffle" -> sendMethodReturnVariantBoolean(
                        os,
                        destination,
                        replySerial,
                        playerConnection.shuffleMode.value
                    )

                    "Metadata" -> sendMethodReturnVariantMetadata(
                        os,
                        destination,
                        replySerial,
                        playerConnection.currentTrack.value
                    )

                    "Volume" -> sendMethodReturnVariantDouble(
                        os,
                        destination,
                        replySerial,
                        playerConnection.volume.value.toDouble()
                    )

                    "Position" -> sendMethodReturnVariantInt64(
                        os,
                        destination,
                        replySerial,
                        playerConnection.currentPositionMs * 1000L
                    )

                    "CanControl" -> sendMethodReturnVariantBoolean(os, destination, replySerial, true)
                    "CanPlay" -> sendMethodReturnVariantBoolean(os, destination, replySerial, true)
                    "CanPause" -> sendMethodReturnVariantBoolean(os, destination, replySerial, true)
                    "CanSeek" -> sendMethodReturnVariantBoolean(os, destination, replySerial, true)
                    "CanGoNext" -> sendMethodReturnVariantBoolean(
                        os,
                        destination,
                        replySerial,
                        playerConnection.canSkipNext.value
                    )

                    "CanGoPrevious" -> sendMethodReturnVariantBoolean(
                        os,
                        destination,
                        replySerial,
                        playerConnection.canSkipPrevious.value
                    )

                    "MinimumRate" -> sendMethodReturnVariantDouble(os, destination, replySerial, 1.0)
                    "MaximumRate" -> sendMethodReturnVariantDouble(os, destination, replySerial, 1.0)
                    else -> sendEmptyReturn(os, destination, replySerial)
                }
            }

            else -> {
                if (iface.isEmpty()) {
                    if (prop in setOf(
                            "Identity",
                            "DesktopEntry",
                            "CanQuit",
                            "CanRaise",
                            "HasTrackList",
                            "SupportedUriSchemes",
                            "SupportedMimeTypes"
                        )
                    ) {
                        handleGetProperty(os, destination, replySerial, "org.mpris.MediaPlayer2", prop)
                    } else if (prop in setOf(
                            "PlaybackStatus",
                            "LoopStatus",
                            "Rate",
                            "Shuffle",
                            "Metadata",
                            "Volume",
                            "Position",
                            "CanControl",
                            "CanPlay",
                            "CanPause",
                            "CanSeek",
                            "CanGoNext",
                            "CanGoPrevious",
                            "MinimumRate",
                            "MaximumRate"
                        )
                    ) {
                        handleGetProperty(os, destination, replySerial, "org.mpris.MediaPlayer2.Player", prop)
                    } else {
                        sendEmptyReturn(os, destination, replySerial)
                    }
                } else {
                    sendEmptyReturn(os, destination, replySerial)
                }
            }
        }
    }

    private fun writeRootProperties(dict: ByteArrayOutputStream) {
        writePropertyEntry(dict, "CanQuit", "b") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(1)
            out.write(bb.array())
        }
        writePropertyEntry(dict, "CanRaise", "b") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0)
            out.write(bb.array())
        }
        writePropertyEntry(dict, "HasTrackList", "b") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0)
            out.write(bb.array())
        }
        writePropertyEntry(dict, "Identity", "s") { out ->
            writeDbusString(out, "Peerless")
        }
        writePropertyEntry(dict, "DesktopEntry", "s") { out ->
            writeDbusString(out, "peerless")
        }
        writePropertyEntry(dict, "SupportedUriSchemes", "as") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0)
            out.write(bb.array())
        }
        writePropertyEntry(dict, "SupportedMimeTypes", "as") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0)
            out.write(bb.array())
        }
    }

    private fun writePlayerProperties(dict: ByteArrayOutputStream) {
        writePropertyEntry(dict, "PlaybackStatus", "s") { out ->
            val statusStr = when (playerConnection.status.value) {
                PlaybackStatus.PLAYING -> "Playing"
                PlaybackStatus.PAUSED -> "Paused"
                else -> "Stopped"
            }
            writeDbusString(out, statusStr)
        }

        writePropertyEntry(dict, "LoopStatus", "s") { out ->
            val loopStr = when (playerConnection.repeatMode.value) {
                RepeatMode.ONE -> "Track"
                RepeatMode.ALL -> "Playlist"
                else -> "None"
            }
            writeDbusString(out, loopStr)
        }

        writePropertyEntry(dict, "Rate", "d") { out ->
            val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(1.0)
            out.write(bb.array())
        }

        writePropertyEntry(dict, "Shuffle", "b") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(if (playerConnection.shuffleMode.value) 1 else 0)
            out.write(bb.array())
        }

        val track = playerConnection.currentTrack.value
        if (track != null) {
            writePropertyEntry(dict, "Metadata", "a{sv}") { out ->
                writeMetadataDict(out, track)
            }
        } else {
            writePropertyEntry(dict, "Metadata", "a{sv}") { out ->
                val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0)
                out.write(lenBuf.array())
            }
        }

        writePropertyEntry(dict, "Volume", "d") { out ->
            val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                .putDouble(playerConnection.volume.value.toDouble())
            out.write(bb.array())
        }

        writePropertyEntry(dict, "Position", "x") { out ->
            val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                .putLong(playerConnection.currentPositionMs * 1000L)
            out.write(bb.array())
        }

        writePropertyEntry(dict, "CanControl", "b") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(1)
            out.write(bb.array())
        }

        writePropertyEntry(dict, "CanPlay", "b") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(1)
            out.write(bb.array())
        }

        writePropertyEntry(dict, "CanPause", "b") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(1)
            out.write(bb.array())
        }

        writePropertyEntry(dict, "CanSeek", "b") { out ->
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(1)
            out.write(bb.array())
        }

        writePropertyEntry(dict, "CanGoNext", "b") { out ->
            val can = if (playerConnection.canSkipNext.value) 1 else 0
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(can)
            out.write(bb.array())
        }

        writePropertyEntry(dict, "CanGoPrevious", "b") { out ->
            val can = if (playerConnection.canSkipPrevious.value) 1 else 0
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(can)
            out.write(bb.array())
        }

        writePropertyEntry(dict, "MinimumRate", "d") { out ->
            val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(1.0)
            out.write(bb.array())
        }

        writePropertyEntry(dict, "MaximumRate", "d") { out ->
            val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(1.0)
            out.write(bb.array())
        }
    }

    private fun readDbusString(bytes: ByteArray, offset: Int): Pair<String, Int> {
        val sp = (4 - (offset % 4)) % 4
        val strOffset = offset + sp
        if (strOffset + 4 > bytes.size) return Pair("", strOffset)
        val len = ByteBuffer.wrap(bytes, strOffset, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val start = strOffset + 4
        if (len < 0 || start + len > bytes.size) return Pair("", bytes.size)
        val str = String(bytes, start, len, StandardCharsets.UTF_8)
        val nextOffset = if (start + len < bytes.size && bytes[start + len] == 0.toByte()) {
            start + len + 1
        } else {
            start + len
        }
        return Pair(str, nextOffset)
    }

    private fun sendMethodReturnVariantString(os: OutputStream, destination: String, replySerial: Int, value: String) {
        val body = ByteArrayOutputStream()
        val sig = "s".toByteArray(StandardCharsets.US_ASCII)
        body.write(sig.size)
        body.write(sig)
        body.write(0)
        val pad = (4 - (body.size() % 4)) % 4
        for (i in 0 until pad) body.write(0)
        writeDbusString(body, value)
        sendMethodReturn(os, destination, replySerial, "v", body.toByteArray())
    }

    private fun sendMethodReturnVariantBoolean(
        os: OutputStream,
        destination: String,
        replySerial: Int,
        value: Boolean
    ) {
        val body = ByteArrayOutputStream()
        val sig = "b".toByteArray(StandardCharsets.US_ASCII)
        body.write(sig.size)
        body.write(sig)
        body.write(0)
        val pad = (4 - (body.size() % 4)) % 4
        for (i in 0 until pad) body.write(0)
        val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(if (value) 1 else 0)
        body.write(bb.array())
        sendMethodReturn(os, destination, replySerial, "v", body.toByteArray())
    }

    private fun sendMethodReturnVariantDouble(os: OutputStream, destination: String, replySerial: Int, value: Double) {
        val body = ByteArrayOutputStream()
        val sig = "d".toByteArray(StandardCharsets.US_ASCII)
        body.write(sig.size)
        body.write(sig)
        body.write(0)
        val pad = (8 - (body.size() % 8)) % 8
        for (i in 0 until pad) body.write(0)
        val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value)
        body.write(bb.array())
        sendMethodReturn(os, destination, replySerial, "v", body.toByteArray())
    }

    private fun sendMethodReturnVariantInt64(os: OutputStream, destination: String, replySerial: Int, value: Long) {
        val body = ByteArrayOutputStream()
        val sig = "x".toByteArray(StandardCharsets.US_ASCII)
        body.write(sig.size)
        body.write(sig)
        body.write(0)
        val pad = (8 - (body.size() % 8)) % 8
        for (i in 0 until pad) body.write(0)
        val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value)
        body.write(bb.array())
        sendMethodReturn(os, destination, replySerial, "v", body.toByteArray())
    }

    private fun sendMethodReturnVariantStringArray(
        os: OutputStream,
        destination: String,
        replySerial: Int,
        list: List<String>
    ) {
        val body = ByteArrayOutputStream()
        val sig = "as".toByteArray(StandardCharsets.US_ASCII)
        body.write(sig.size)
        body.write(sig)
        body.write(0)
        val pad = (4 - (body.size() % 4)) % 4
        for (i in 0 until pad) body.write(0)

        val arrOut = ByteArrayOutputStream()
        for (item in list) {
            writeDbusString(arrOut, item)
        }
        val arrBytes = arrOut.toByteArray()
        val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(arrBytes.size)
        body.write(lenBuf.array())
        body.write(arrBytes)
        sendMethodReturn(os, destination, replySerial, "v", body.toByteArray())
    }

    private fun sendMethodReturnVariantMetadata(
        os: OutputStream,
        destination: String,
        replySerial: Int,
        track: Track?
    ) {
        val body = ByteArrayOutputStream()
        val sig = "a{sv}".toByteArray(StandardCharsets.US_ASCII)
        body.write(sig.size)
        body.write(sig)
        body.write(0)
        val pad = (4 - (body.size() % 4)) % 4
        for (i in 0 until pad) body.write(0)

        if (track != null) {
            writeMetadataDict(body, track)
        } else {
            val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0)
            body.write(lenBuf.array())
        }
        sendMethodReturn(os, destination, replySerial, "v", body.toByteArray())
    }

    private fun writeMetadataDict(out: ByteArrayOutputStream, track: Track) {
        val metaEntries = ByteArrayOutputStream()
        writeMetadataEntries(metaEntries, track)
        val entriesBytes = metaEntries.toByteArray()

        val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(entriesBytes.size)
        out.write(lenBuf.array())

        val structPad = (8 - (out.size() % 8)) % 8
        for (i in 0 until structPad) out.write(0)
        out.write(entriesBytes)
    }

    private fun writeMetadataEntries(out: ByteArrayOutputStream, track: Track) {
        writePropertyEntry(out, "mpris:trackid", "o") { b ->
            writeDbusString(b, "/org/shilpo/peerless/track/${track.id}")
        }
        writePropertyEntry(out, "mpris:length", "x") { b ->
            val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(track.durationMs * 1000L)
            b.write(bb.array())
        }
        writePropertyEntry(out, "xesam:title", "s") { b ->
            writeDbusString(b, track.title)
        }
        writePropertyEntry(out, "xesam:artist", "as") { b ->
            val listBytes = ByteArrayOutputStream()
            writeDbusString(listBytes, track.artist)
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(listBytes.size())
            b.write(bb.array())
            b.write(listBytes.toByteArray())
        }
        writePropertyEntry(out, "xesam:album", "s") { b ->
            writeDbusString(b, track.album)
        }
        val art = getOrFetchLocalArtUrl(track)
        if (!art.isNullOrBlank()) {
            writePropertyEntry(out, "mpris:artUrl", "s") { b ->
                writeDbusString(b, art)
            }
        }
    }

    private fun sendEmptyReturn(os: OutputStream, destination: String, replySerial: Int) {
        val fields = ByteArrayOutputStream()
        writeField(fields, 5, "u", replySerial.toString())
        if (destination.isNotEmpty()) {
            writeField(fields, 6, "s", destination)
        }
        sendDbusMessage(os, 2, 0, fields.toByteArray(), ByteArray(0))
    }

    private fun sendMethodReturn(
        os: OutputStream,
        destination: String,
        replySerial: Int,
        sig: String,
        body: ByteArray
    ) {
        val fields = ByteArrayOutputStream()
        writeField(fields, 5, "u", replySerial.toString())
        if (destination.isNotEmpty()) {
            writeField(fields, 6, "s", destination)
        }
        writeField(fields, 8, "g", sig)
        sendDbusMessage(os, 2, 0, fields.toByteArray(), body)
    }

    private fun sendHello(os: OutputStream) {
        val fields = ByteArrayOutputStream()
        writeField(fields, 1, "o", "/org/freedesktop/DBus")
        writeField(fields, 6, "s", "org.freedesktop.DBus")
        writeField(fields, 2, "s", "org.freedesktop.DBus")
        writeField(fields, 3, "s", "Hello")
        sendDbusMessage(os, 1, 0, fields.toByteArray(), ByteArray(0))
    }

    private fun sendRequestName(os: OutputStream, name: String) {
        val fields = ByteArrayOutputStream()
        writeField(fields, 1, "o", "/org/freedesktop/DBus")
        writeField(fields, 6, "s", "org.freedesktop.DBus")
        writeField(fields, 2, "s", "org.freedesktop.DBus")
        writeField(fields, 3, "s", "RequestName")
        writeField(fields, 8, "g", "su")

        val body = ByteArrayOutputStream()
        writeDbusString(body, name)
        val flags = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(4)
        body.write(flags.array())

        sendDbusMessage(os, 1, 0, fields.toByteArray(), body.toByteArray())
    }

    private fun sendDbusMessage(
        os: OutputStream,
        type: Int,
        flags: Int,
        fieldsBytes: ByteArray,
        bodyBytes: ByteArray
    ) {
        val serial = synchronized(this) { nextSerial++ }
        val pad = (8 - (fieldsBytes.size % 8)) % 8
        val totalHdrLen = 16 + fieldsBytes.size + pad
        val buf = ByteBuffer.allocate(totalHdrLen + bodyBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.put('l'.code.toByte())
        buf.put(type.toByte())
        buf.put(flags.toByte())
        buf.put(1.toByte())
        buf.putInt(bodyBytes.size)
        buf.putInt(serial)
        buf.putInt(fieldsBytes.size)
        buf.put(fieldsBytes)
        for (i in 0 until pad) buf.put(0.toByte())
        buf.put(bodyBytes)

        synchronized(os) {
            os.write(buf.array())
            os.flush()
        }
    }

    private fun readMessage(`is`: InputStream): ByteArray? {
        val hdr = ByteArray(16)
        val read = `is`.readNBytes(hdr, 0, 16)
        if (read < 16) return null
        val buf = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
        buf.get()
        buf.get()
        buf.get()
        buf.get()
        val bodyLen = buf.getInt()
        buf.getInt()
        val fieldsLen = buf.getInt()

        val fieldsPad = (8 - (fieldsLen % 8)) % 8
        `is`.readNBytes(fieldsLen + fieldsPad)
        val body = ByteArray(bodyLen)
        if (bodyLen > 0) {
            `is`.readNBytes(body, 0, bodyLen)
        }
        return body
    }

    private fun writePropertyEntry(
        out: ByteArrayOutputStream,
        key: String,
        variantSig: String,
        block: (ByteArrayOutputStream) -> Unit
    ) {
        val pad = (8 - (out.size() % 8)) % 8
        for (i in 0 until pad) out.write(0)

        writeDbusString(out, key)

        val sigBytes = variantSig.toByteArray(StandardCharsets.US_ASCII)
        out.write(sigBytes.size)
        out.write(sigBytes)
        out.write(0)

        val align = when (variantSig) {
            "x", "t", "d" -> 8
            "y", "g" -> 1
            else -> 4
        }
        val vPad = (align - (out.size() % align)) % align
        for (i in 0 until vPad) out.write(0)

        block(out)
    }

    private fun writeField(out: ByteArrayOutputStream, code: Int, sig: String, value: String) {
        val pad = (8 - (out.size() % 8)) % 8
        for (i in 0 until pad) out.write(0)

        out.write(code)
        val sigBytes = sig.toByteArray(StandardCharsets.US_ASCII)
        out.write(sigBytes.size)
        out.write(sigBytes)
        out.write(0)

        if (sig == "u") {
            val align = (4 - (out.size() % 4)) % 4
            for (i in 0 until align) out.write(0)
            val v = value.toIntOrNull() ?: 0
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v)
            out.write(bb.array())
        } else if (sig == "g") {
            val vBytes = value.toByteArray(StandardCharsets.US_ASCII)
            out.write(vBytes.size)
            out.write(vBytes)
            out.write(0)
        } else {
            val align = (4 - (out.size() % 4)) % 4
            for (i in 0 until align) out.write(0)
            writeDbusString(out, value)
        }
    }

    private fun writeDbusString(out: OutputStream, str: String) {
        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(bytes.size)
        out.write(lenBuf.array())
        out.write(bytes)
        out.write(0)
    }

    private fun buildIntrospectionXml(): String =
        """<!DOCTYPE node PUBLIC "-//freedesktop//DTD D-BUS Object Introspection 1.0//EN"
"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd">
<node>
  <interface name="org.freedesktop.DBus.Introspectable">
    <method name="Introspect">
      <arg name="xml_data" type="s" direction="out"/>
    </method>
  </interface>
  <interface name="org.freedesktop.DBus.Properties">
    <method name="Get">
      <arg name="interface_name" type="s" direction="in"/>
      <arg name="property_name" type="s" direction="in"/>
      <arg name="value" type="v" direction="out"/>
    </method>
    <method name="GetAll">
      <arg name="interface_name" type="s" direction="in"/>
      <arg name="properties" type="a{sv}" direction="out"/>
    </method>
    <signal name="PropertiesChanged">
      <arg name="interface_name" type="s"/>
      <arg name="changed_properties" type="a{sv}"/>
      <arg name="invalidated_properties" type="as"/>
    </signal>
  </interface>
  <interface name="org.mpris.MediaPlayer2">
    <property name="CanQuit" type="b" access="read"/>
    <property name="CanRaise" type="b" access="read"/>
    <property name="HasTrackList" type="b" access="read"/>
    <property name="Identity" type="s" access="read"/>
    <property name="SupportedUriSchemes" type="as" access="read"/>
    <property name="SupportedMimeTypes" type="as" access="read"/>
    <method name="Raise"/>
    <method name="Quit"/>
  </interface>
  <interface name="org.mpris.MediaPlayer2.Player">
    <property name="PlaybackStatus" type="s" access="read"/>
    <property name="LoopStatus" type="s" access="readwrite"/>
    <property name="Rate" type="d" access="readwrite"/>
    <property name="Shuffle" type="b" access="readwrite"/>
    <property name="Metadata" type="a{sv}" access="read"/>
    <property name="Volume" type="d" access="readwrite"/>
    <property name="Position" type="x" access="read"/>
    <property name="MinimumRate" type="d" access="read"/>
    <property name="MaximumRate" type="d" access="read"/>
    <property name="CanControl" type="b" access="read"/>
    <property name="CanPlay" type="b" access="read"/>
    <property name="CanPause" type="b" access="read"/>
    <property name="CanSeek" type="b" access="read"/>
    <property name="CanGoNext" type="b" access="read"/>
    <property name="CanGoPrevious" type="b" access="read"/>
    <method name="Next"/>
    <method name="Previous"/>
    <method name="Pause"/>
    <method name="PlayPause"/>
    <method name="Stop"/>
    <method name="Play"/>
    <method name="Seek">
      <arg name="Offset" type="x" direction="in"/>
    </method>
    <method name="SetPosition">
      <arg name="TrackId" type="o" direction="in"/>
      <arg name="Position" type="x" direction="in"/>
    </method>
  </interface>
</node>"""
}
