package org.shilpo.peerless.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.PlaybackStateSnapshot
import org.shilpo.peerless.model.QueueEntry
import org.shilpo.peerless.model.QueueEntrySource
import org.shilpo.peerless.model.RadioSessionSnapshot
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
    override val spectrum: StateFlow<AudioSpectrumFrame?> = audioEngine.spectrum

    private val _status = MutableStateFlow(PlaybackStatus.IDLE)
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var originalQueue: List<QueueEntry> = emptyList()

    private val _queueEntries = MutableStateFlow<List<QueueEntry>>(emptyList())
    private var nextQueueEntryId = 1L
    private var radioSession: RadioSessionSnapshot? = null
    private var nextRadioSessionId = 1L
    private var radioRecommendationProvider: TrackRecommendationProvider? = null
    private var radioRefillJob: Job? = null
    private val radioRefillMutex = kotlinx.coroutines.sync.Mutex()

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

    private val _outputLatencyMs = MutableStateFlow(0L)
    override val outputLatencyMs: StateFlow<Long> = _outputLatencyMs.asStateFlow()

    override val currentPositionMs: Long
        get() = _positionMs.value

    override val currentDurationMs: Long
        get() = _durationMs.value

    override val currentBufferedPositionMs: Long
        get() = _bufferedPositionMs.value

    private fun newQueueEntries(
        tracks: List<Track>,
        source: QueueEntrySource = QueueEntrySource.CONTEXT
    ): List<QueueEntry> = tracks.map { track ->
        QueueEntry(entryId = nextQueueEntryId++, track = track, source = source)
    }

    private fun setVisibleQueue(entries: List<QueueEntry>) {
        _queueEntries.value = entries
        _queue.value = entries.map { it.track }
    }

    private fun currentQueueEntry(): QueueEntry? =
        _queueEntries.value.getOrNull(_currentIndex.value)

    private fun playbackSnapshot(): PlaybackStateSnapshot = PlaybackStateSnapshot(
        queue = _queue.value,
        currentIndex = _currentIndex.value,
        positionMs = _positionMs.value,
        shuffleMode = _shuffleMode.value,
        repeatMode = _repeatMode.value,
        isPlaying = _isPlaying.value,
        queueEntries = _queueEntries.value,
        originalQueueEntries = originalQueue,
        radioSession = radioSession
    )

    private fun restoreQueueState(snapshot: PlaybackStateSnapshot) {
        val visibleEntries = snapshot.queueEntries
            .takeIf { it.size == snapshot.queue.size }
            ?: newQueueEntries(snapshot.queue)
        val naturalEntries = snapshot.originalQueueEntries
            .takeIf { it.isNotEmpty() }
            ?: visibleEntries
        originalQueue = naturalEntries
        setVisibleQueue(visibleEntries)
        nextQueueEntryId = (visibleEntries + naturalEntries).maxOfOrNull { it.entryId }
            ?.plus(1L) ?: nextQueueEntryId
        radioSession = snapshot.radioSession
        nextRadioSessionId = maxOf(nextRadioSessionId, (radioSession?.sessionId ?: 0L) + 1L)
    }

    override fun configureRadioRecommendations(provider: TrackRecommendationProvider?) {
        radioRecommendationProvider = provider
        if (provider != null) scheduleRadioRefill()
    }

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
                    restoreQueueState(snapshot)
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

                        "play_context" -> {
                            val snapshot = cmd.data?.let {
                                runCatching { Json.decodeFromJsonElement<PlaybackStateSnapshot>(it) }
                                    .getOrNull()
                            }
                            val track = snapshot?.queue?.getOrNull(snapshot.currentIndex)
                            if (snapshot != null && track != null) {
                                playFromContext(track, snapshot.queue)
                            }
                        }

                        "select_queue_item" -> {
                            val index = cmd.data?.jsonPrimitive?.intOrNull ?: return@collect
                            playQueueItem(index)
                        }

                        "start_radio" -> {
                            val track = cmd.data?.let {
                                runCatching { Json.decodeFromJsonElement<Track>(it) }.getOrNull()
                            } ?: return@collect
                            startRadio(track)
                        }

                        "queue_add", "queue_play_next" -> {
                            val track = cmd.data?.let {
                                runCatching { Json.decodeFromJsonElement<Track>(it) }.getOrNull()
                            } ?: return@collect
                            if (cmd.action == "queue_add") addToQueue(track) else playNextInQueue(
                                track
                            )
                        }

                        "queue_move" -> {
                            val data = cmd.data as? JsonObject ?: return@collect
                            val from = data["from"]?.jsonPrimitive?.intOrNull ?: return@collect
                            val to = data["to"]?.jsonPrimitive?.intOrNull ?: return@collect
                            moveInQueue(from, to)
                        }

                        "queue_remove" -> {
                            val index = cmd.data?.jsonPrimitive?.intOrNull ?: return@collect
                            removeAt(index)
                        }

                        "queue_clear" -> clearQueue()
                    }
                }
            }
        }

        // Explicit device selection is the only operation that moves audio output.
        syncActiveDeviceJob = scope.launch {
            sync.isSelfActiveDevice
                .drop(1)
                .collect { isActive ->
                    if (!isActive) radioRefillJob?.cancel()
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
                    if (isActive) scheduleRadioRefill()
                }
        }
    }

    private fun notifySyncState() {
        val sync = syncManager ?: return
        if (sync.isSelfActiveDevice.value) {
            scope.launch {
                sync.reportState(playbackSnapshot())
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
                _outputLatencyMs.value = engState.outputLatencyMs

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
                    _bufferedPositionMs.value =
                        maxOf(_bufferedPositionMs.value, engState.positionMs)
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
            val entry = _queueEntries.value[nextIdx]
            val track = entry.track
            _currentTrack.value = track
            recordRadioSeed(entry)
            _positionMs.value = 0L
            _durationMs.value = track.durationMs
            updateSkipFlags()
            preloadNextTrack()
            scheduleSave()
            notifySyncState()
            scheduleRadioRefill()
        }
    }

    private suspend fun hydrateSavedState() {
        val snapshot = storage.loadState() ?: return
        if (snapshot.queue.isEmpty()) return

        restoreQueueState(snapshot)

        _shuffleMode.value = snapshot.shuffleMode
        _repeatMode.value = snapshot.repeatMode

        _currentIndex.value =
            snapshot.currentIndex.coerceIn(0, (_queue.value.size - 1).coerceAtLeast(0))

        val track = _queue.value.getOrNull(_currentIndex.value)
        _currentTrack.value = track
        updateSkipFlags()

        if (track != null) {
            _durationMs.value = track.durationMs
            if (snapshot.positionMs > 0L) {
                _positionMs.value = snapshot.positionMs
                val streamUrl = resolveStreamUrl(track)
                if (streamUrl != null) {
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
        scheduleRadioRefill()
    }

    private fun handlePlaybackCompleted() {
        if (_repeatMode.value == RepeatMode.ONE) {
            audioEngine.seekTo(0L)
            audioEngine.play()
        } else {
            playNextInternal(autoTriggered = true)
        }
    }

    override fun play(track: Track, queue: List<Track>) = replaceQueueContext(
        track = track,
        queue = queue,
        toggleIfCurrent = true
    )

    override fun playFromContext(track: Track, queue: List<Track>) = replaceQueueContext(
        track = track,
        queue = queue,
        toggleIfCurrent = false
    )

    private fun replaceQueueContext(track: Track, queue: List<Track>, toggleIfCurrent: Boolean) {
        val effectiveQueue = queue.ifEmpty { listOf(track) }
        val selectedIndex = effectiveQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val entries = effectiveQueue.map { queuedTrack ->
            QueueEntry(
                entryId = nextQueueEntryId++,
                track = queuedTrack,
                source = QueueEntrySource.CONTEXT
            )
        }
        if (syncManager?.isSelfActiveDevice?.value == false) {
            val snapshot = PlaybackStateSnapshot(
                queue = effectiveQueue,
                currentIndex = selectedIndex,
                positionMs = 0L,
                shuffleMode = _shuffleMode.value,
                repeatMode = _repeatMode.value,
                isPlaying = true,
                queueEntries = entries,
                originalQueueEntries = entries
            )
            scope.launch {
                syncManager?.sendCommand(
                    if (toggleIfCurrent) "select_track" else "play_context",
                    Json.encodeToJsonElement(snapshot)
                )
            }
            return
        }
        val isCurrentTrack = _currentTrack.value?.id == track.id
        if (toggleIfCurrent && isCurrentTrack &&
            (_status.value == PlaybackStatus.PLAYING || _status.value == PlaybackStatus.PAUSED)
        ) {
            togglePlayPause()
            return
        }
        val keepCurrentPlayback =
            !toggleIfCurrent && isCurrentTrack && _status.value == PlaybackStatus.PLAYING

        radioRefillJob?.cancel()
        radioSession = null
        originalQueue = entries
        if (_shuffleMode.value) {
            val selected = entries[selectedIndex]
            setVisibleQueue(listOf(selected) + entries.filterIndexed { index, _ -> index != selectedIndex }
                .shuffled())
            _currentIndex.value = 0
        } else {
            setVisibleQueue(entries)
            _currentIndex.value = selectedIndex
        }

        _currentTrack.value = track
        updateSkipFlags()
        if (keepCurrentPlayback) {
            notifySyncState()
        } else {
            startLoadingTrack(track)
        }
        scheduleSave()
    }

    override fun playQueueItem(index: Int) {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("select_queue_item", JsonPrimitive(index)) }
            return
        }
        if (index !in _queueEntries.value.indices) return
        if (index == _currentIndex.value) {
            togglePlayPause()
            return
        }
        val entry = _queueEntries.value[index]
        _currentIndex.value = index
        _currentTrack.value = entry.track
        recordRadioSeed(entry)
        updateSkipFlags()
        startLoadingTrack(entry.track)
        scheduleSave()
        scheduleRadioRefill()
    }

    override fun startRadio(track: Track) {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch {
                syncManager?.sendCommand("start_radio", Json.encodeToJsonElement(track))
            }
            return
        }

        val keepCurrentPlayback =
            _currentTrack.value?.id == track.id && _status.value == PlaybackStatus.PLAYING
        radioRefillJob?.cancel()
        val seedEntry = QueueEntry(
            entryId = nextQueueEntryId++,
            track = track,
            source = QueueEntrySource.RADIO_SEED
        )
        originalQueue = listOf(seedEntry)
        setVisibleQueue(originalQueue)
        _currentIndex.value = 0
        radioSession = RadioSessionSnapshot(
            sessionId = nextRadioSessionId++,
            seed = track,
            seenTrackKeys = listOf(recommendationTrackKey(track))
        )
        _currentTrack.value = track
        updateSkipFlags()
        if (keepCurrentPlayback) {
            notifySyncState()
        } else {
            startLoadingTrack(track)
        }
        scheduleSave()
        scheduleRadioRefill()
    }

    private fun recordRadioSeed(entry: QueueEntry) {
        if (entry.source != QueueEntrySource.RADIO_GENERATED) return
        val session = radioSession ?: return
        radioSession = session.copy(
            seedHistory = (session.seedHistory.filterNot { it.id == entry.track.id } + entry.track)
                .takeLast(RADIO_SEED_HISTORY_LIMIT)
        )
    }

    private fun scheduleRadioRefill() {
        if (syncManager?.isSelfActiveDevice?.value == false) return
        if (radioSession == null || radioRecommendationProvider == null) return
        if (remainingRadioTracks() >= RADIO_LOW_WATER_MARK) return
        if (radioRefillJob?.isActive == true) return

        radioRefillJob = scope.launch {
            radioRefillMutex.withLock {
                val session = radioSession ?: return@withLock
                val provider = radioRecommendationProvider ?: return@withLock
                if (remainingRadioTracks() >= RADIO_LOW_WATER_MARK) return@withLock

                val currentIsRadioGenerated =
                    currentQueueEntry()?.source == QueueEntrySource.RADIO_GENERATED
                val seed = if (currentIsRadioGenerated) {
                    _currentTrack.value ?: session.seedHistory.lastOrNull() ?: session.seed
                } else {
                    session.seedHistory.lastOrNull() ?: session.seed
                }
                val excludedKeys = buildSet {
                    addAll(session.seenTrackKeys)
                    _queueEntries.value.forEach { add(recommendationTrackKey(it.track)) }
                }
                val recommendations = try {
                    provider.recommend(seed, excludedKeys, RADIO_BATCH_SIZE)
                        .distinctBy(::recommendationTrackKey)
                        .filterNot { recommendationTrackKey(it) in excludedKeys }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    emptyList()
                }
                if (syncManager?.isSelfActiveDevice?.value == false) return@withLock
                val currentSession = radioSession
                    ?.takeIf { it.sessionId == session.sessionId }
                    ?: return@withLock
                val currentExcludedKeys = buildSet {
                    addAll(currentSession.seenTrackKeys)
                    _queueEntries.value.forEach { add(recommendationTrackKey(it.track)) }
                }
                val freshRecommendations = recommendations
                    .filterNot { recommendationTrackKey(it) in currentExcludedKeys }
                if (freshRecommendations.isEmpty()) return@withLock

                val additions = freshRecommendations.map { track ->
                    QueueEntry(
                        entryId = nextQueueEntryId++,
                        track = track,
                        source = QueueEntrySource.RADIO_GENERATED
                    )
                }
                originalQueue = originalQueue + additions
                val visibleQueue =
                    _queueEntries.value + if (_shuffleMode.value) additions.shuffled() else additions
                setVisibleQueue(visibleQueue)
                radioSession = currentSession.copy(
                    seenTrackKeys = (currentSession.seenTrackKeys + freshRecommendations.map(::recommendationTrackKey))
                        .distinct()
                        .takeLast(RADIO_SEEN_HISTORY_LIMIT)
                )
                updateSkipFlags()
                preloadNextTrack()
                scheduleSave()
            }
        }
    }

    private fun remainingRadioTracks(): Int = _queueEntries.value
        .withIndex()
        .count { (index, entry) ->
            index > _currentIndex.value && entry.source == QueueEntrySource.RADIO_GENERATED
        }

    private fun startLoadingTrack(track: Track) {
        _status.value = PlaybackStatus.BUFFERING
        _positionMs.value = 0L
        _durationMs.value = track.durationMs
        _bufferedPositionMs.value =
            if (track.isCached && track.durationMs > 0L) track.durationMs else 0L
        notifySyncState()
        loadJob?.cancel()
        preloadJob?.cancel()

        loadJob = scope.launch {
            val streamUrl = resolveStreamUrl(track)
            if (streamUrl == null) {
                audioEngine.stop()
                return@launch
            }

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

    private suspend fun resolveStreamUrl(track: Track): String? {
        val ticketResult = apiClient.getPlaybackInfo(track.id)
        val playbackInfo = ticketResult.getOrNull() ?: return null
        val streamUrl = playbackInfo.stream_url.trim()
        if (streamUrl.isBlank()) return null
        return if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
            streamUrl
        } else {
            "${apiClient.baseUrl}$streamUrl"
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
            val entry = _queueEntries.value[nextIdx]
            val track = entry.track
            _currentTrack.value = track
            recordRadioSeed(entry)
            updateSkipFlags()
            startLoadingTrack(track)
            scheduleSave()
            scheduleRadioRefill()
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
            val entry = _queueEntries.value[prevIdx]
            val track = entry.track
            _currentTrack.value = track
            recordRadioSeed(entry)
            updateSkipFlags()
            startLoadingTrack(track)
            scheduleSave()
            scheduleRadioRefill()
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

        val currentEntryId = currentQueueEntry()?.entryId
        if (enabled) {
            val entries = originalQueue
            val currentEntry = entries.firstOrNull { it.entryId == currentEntryId }
            if (currentEntry != null && radioSession != null) {
                val remaining = entries.filterNot { it.entryId == currentEntry.entryId }
                val contextAndManual = remaining
                    .filterNot { it.source == QueueEntrySource.RADIO_GENERATED }
                    .shuffled()
                val generated = remaining
                    .filter { it.source == QueueEntrySource.RADIO_GENERATED }
                    .shuffled()
                setVisibleQueue(listOf(currentEntry) + contextAndManual + generated)
                _currentIndex.value = 0
            } else if (currentEntry != null) {
                val shuffled = entries.filterNot { it.entryId == currentEntry.entryId }.shuffled()
                setVisibleQueue(listOf(currentEntry) + shuffled)
                _currentIndex.value = 0
            } else {
                setVisibleQueue(entries.shuffled())
                _currentIndex.value = 0
            }
        } else {
            setVisibleQueue(originalQueue)
            val idx =
                if (currentEntryId != null) originalQueue.indexOfFirst { it.entryId == currentEntryId } else 0
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
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("queue_add", Json.encodeToJsonElement(track)) }
            return
        }

        val entry = QueueEntry(nextQueueEntryId++, track, QueueEntrySource.MANUAL)
        val naturalInsertIndex =
            originalQueue.indexOfFirst { it.source == QueueEntrySource.RADIO_GENERATED }
                .takeIf { it >= 0 } ?: originalQueue.size
        originalQueue = originalQueue.toMutableList().apply { add(naturalInsertIndex, entry) }
        val visible = _queueEntries.value.toMutableList()
        val visibleInsertIndex =
            visible.indexOfFirst { it.source == QueueEntrySource.RADIO_GENERATED }
                .takeIf { it >= 0 } ?: visible.size
        visible.add(visibleInsertIndex, entry)
        setVisibleQueue(visible)
        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
        scheduleRadioRefill()
    }

    override fun playNextInQueue(track: Track) {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch {
                syncManager?.sendCommand(
                    "queue_play_next",
                    Json.encodeToJsonElement(track)
                )
            }
            return
        }

        val entry = QueueEntry(nextQueueEntryId++, track, QueueEntrySource.MANUAL)
        val visibleInsertIndex = (_currentIndex.value + 1).coerceIn(0, _queueEntries.value.size)
        val visible = _queueEntries.value.toMutableList().apply { add(visibleInsertIndex, entry) }
        setVisibleQueue(visible)

        val currentEntryId = visible.getOrNull(_currentIndex.value)?.entryId
        val naturalCurrentIndex = originalQueue.indexOfFirst { it.entryId == currentEntryId }
        val naturalInsertIndex =
            if (naturalCurrentIndex >= 0) naturalCurrentIndex + 1 else originalQueue.size
        originalQueue = originalQueue.toMutableList().apply { add(naturalInsertIndex, entry) }

        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
        scheduleRadioRefill()
    }

    override fun moveInQueue(fromIndex: Int, toIndex: Int) {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch {
                syncManager?.sendCommand(
                    "queue_move",
                    buildJsonObject {
                        put("from", fromIndex)
                        put("to", toIndex)
                    }
                )
            }
            return
        }
        val q = _queueEntries.value.toMutableList()
        if (fromIndex !in q.indices || toIndex !in q.indices) return

        val moved = q.removeAt(fromIndex)
        q.add(toIndex, moved)
        setVisibleQueue(q)
        originalQueue = q

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
        scheduleRadioRefill()
    }

    override fun removeAt(index: Int) {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("queue_remove", JsonPrimitive(index)) }
            return
        }
        val q = _queueEntries.value.toMutableList()
        if (index !in q.indices) return

        val removed = q.removeAt(index)
        originalQueue = originalQueue.filterNot { it.entryId == removed.entryId }
        setVisibleQueue(q)

        val cur = _currentIndex.value
        if (q.isEmpty()) {
            stopAndDismiss()
        } else if (index == cur) {
            val newIdx = cur.coerceIn(0, q.size - 1)
            _currentIndex.value = newIdx
            val nextEntry = q[newIdx]
            val nextTrack = nextEntry.track
            _currentTrack.value = nextTrack
            recordRadioSeed(nextEntry)
            startLoadingTrack(nextTrack)
        } else if (index < cur) {
            _currentIndex.value = cur - 1
        }

        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
        scheduleRadioRefill()
    }

    override fun clearQueue() {
        if (syncManager?.isSelfActiveDevice?.value == false) {
            scope.launch { syncManager?.sendCommand("queue_clear") }
            return
        }
        radioRefillJob?.cancel()
        radioSession = null
        val currentEntry = currentQueueEntry()
        if (currentEntry == null) {
            originalQueue = emptyList()
            setVisibleQueue(emptyList())
            _currentIndex.value = -1
            stopAndDismiss()
            return
        }

        originalQueue = listOf(currentEntry)
        setVisibleQueue(originalQueue)
        _currentIndex.value = 0
        updateSkipFlags()
        preloadNextTrack()
        scheduleSave()
    }

    override fun stopAndDismiss() {
        radioRefillJob?.cancel()
        radioSession = null
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
            val snapshot = playbackSnapshot().copy(positionMs = currentPositionMs)
            storage.saveState(snapshot)
            notifySyncState()
        }
    }

    private companion object {
        const val RADIO_SEED_HISTORY_LIMIT = 6
        const val RADIO_LOW_WATER_MARK = 4
        const val RADIO_BATCH_SIZE = 8
        const val RADIO_SEEN_HISTORY_LIMIT = 500
    }
}
