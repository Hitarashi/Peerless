package org.shilpo.peerless.sync

import androidx.compose.runtime.staticCompositionLocalOf
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.shilpo.peerless.auth.TokenStorage
import org.shilpo.peerless.getDeviceDisplayName
import org.shilpo.peerless.getPlatform
import org.shilpo.peerless.model.PlaybackStateSnapshot
import org.shilpo.peerless.network.createDefaultPeerlessHttpClient

@Serializable
data class ConnectedDevice(
    val device_id: String,
    val device_name: String,
    val platform: String
)

@Serializable
sealed class ClientSyncMessage {
    @Serializable
    @SerialName("hello")
    data class Hello(val payload: HelloPayload) : ClientSyncMessage()

    @Serializable
    @SerialName("report_state")
    data class ReportState(val payload: ReportStatePayload) : ClientSyncMessage()

    @Serializable
    @SerialName("command")
    data class Command(val payload: CommandPayload) : ClientSyncMessage()

    @Serializable
    @SerialName("transfer_playback")
    data class TransferPlayback(val payload: TransferPlaybackPayload) : ClientSyncMessage()
}

@Serializable
data class HelloPayload(val device_id: String, val device_name: String, val platform: String)

@Serializable
data class ReportStatePayload(val snapshot: JsonElement)

@Serializable
data class CommandPayload(val action: String, val data: JsonElement? = null)

@Serializable
data class TransferPlaybackPayload(val target_device_id: String)

/**
 * Serializes through the sealed base type so kotlinx.serialization includes the
 * `type` discriminator required by the server's adjacently tagged enum.
 */
internal fun Json.encodeClientSyncMessage(message: ClientSyncMessage): String =
    encodeToString<ClientSyncMessage>(message)

@Serializable
sealed class ServerSyncMessage {
    @Serializable
    @SerialName("room_state")
    data class RoomState(val payload: RoomStatePayload) : ServerSyncMessage()

    @Serializable
    @SerialName("state_updated")
    data class StateUpdated(val payload: StateUpdatedPayload) : ServerSyncMessage()

    @Serializable
    @SerialName("execute_command")
    data class ExecuteCommand(val payload: ExecuteCommandPayload) : ServerSyncMessage()
}

@Serializable
data class RoomStatePayload(
    val active_device_id: String? = null,
    val devices: List<ConnectedDevice> = emptyList(),
    val snapshot: JsonElement? = null
)

@Serializable
data class StateUpdatedPayload(
    val active_device_id: String? = null,
    val snapshot: JsonElement
)

@Serializable
data class ExecuteCommandPayload(
    val action: String,
    val data: JsonElement? = null
)

/**
 * High-leverage client for Spotify Connect-style real-time cross-device playback sync.
 * Maintains full-duplex WebSocket connection to peerless-server.
 */
