package org.shilpo.peerless.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.PlaybackStateSnapshot
import org.shilpo.peerless.model.RepeatMode
import org.shilpo.peerless.model.Track
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.sync.PlaybackSyncManager
import kotlin.time.Duration.Companion.milliseconds

internal fun remotePlaybackStatus(isPlaying: Boolean): PlaybackStatus =
    if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED

class RealPlayerConnection(
    val apiClient: PeerlessApiClient = PeerlessApiClient(),
    val audioEngine: AudioEngine = createAudioEngine(),
    val storage: QueueStorage = createPlatformQueueStorage(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : PlayerConnection {

    private val _currentTrack = MutableStateFlow<Track?>(null)
    override val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _playbackInfo = MutableStateFlow<PlaybackInfo?>(null)
    override val playbackInfo: StateFlow<PlaybackInfo?> = _playbackInfo.asStateFlow()

    override val signalPath: StateFlow<SignalPathSnapshot?> = audioEngine.signalPath

    private val _status = MutableStateFlow(PlaybackStatus.IDLE)
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val originalQueue = mutableListOf<Track>()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    override val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    override val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    override val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    override val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _canSkipNext = MutableStateFlow(false)
    override val canSkipNext: StateFlow<Boolean> = _canSkipNext.asStateFlow()

    private val _canSkipPrevious = MutableStateFlow(false)
    override val canSkipPrevious: StateFlow<Boolean> = _canSkipPrevious.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    override val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    override val currentPositionMs: Long
        get() = _positionMs.value

    override val currentDurationMs: Long
        get() = _durationMs.value

    override val currentBufferedPositionMs: Long
        get() = _bufferedPositionMs.value

    private var loadJob: Job? = null
    private var preloadJob: Job? = null
    private var saveDebounceJob: Job? = null
    private var tickerJob: Job? = null
    private var syncManager: PlaybackSyncManager? = null
    private var syncSnapshotJob: Job? = null
    private var syncCommandJob: Job? = null
    private var syncActiveDeviceJob: Job? = null
    private var lastSyncReportMs: Long = 0L

    fun attachSync(sync: PlaybackSyncManager) {
        this.syncManager = sync
        syncSnapshotJob?.cancel()
        syncCommandJob?.cancel()
        syncActiveDeviceJob?.cancel()

        // 1. Observe remote snapshots when not the active playback device
        syncSnapshotJob = scope.launch {
            sync.remoteSnapshot.collect { snapshot ->
                if (snapshot != null && !sync.isSelfActiveDevice.value) {
                    val remoteTrack = snapshot.queue.getOrNull(snapshot.currentIndex)
                    _currentTrack.value = remoteTrack
                    _queue.value = snapshot.queue
                    _currentIndex.value = snapshot.currentIndex
                    _positionMs.value = snapshot.positionMs
                    _durationMs.value = remoteTrack?.durationMs ?: 0L
                    _isPlaying.value = snapshot.isPlaying
                    _status.value = remotePlaybackStatus(snapshot.isPlaying)
                    updateSkipFlags()
                    if (audioEngine.state.value.status == PlaybackStatus.PLAYING) {
                        audioEngine.pause()
                    }
                }
            }
        }

        // 2. Observe remote commands directed to this active device
        syncCommandJob = scope.launch {
            sync.incomingCommands.collect { cmd ->
                if (sync.isSelfActiveDevice.value) {
                    when (cmd.action) {
                        "play" -> play()
                        "pause" -> pause()
                        "next" -> skipNext()
                        "prev" -> skipPrevious()
                        "seek" -> {
                            val pos = cmd.data?.jsonPrimitive?.longOrNull ?: 0L
                            seekTo(pos)
                        }

                        "select_track" -> {
                            val snapshot = cmd.data?.let {
                                runCatching { Json.decodeFromJsonElement<PlaybackStateSnapshot>(it) }
                                    .getOrNull()
                            }
                            val track = snapshot?.queue?.getOrNull(snapshot.currentIndex)
                            if (snapshot != null && track != null) {
                                play(track, snapshot.queue)
                                if (snapshot.positionMs > 0L) seekTo(snapshot.positionMs)
                            }
                        }
                    }
                }
            }
        }

        // Explicit device selection is the only operation that moves audio output.
        syncActiveDeviceJob = scope.launch {
            sync.isSelfActiveDevice
                .drop(1)
                .collect { isActive ->
                    if (isActive && _isPlaying.value && audioEngine.state.value.status != PlaybackStatus.PLAYING) {
                        val track = _currentTrack.value ?: return@collect
                        val resumePosition = _positionMs.value
                        startLoadingTrack(track)
                        if (resumePosition > 0L) {
                            audioEngine.state.first {
                                it.status == PlaybackStatus.PAUSED ||
                                        it.status == PlaybackStatus.PLAYING ||
                                        it.status == PlaybackStatus.BUFFERING ||
                                        it.durationMs > 0L
                            }
                            audioEngine.seekTo(resumePosition)
                        }
                    }
                }
        }
    }

    private fun notifySyncState() {
        val sync = syncManager ?: return
        if (sync.isSelfActiveDevice.value) {
            scope.launch {
                sync.reportState(
                    PlaybackStateSnapshot(
                        queue = _queue.value,
                        currentIndex = _currentIndex.value,
                        positionMs = _positionMs.value,
                        shuffleMode = _shuffleMode.value,
                        repeatMode = _repeatMode.value,
                        isPlaying = _isPlaying.value
                    )
                )
            }
        }
    }

    init {
        scope.launch {
            audioEngine.state.collect { engState ->
                // A dormant local engine must not overwrite the active remote device's snapshot.
                if (syncManager?.isSelfActiveDevice?.value == false) return@collect
                _status.value = engState.status
                val playing = (engState.status == PlaybackStatus.PLAYING)
                if (_isPlaying.value != playing) {
                    _isPlaying.value = playing
                    if (playing) {
                        startTicker()
                    } else {
                        stopTicker()
                    }
                    notifySyncState()
                }
                _positionMs.value = engState.positionMs

                val dur = engState.durationMs.takeIf { it > 0L }
                    ?: (_currentTrack.value?.durationMs ?: 0L)
                _durationMs.value = dur

                val isCached = _currentTrack.value?.isCached == true
                val engBuffered = engState.bufferedPositionMs
                if (isCached && dur > 0L) {
                    _bufferedPositionMs.value = dur
                } else if (engBuffered > 0L) {
                    _bufferedPositionMs.value = maxOf(engBuffered, engState.positionMs)
                } else if (engState.positionMs > 0L) {
                    _bufferedPositionMs.value = maxOf(_bufferedPositionMs.value, engState.positionMs)
                }

                if (engState.status == PlaybackStatus.COMPLETED) {
                    handlePlaybackCompleted()
                }
                updateSkipFlags()
            }
        }

        scope.launch {
            audioEngine.events.collect { event ->
                when (event) {
                    is AudioEngineEvent.TransitionedToNext -> {
                        handleEngineTransitionedToNext()
                    }

                    else -> {}
                }
            }
        }

        scope.launch {
            hydrateSavedState()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val engState = audioEngine.state.value
                val pos = engState.positionMs
                if (pos >= 0L) {
                    _positionMs.value = pos
                }
                val dur = engState.durationMs.takeIf { it > 0L }
                    ?: (_currentTrack.value?.durationMs ?: 0L)
                if (dur > 0L) {
                    _durationMs.value = dur
                }
                val buf = engState.bufferedPositionMs
                val isCached = _currentTrack.value?.isCached == true
                if (isCached && _durationMs.value > 0L) {
                    _bufferedPositionMs.value = _durationMs.value
                } else if (buf > 0L) {
                    _bufferedPositionMs.value = maxOf(buf, pos)
                } else if (pos > 0L) {
                    _bufferedPositionMs.value = maxOf(_bufferedPositionMs.value, pos)
                }
                if (kotlin.math.abs(pos - lastSyncReportMs) >= 2000L) {
                    lastSyncReportMs = pos
                    notifySyncState()
                }
                delay(50.milliseconds)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        val pos = audioEngine.state.value.positionMs
        if (pos >= 0L) {
            _positionMs.value = pos
        }
        lastSyncReportMs = _positionMs.value
        notifySyncState()
    }

    private fun handleEngineTransitionedToNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIdx = getNextIndex()
        if (nextIdx != null) {
            _currentIndex.value = nextIdx
            val track = q[nextIdx]
            _currentTrack.value = track
            _positionMs.value = 0L
            _durationMs.value = track.durationMs
            updateSkipFlags()
            preloadNextTrack()
            scheduleSave()
            notifySyncState()
        }
    }

    private suspend fun hydrateSavedState() {
        val snapshot = storage.loadState() ?: return
        if (snapshot.queue.isEmpty()) return

        originalQueue.clear()
        originalQueue.addAll(snapshot.queue)

        _shuffleMode.value = snapshot.shuffleMode
        _repeatMode.value = snapshot.repeatMode

        if (snapshot.shuffleMode) {
            val shuffled = snapshot.queue.shuffled()
            _queue.value = shuffled
            _currentIndex.value = snapshot.currentIndex.coerceIn(0, (shuffled.size - 1).coerceAtLeast(0))
        } else {
            _queue.value = snapshot.queue
            _currentIndex.value = snapshot.currentIndex.coerceIn(0, (snapshot.queue.size - 1).coerceAtLeast(0))
        }

        val track = _queue.value.getOrNull(_currentIndex.value)
        _currentTrack.value = track
        updateSkipFlags()

        if (track != null) {
            _durationMs.value = track.durationMs
            if (snapshot.positionMs > 0L) {
                _positionMs.value = snapshot.positionMs
                val streamUrl = resolveStreamUrl(track)
                audioEngine.prepare(
                    streamUrl,
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = apiClient.getArtworkUrl(track.toSummaryDto(), size = 600)
                )
                scope.launch {
                    audioEngine.state.first {
                        it.status == PlaybackStatus.PAUSED ||
                                it.status == PlaybackStatus.PLAYING ||
                                it.status == PlaybackStatus.BUFFERING ||
                                it.durationMs > 0L
                    }
                    audioEngine.seekTo(snapshot.positionMs)
                    _positionMs.value = snapshot.positionMs
                }
            }
        }
    }

    private fun handlePlaybackCompleted() {
        if (_repeatMode.value == RepeatMode.ONE) {
            audioEngine.seekTo(0L)
            audioEngine.play()
        } else {
            playNextInternal(autoTriggered = true)
        }
    }

    override fun play(track: Track, queue: List<Track>) {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            val effectiveQueue = queue.ifEmpty { listOf(track) }
            val selectedIndex = effectiveQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            val snapshot = PlaybackStateSnapshot(
                queue = effectiveQueue,
                currentIndex = selectedIndex,
                positionMs = 0L,
                shuffleMode = _shuffleMode.value,
                repeatMode = _repeatMode.value,
                isPlaying = true
            )
            scope.launch {
                syncManager?.sendCommand(
                    "select_track",
                    Json.encodeToJsonElement(snapshot)
                )
            }
            return
        }
        if (_currentTrack.value?.id == track.id && (_status.value == PlaybackStatus.PLAYING || _status.value == PlaybackStatus.PAUSED)) {
            togglePlayPause()
            return
        }
        if (queue.isNotEmpty()) {
            originalQueue.clear()
            originalQueue.addAll(queue)
            if (_shuffleMode.value) {
                val others = queue.filter { it.id != track.id }.shuffled()
                _queue.value = listOf(track) + others
                _currentIndex.value = 0
            } else {
                _queue.value = queue
                val idx = queue.indexOfFirst { it.id == track.id }
                _currentIndex.value = if (idx >= 0) idx else 0
            }
        } else {
            if (originalQueue.none { it.id == track.id }) {
                originalQueue.add(track)
            }
            if (_queue.value.none { it.id == track.id }) {
                _queue.update { it + track }
            }
            val idx = _queue.value.indexOfFirst { it.id == track.id }
            _currentIndex.value = if (idx >= 0) idx else 0
        }

        _currentTrack.value = track
        updateSkipFlags()
        startLoadingTrack(track)
        scheduleSave()
    }

    private fun startLoadingTrack(track: Track) {
        _status.value = PlaybackStatus.BUFFERING
        _positionMs.value = 0L
        _durationMs.value = track.durationMs
        _bufferedPositionMs.value = if (track.isCached && track.durationMs > 0L) track.durationMs else 0L
        notifySyncState()
        loadJob?.cancel()
        preloadJob?.cancel()

        loadJob = scope.launch {
            val streamUrl = resolveStreamUrl(track)

            launch {
                val infoResult = apiClient.getPlaybackInfo(track.id)
                infoResult.onSuccess { info ->
                    _playbackInfo.value = info
                }
            }

            audioEngine.prepare(
                streamUrl,
                title = track.title,
                artist = track.artist,
                artworkUrl = apiClient.getArtworkUrl(track.toSummaryDto(), size = 600)
            )
            audioEngine.play()

            preloadNextTrack()
        }
    }

    private fun preloadNextTrack() {
        val nextIdx = getNextIndex()
        if (nextIdx != null) {
            val nextTrack = _queue.value.getOrNull(nextIdx) ?: return
            preloadJob = scope.launch {
                val nextUrl = resolveStreamUrl(nextTrack)
                audioEngine.prepareNext(nextUrl)
            }
        } else {
            audioEngine.prepareNext(null)
        }
    }

    private suspend fun resolveStreamUrl(track: Track): String {
        val ticketResult = apiClient.getPlaybackInfo(track.id)
        val playbackInfo = ticketResult.getOrNull() ?: return ""
        return if (playbackInfo.stream_url.startsWith("http://") || playbackInfo.stream_url.startsWith("https://")) {
            playbackInfo.stream_url
        } else {
            "${apiClient.baseUrl}${playbackInfo.stream_url}"
        }
    }

    override fun play() {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("play") }
            return
        }
        val currentStatus = _status.value
        if (currentStatus == PlaybackStatus.PAUSED || currentStatus == PlaybackStatus.IDLE) {
            if (_currentTrack.value != null) {
                if (audioEngine.state.value.status == PlaybackStatus.IDLE) {
                    val track = _currentTrack.value!!
                    val resumePos = _positionMs.value
                    startLoadingTrack(track)
                    if (resumePos > 0L) {
                        scope.launch {
                            audioEngine.state.first {
                                it.status == PlaybackStatus.PAUSED ||
                                        it.status == PlaybackStatus.PLAYING ||
                                        it.status == PlaybackStatus.BUFFERING ||
                                        it.durationMs > 0L
                            }
                            audioEngine.seekTo(resumePos)
                            _positionMs.value = resumePos
                            notifySyncState()
                        }
                    }
                } else {
                    audioEngine.play()
                }
            } else if (_queue.value.isNotEmpty()) {
                val first = _queue.value.first()
                play(first, _queue.value)
            }
        }
    }

    override fun pause() {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("pause") }
            return
        }
        if (_status.value == PlaybackStatus.PLAYING || _status.value == PlaybackStatus.BUFFERING) {
            audioEngine.pause()
        }
    }

    override fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    override fun playNext() {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("next") }
            return
        }
        playNextInternal(autoTriggered = false)
    }

    override fun skipNext() {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("next") }
            return
        }
        playNext()
    }

    private fun playNextInternal(autoTriggered: Boolean) {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIdx = getNextIndex()
        if (nextIdx != null) {
            _currentIndex.value = nextIdx
            val track = q[nextIdx]
            _currentTrack.value = track
            updateSkipFlags()
            startLoadingTrack(track)
            scheduleSave()
        } else {
            if (autoTriggered) {
                _status.value = PlaybackStatus.IDLE
                val wasPlaying = _isPlaying.value
                _isPlaying.value = false
                if (wasPlaying) notifySyncState()
            }
        }
    }

    private fun getNextIndex(): Int? {
        val q = _queue.value
        if (q.isEmpty()) return null
        val cur = _currentIndex.value
        return when {
            cur < q.size - 1 -> cur + 1
            _repeatMode.value == RepeatMode.ALL -> 0
            else -> null
        }
    }

    override fun playPrevious() {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("prev") }
            return
        }
        val q = _queue.value
        if (q.isEmpty()) return

        if (currentPositionMs > 3000L) {
            audioEngine.seekTo(0L)
            _positionMs.value = 0L
            notifySyncState()
            return
        }

        val cur = _currentIndex.value
        val prevIdx = when {
            cur > 0 -> cur - 1
            _repeatMode.value == RepeatMode.ALL -> q.size - 1
            else -> null
        }

        if (prevIdx != null) {
            _currentIndex.value = prevIdx
            val track = q[prevIdx]
            _currentTrack.value = track
            updateSkipFlags()
            startLoadingTrack(track)
            scheduleSave()
        } else {
            audioEngine.seekTo(0L)
            _positionMs.value = 0L
            notifySyncState()
        }
    }

    override fun skipPrevious() {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("prev") }
            return
        }
        playPrevious()
    }

    override fun seekTo(positionMs: Long) {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("seek", JsonPrimitive(positionMs)) }
            _positionMs.value = positionMs
            return
        }
        _positionMs.value = positionMs
        audioEngine.seekTo(positionMs)
        scheduleSave()
        notifySyncState()
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _volume.value = clamped
        audioEngine.setVolume(clamped)
    }

    override fun setShuffleMode(enabled: Boolean) {
        if (_shuffleMode.value == enabled) return
        _shuffleMode.value = enabled

        val current = _currentTrack.value
        if (enabled) {
            val list = originalQueue.toMutableList()
            if (current != null) {
                list.remove(current)
                list.shuffle()
                list.add(0, current)
                _queue.value = list
                _currentIndex.value = 0
            } else {
                list.shuffle()
                _queue.value = list
                _currentIndex.value = 0
            }
        } else {
            _queue.value = originalQueue.toList()
            val idx = if (current != null) originalQueue.indexOfFirst { it.id == current.id } else 0
            _currentIndex.value = if (idx >= 0) idx else 0
        }
        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
    }

    override fun toggleShuffle() {
        setShuffleMode(!_shuffleMode.value)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
    }

    override fun cycleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    override fun addToQueue(track: Track) {
        originalQueue.add(track)
        _queue.update { it + track }
        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
    }

    override fun playNextInQueue(track: Track) {
        val insertIdx = (_currentIndex.value + 1).coerceIn(0, _queue.value.size)
        originalQueue.add(insertIdx.coerceIn(0, originalQueue.size), track)

        val updated = _queue.value.toMutableList()
        updated.add(insertIdx, track)
        _queue.value = updated

        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
    }

    override fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val q = _queue.value.toMutableList()
        if (fromIndex !in q.indices || toIndex !in q.indices) return

        val moved = q.removeAt(fromIndex)
        q.add(toIndex, moved)
        _queue.value = q

        val cur = _currentIndex.value
        if (cur == fromIndex) {
            _currentIndex.value = toIndex
        } else if (fromIndex < cur && toIndex >= cur) {
            _currentIndex.value = cur - 1
        } else if (fromIndex > cur && toIndex <= cur) {
            _currentIndex.value = cur + 1
        }

        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
    }

    override fun removeAt(index: Int) {
        val q = _queue.value.toMutableList()
        if (index !in q.indices) return

        val removed = q.removeAt(index)
        originalQueue.removeAll { it.id == removed.id }
        _queue.value = q

        val cur = _currentIndex.value
        if (q.isEmpty()) {
            stopAndDismiss()
        } else if (index == cur) {
            val newIdx = cur.coerceIn(0, q.size - 1)
            _currentIndex.value = newIdx
            val nextTrack = q[newIdx]
            _currentTrack.value = nextTrack
            startLoadingTrack(nextTrack)
        } else if (index < cur) {
            _currentIndex.value = cur - 1
        }

        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
    }

    override fun clearQueue() {
        originalQueue.clear()
        _queue.value = emptyList()
        _currentIndex.value = -1
        stopAndDismiss()
    }

    override fun stopAndDismiss() {
        stopTicker()
        loadJob?.cancel()
        preloadJob?.cancel()
        audioEngine.stop()
        _currentTrack.value = null
        _playbackInfo.value = null
        _status.value = PlaybackStatus.IDLE
        _isPlaying.value = false
        _positionMs.value = 0L
        _durationMs.value = 0L
        updateSkipFlags()
        notifySyncState()
        scope.launch {
            storage.clearState()
        }
    }

    private fun updateSkipFlags() {
        val q = _queue.value
        val cur = _currentIndex.value
        val rep = _repeatMode.value

        _canSkipNext.value = when {
            q.isEmpty() -> false
            rep == RepeatMode.ALL -> true
            cur < q.size - 1 -> true
            else -> false
        }

        _canSkipPrevious.value = when {
            q.isEmpty() -> false
            rep == RepeatMode.ALL -> true
            cur > 0 -> true
            currentPositionMs > 3000L -> true
            else -> false
        }
    }

    private fun scheduleSave() {
        saveDebounceJob?.cancel()
        saveDebounceJob = scope.launch {
            delay(500)
            val track = _currentTrack.value ?: return@launch
            val snapshot = PlaybackStateSnapshot(
                queue = _queue.value,
                currentIndex = _currentIndex.value,
                positionMs = currentPositionMs,
                shuffleMode = _shuffleMode.value,
                repeatMode = _repeatMode.value,
                isPlaying = _isPlaying.value
            )
            storage.saveState(snapshot)
            notifySyncState()
        }
    }
}
