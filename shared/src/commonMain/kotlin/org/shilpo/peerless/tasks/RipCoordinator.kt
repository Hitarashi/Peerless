package org.shilpo.peerless.tasks

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.shilpo.peerless.model.*
import org.shilpo.peerless.network.PeerlessApiClient

class RipCoordinator(
    val apiClient: PeerlessApiClient = PeerlessApiClient(),
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _activeTasks = MutableStateFlow<Map<String, ActiveRipTask>>(emptyMap())
    val activeTasks: StateFlow<Map<String, ActiveRipTask>> = _activeTasks.asStateFlow()

    private val runningJobs = mutableMapOf<String, Job>()

    suspend fun startRip(
        track: TrackSummaryDto,
        provider: String? = null,
        codec: String? = null,
        token: String? = null
    ): Result<String> {
        val reqProvider = provider ?: track.provider
        val reqTrackId = track.track_id
        val reqCodec = codec ?: track.codec
        val result = apiClient.createRipTask(
            provider = reqProvider,
            trackId = reqTrackId,
            codec = reqCodec,
            token = token
        )
        return result.map { resp ->
            val taskId = resp.task_id
            val initialTask = ActiveRipTask(
                taskId = taskId,
                track = track,
                stage = RipStage.QUEUED,
                percent = 0f
            )
            _activeTasks.update { it + (taskId to initialTask) }
            monitorTask(taskId, track, token)
            taskId
        }
    }

    suspend fun startRip(
        track: UncachedTrackDto,
        provider: String? = null,
        codec: String? = null,
        token: String? = null
    ): Result<String> {
        val summary = TrackSummaryDto(
            id = 0,
            provider = track.provider,
            track_id = track.track_id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration = track.duration,
            codec = codec ?: "alac",
            is_cached = false
        )
        return startRip(summary, provider, codec, token)
    }

    fun startRipAsync(
        track: TrackSummaryDto,
        provider: String? = null,
        codec: String? = null,
        token: String? = null,
        onResult: ((Result<String>) -> Unit)? = null
    ): Job = coroutineScope.launch {
        val res = startRip(track, provider, codec, token)
        onResult?.invoke(res)
    }

    fun monitorTask(taskId: String, track: TrackSummaryDto, token: String? = null): Job {
        return monitorTaskFlow(taskId, track, apiClient.streamTaskEvents(taskId, token))
    }

    fun monitorTaskFlow(taskId: String, track: TrackSummaryDto, eventsFlow: Flow<TaskProgressEvent>): Job {
        runningJobs[taskId]?.cancel()
        val job = coroutineScope.launch {
            eventsFlow.collect { event ->
                val stage = RipStage.fromStage(event.effectiveStage)
                val percent = event.effectivePercent
                val speed = event.speed
                val hasError = event.error != null || stage == RipStage.ERROR
                val isCompleted = (event.completed || stage == RipStage.COMPLETED) && !hasError

                _activeTasks.update { currentMap ->
                    val existing = currentMap[taskId] ?: ActiveRipTask(taskId = taskId, track = track)
                    val resolvedStage = when {
                        hasError -> RipStage.ERROR
                        isCompleted -> RipStage.COMPLETED
                        else -> stage
                    }
                    val resolvedPercent = if (isCompleted) 100f else percent
                    val updated = existing.copy(
                        stage = resolvedStage,
                        percent = resolvedPercent,
                        speed = speed ?: existing.speed,
                        completed = isCompleted,
                        error = event.error ?: if (stage == RipStage.ERROR) "Rip failed" else null,
                        resultingTrackId = event.track_id ?: existing.resultingTrackId
                    )
                    currentMap + (taskId to updated)
                }
            }
        }
        runningJobs[taskId] = job
        return job
    }

    fun dismissTask(taskId: String) {
        runningJobs.remove(taskId)?.cancel()
        _activeTasks.update { it - taskId }
    }

    fun getTaskForTrack(trackSummaryDto: TrackSummaryDto): ActiveRipTask? {
        return _activeTasks.value.values.firstOrNull { task ->
            (task.track.id > 0 && task.track.id == trackSummaryDto.id) ||
                    (task.track.provider.equals(trackSummaryDto.provider, ignoreCase = true) &&
                            task.track.track_id == trackSummaryDto.track_id)
        }
    }

    fun getTaskForTrack(track: UncachedTrackDto): ActiveRipTask? {
        return _activeTasks.value.values.firstOrNull { task ->
            task.track.provider.equals(track.provider, ignoreCase = true) &&
                    task.track.track_id == track.track_id
        }
    }

    fun getTask(taskId: String): ActiveRipTask? = _activeTasks.value[taskId]

    fun getTaskFlow(taskId: String): Flow<ActiveRipTask?> = activeTasks.map { it[taskId] }

    companion object {
        private var defaultInstance: RipCoordinator? = null

        fun getInstance(
            apiClient: PeerlessApiClient = PeerlessApiClient(),
            coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        ): RipCoordinator {
            return defaultInstance ?: RipCoordinator(apiClient, coroutineScope).also {
                defaultInstance = it
            }
        }
    }
}