class PlaybackSyncManager(
    val tokenStorage: TokenStorage,
    private val httpClient: HttpClient = createDefaultPeerlessHttpClient().config { install(WebSockets) },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    val selfDeviceId: String = tokenStorage.getOrCreateDeviceId()
    val selfDeviceName: String = getDeviceDisplayName()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _activeDeviceId = MutableStateFlow<String?>(null)
    val activeDeviceId: StateFlow<String?> = _activeDeviceId.asStateFlow()

    val isSelfActiveDevice: StateFlow<Boolean> = _activeDeviceId.map { it == null || it == selfDeviceId }
        .stateIn(scope, SharingStarted.Eagerly, true)

    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    private val _remoteSnapshot = MutableStateFlow<PlaybackStateSnapshot?>(null)
    val remoteSnapshot: StateFlow<PlaybackStateSnapshot?> = _remoteSnapshot.asStateFlow()

    private val _commandFlow = MutableSharedFlow<ExecuteCommandPayload>(extraBufferCapacity = 16)
    val incomingCommands: SharedFlow<ExecuteCommandPayload> = _commandFlow.asSharedFlow()

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    fun start(serverUrl: String, token: String) {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            closeCurrentSession()
            while (isActive) {
                try {
                    connectSocket(serverUrl, token)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    _isConnected.value = false
                    delay(3000) // reconnect backoff
                }
            }
        }
    }

    fun stop() {
        connectionJob?.cancel()
        connectionJob = null
        scope.launch {
            closeCurrentSession()
        }
    }

    private suspend fun closeCurrentSession() {
        val socket = session
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        if (session === socket) session = null
        _isConnected.value = false
    }

    private suspend fun connectSocket(serverUrl: String, token: String) {
        val wsUrl = serverUrl.replace("https://", "wss://").replace("http://", "ws://").trimEnd('/')
        val fullUrl = "$wsUrl/api/v1/ws/playback?token=$token"

        httpClient.webSocket(urlString = fullUrl) {
            val socket = this
            session = socket
            _isConnected.value = true

            try {
                // Send Hello
                val hello = ClientSyncMessage.Hello(
                    HelloPayload(
                        device_id = selfDeviceId,
                        device_name = selfDeviceName,
                        platform = getPlatform().name
                    )
                )
                send(Frame.Text(json.encodeClientSyncMessage(hello)))

                // Listen loop
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        handleIncomingFrame(text)
                    }
                }
            } finally {
                if (session === socket) {
                    session = null
                    _isConnected.value = false
                }
            }
        }
    }

    private fun handleIncomingFrame(text: String) {
        runCatching {
            val element = json.parseToJsonElement(text).jsonObject
            val type = element["type"]?.jsonPrimitive?.content ?: return
            val payload = element["payload"] ?: return

            when (type) {
                "room_state" -> {
                    val roomState = json.decodeFromJsonElement<RoomStatePayload>(payload)
                    _activeDeviceId.value = roomState.active_device_id
                    _connectedDevices.value = roomState.devices
                    roomState.snapshot?.let { snapElem ->
                        val snap =
                            runCatching { json.decodeFromJsonElement<PlaybackStateSnapshot>(snapElem) }.getOrNull()
                        if (snap != null) _remoteSnapshot.value = snap
                    }
                }

                "state_updated" -> {
                    val update = json.decodeFromJsonElement<StateUpdatedPayload>(payload)
                    _activeDeviceId.value = update.active_device_id
                    val snap =
                        runCatching { json.decodeFromJsonElement<PlaybackStateSnapshot>(update.snapshot) }.getOrNull()
                    if (snap != null) _remoteSnapshot.value = snap
                }

                "execute_command" -> {
                    val cmd = json.decodeFromJsonElement<ExecuteCommandPayload>(payload)
                    _commandFlow.tryEmit(cmd)
                }
            }
        }
    }

    suspend fun reportState(snapshot: PlaybackStateSnapshot) {
        val sess = session ?: return
        if (!_isConnected.value) return
        val snapElem = json.encodeToJsonElement(snapshot)
        val msg = ClientSyncMessage.ReportState(ReportStatePayload(snapElem))
        runCatching { sess.send(Frame.Text(json.encodeClientSyncMessage(msg))) }
    }

    suspend fun sendCommand(action: String, data: JsonElement? = null) {
        val sess = session ?: return
        if (!_isConnected.value) return
        val msg = ClientSyncMessage.Command(CommandPayload(action = action, data = data))
        runCatching { sess.send(Frame.Text(json.encodeClientSyncMessage(msg))) }
    }

    suspend fun transferPlayback(targetDeviceId: String = selfDeviceId) {
        val sess = session ?: return
        if (!_isConnected.value) return
        val msg = ClientSyncMessage.TransferPlayback(TransferPlaybackPayload(targetDeviceId))
        runCatching { sess.send(Frame.Text(json.encodeClientSyncMessage(msg))) }
    }

}

val LocalPlaybackSyncManager = staticCompositionLocalOf<PlaybackSyncManager> {
    error("No PlaybackSyncManager provided")
}
