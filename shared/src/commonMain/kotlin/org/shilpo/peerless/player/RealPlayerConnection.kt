package org.shilpo.peerless.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.PlaybackStateSnapshot
import org.shilpo.peerless.model.RepeatMode
import org.shilpo.peerless.model.Track
import org.shilpo.peerless.network.PeerlessApiClient

class RealPlayerConnection(
    val apiClient: PeerlessApiClient = PeerlessApiClient(),
    val audioEngine: AudioEngine = createAudioEngine(),
    val storage: QueueStorage = createPlatformQueueStorage(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    var isDevMode: Boolean = true
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

    override val currentPositionMs: Long
        get() = audioEngine.state.value.positionMs

    override val durationMs: Long
        get() = audioEngine.state.value.durationMs.takeIf { it > 0L }
            ?: (_currentTrack.value?.durationMs ?: 0L)

    private var loadJob: Job? = null
    private var preloadJob: Job? = null
    private var saveDebounceJob: Job? = null

    init {
        scope.launch {
            audioEngine.state.collect { engState ->
                _status.value = engState.status
                _isPlaying.value = (engState.status == PlaybackStatus.PLAYING)

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

    private fun handleEngineTransitionedToNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIdx = getNextIndex()
        if (nextIdx != null) {
            _currentIndex.value = nextIdx
            val track = q[nextIdx]
            _currentTrack.value = track
            updateSkipFlags()
            preloadNextTrack()
            scheduleSave()
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

        if (track != null && snapshot.positionMs > 0L) {
            val streamUrl = resolveStreamUrl(track)
            audioEngine.prepare(streamUrl)
            audioEngine.seekTo(snapshot.positionMs)
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

            audioEngine.prepare(streamUrl)
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
        return if (isDevMode) {
            apiClient.getStreamUrl(track.id)
        } else {
            val ticketResult = apiClient.getPlaybackInfo(track.id)
            ticketResult.getOrNull()?.let { playbackInfo ->
                if (playbackInfo.stream_url.startsWith("http://") || playbackInfo.stream_url.startsWith("https://")) {
                    playbackInfo.stream_url
                } else {
                    "${apiClient.baseUrl}${playbackInfo.stream_url}"
                }
            } ?: apiClient.getStreamUrl(track.id)
        }
    }

    override fun togglePlayPause() {
        val currentStatus = _status.value
        if (currentStatus == PlaybackStatus.PLAYING) {
            audioEngine.pause()
        } else if (currentStatus == PlaybackStatus.PAUSED || currentStatus == PlaybackStatus.IDLE) {
            if (_currentTrack.value != null) {
                audioEngine.play()
            } else if (_queue.value.isNotEmpty()) {
                val first = _queue.value.first()
                play(first, _queue.value)
            }
        }
    }

    override fun playNext() {
        playNextInternal(autoTriggered = false)
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
                _isPlaying.value = false
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
        val q = _queue.value
        if (q.isEmpty()) return

        if (currentPositionMs > 3000L) {
            audioEngine.seekTo(0L)
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
        }
    }

    override fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        scheduleSave()
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

    override fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
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
        loadJob?.cancel()
        preloadJob?.cancel()
        audioEngine.stop()
        _currentTrack.value = null
        _playbackInfo.value = null
        _status.value = PlaybackStatus.IDLE
        _isPlaying.value = false
        updateSkipFlags()
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
                repeatMode = _repeatMode.value
            )
            storage.saveState(snapshot)
        }
    }
}
