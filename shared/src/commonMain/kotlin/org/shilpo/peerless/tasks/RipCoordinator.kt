package org.shilpo.peerless.tasks

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.shilpo.peerless.model.ActiveRipTask
import org.shilpo.peerless.model.RipStage
import org.shilpo.peerless.model.TaskProgressEvent
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.UncachedTrackDto
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.player.playTrack

val LocalRipCoordinator = staticCompositionLocalOf<RipCoordinator?> { null }

private class TaskCompletedException : CancellationException("Rip task completed")

open class RipCoordinator(
    val apiClient: PeerlessApiClient = PeerlessApiClient(),
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val initialRetryDelayMs: Long = 500L,
    private val maxSseRetries: Int = 3,
    val autoCleanCompleted: Boolean = true
) {
    private val _activeTasks = MutableStateFlow<Map<String, ActiveRipTask>>(emptyMap())
    val activeTasks: StateFlow<Map<String, ActiveRipTask>> = _activeTasks.asStateFlow()

    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    private val runningJobs = mutableMapOf<String, Job>()
    private val autoPlayConnections = mutableMapOf<String, PlayerConnection>()

    private fun updateActiveTasks(transform: (Map<String, ActiveRipTask>) -> Map<String, ActiveRipTask>) {
        _activeTasks.update { current ->
            val updated = transform(current)
            _activeCount.value = updated.values.count { !it.isFinished }
            updated
        }
    }

    suspend fun ripTrack(
        track: TrackSummaryDto,
        provider: String? = null,
        codec: String? = null,
        token: String? = null
    ): Result<String> {
        val reqProvider = (provider ?: track.provider).ifBlank { "apple" }
        val reqTrackId = track.track_id
        val reqCodec = codec ?: track.codec.ifBlank { "alac" }

        // Deduplicates: if a rip for (provider, track.track_id) is already running, returns that task ID
        val runningTask = _activeTasks.value.values.firstOrNull { task ->
            !task.isFinished &&
                    task.track.provider.equals(reqProvider, ignoreCase = true) &&
                    task.track.track_id == reqTrackId
        }
        if (runningTask != null) {
            return Result.success(runningTask.taskId)
        }

        val createResult = apiClient.createRipTask(
            provider = reqProvider,
            trackId = reqTrackId,
            codec = reqCodec,
            token = token
        )

        return createResult.map { resp ->
            val taskId = resp.task_id
            val initialTask = ActiveRipTask(
                taskId = taskId,
                track = track.copy(provider = reqProvider, codec = reqCodec),
                stage = RipStage.QUEUED,
                percent = 0f,
                isOwner = true
            )
            updateActiveTasks { it + (taskId to initialTask) }
            monitorTask(taskId, initialTask.track, token)
            taskId
        }
    }

    suspend fun ripTrack(
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
        return ripTrack(summary, provider, codec, token)
    }

    suspend fun ripAndPlay(
        track: TrackSummaryDto,
        playerConnection: PlayerConnection,
        provider: String? = null,
        codec: String? = null,
        token: String? = null
    ): Result<String> {
        val ripResult = ripTrack(track, provider, codec, token)
        if (ripResult.isFailure) {
            return ripResult
        }
        val taskId = ripResult.getOrThrow()
        autoPlayConnections[taskId] = playerConnection
        updateActiveTasks { currentMap ->
            val existing = currentMap[taskId] ?: return@updateActiveTasks currentMap
            currentMap + (taskId to existing.copy(isAutoPlayPending = true))
        }
        triggerAutoPlayIfReady(taskId)
        return Result.success(taskId)
    }

    suspend fun ripAndPlay(
        track: UncachedTrackDto,
        playerConnection: PlayerConnection,
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
        return ripAndPlay(summary, playerConnection, provider, codec, token)
    }

    suspend fun cancelRip(taskId: String, token: String? = null): Result<Unit> {
        runningJobs.remove(taskId)?.cancel()
        autoPlayConnections.remove(taskId)
        updateActiveTasks { currentMap ->
            val existing = currentMap[taskId] ?: return@updateActiveTasks currentMap
            currentMap + (taskId to existing.copy(
                stage = RipStage.CANCELLED,
                completed = true,
                isAutoPlayPending = false
            ))
        }
        return apiClient.cancelRipTask(taskId, token)
    }

    suspend fun retryRip(taskId: String, token: String? = null): Result<String> {
        val existingTask = _activeTasks.value[taskId]
            ?: return Result.failure(IllegalArgumentException("Task $taskId not found"))
        val pendingPlayer = autoPlayConnections.remove(taskId)
        val wasAutoPlay = existingTask.isAutoPlayPending || pendingPlayer != null

        dismissTask(taskId)

        return if (wasAutoPlay && pendingPlayer != null) {
            ripAndPlay(
                track = existingTask.track,
                playerConnection = pendingPlayer,
                provider = existingTask.track.provider,
                codec = existingTask.track.codec,
                token = token
            )
        } else {
            ripTrack(
                track = existingTask.track,
                provider = existingTask.track.provider,
                codec = existingTask.track.codec,
                token = token
            )
        }
    }

    fun dismissTask(taskId: String) {
        runningJobs.remove(taskId)?.cancel()
        autoPlayConnections.remove(taskId)
        updateActiveTasks { it - taskId }
    }

    fun clearFinishedTasks() {
        val finishedTasks = _activeTasks.value.filterValues { task ->
            task.completed || task.stage == RipStage.ERROR || task.stage == RipStage.CANCELLED || task.stage == RipStage.COMPLETED
        }
        finishedTasks.keys.forEach { taskId ->
            runningJobs.remove(taskId)?.cancel()
            autoPlayConnections.remove(taskId)
        }
        updateActiveTasks { map ->
            map - finishedTasks.keys
        }
    }

    fun monitorTask(taskId: String, track: TrackSummaryDto, token: String? = null): Job {
        return monitorTaskFlow(taskId, track) {
            apiClient.streamTaskEvents(taskId, token)
        }
    }

    fun monitorTaskFlow(
        taskId: String,
        track: TrackSummaryDto,
        eventsFlow: Flow<TaskProgressEvent>
    ): Job {
        return monitorTaskFlow(taskId, track) { eventsFlow }
    }

    fun monitorTaskFlow(
        taskId: String,
        track: TrackSummaryDto,
        flowProvider: () -> Flow<TaskProgressEvent>
    ): Job {
        runningJobs[taskId]?.cancel()
        val job = coroutineScope.launch {
            var retryCount = 0
            var delayMs = initialRetryDelayMs

            try {
                while (isActive) {
                    var completedOrCancelled = false
                    var streamError: String? = null

                    try {
                        flowProvider().collect { event ->
                            val stage = RipStage.fromStage(event.effectiveStage)
                            val percent = event.effectivePercent
                            val speed = event.speed
                            val isError = event.error != null || stage == RipStage.ERROR
                            val isCompleted =
                                (event.completed || stage == RipStage.COMPLETED) && !isError

                            if (isError) {
                                streamError = event.error ?: "Rip failed"
                            } else {
                                // Valid progress received, reset retry budget
                                retryCount = 0
                                delayMs = initialRetryDelayMs

                                updateActiveTasks { currentMap ->
                                    val existing = currentMap[taskId] ?: ActiveRipTask(
                                        taskId = taskId,
                                        track = track,
                                        isOwner = true
                                    )
                                    val resolvedStage =
                                        if (isCompleted) RipStage.COMPLETED else stage
                                    val resolvedPercent = if (isCompleted) 100f else percent
                                    val updated = existing.copy(
                                        stage = resolvedStage,
                                        percent = resolvedPercent,
                                        speed = speed ?: existing.speed,
                                        completed = isCompleted,
                                        error = null,
                                        resultingTrackId = event.track_id
                                            ?: existing.resultingTrackId
                                    )
                                    currentMap + (taskId to updated)
                                }

                                if (isCompleted) {
                                    completedOrCancelled = true
                                    triggerAutoPlayIfReady(taskId)
                                    if (autoCleanCompleted) {
                                        dismissTask(taskId)
                                    }
                                    throw TaskCompletedException()
                                }
                            }
                        }
                    } catch (e: TaskCompletedException) {
                        break
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Throwable) {
                        streamError = e.message ?: "Network error during SSE stream"
                    }

                    val currentTask = _activeTasks.value[taskId]
                    if (currentTask?.stage == RipStage.CANCELLED) {
                        break
                    }
                    if (completedOrCancelled || currentTask?.completed == true || currentTask?.stage == RipStage.COMPLETED) {
                        if (autoCleanCompleted && (completedOrCancelled || currentTask?.stage == RipStage.COMPLETED || currentTask?.completed == true)) {
                            dismissTask(taskId)
                        }
                        break
                    }

                    // SSE disconnected unexpectedly before completion -> retry with exponential backoff
                    if (retryCount < maxSseRetries) {
                        retryCount++
                        delay(delayMs)
                        delayMs *= 2
                    } else {
                        // Retries exhausted -> emit error
                        updateActiveTasks { currentMap ->
                            val existing = currentMap[taskId] ?: ActiveRipTask(
                                taskId = taskId,
                                track = track,
                                isOwner = true
                            )
                            val updated = existing.copy(
                                stage = RipStage.ERROR,
                                error = streamError
                                    ?: "SSE connection lost after $maxSseRetries retries",
                                completed = true,
                                isAutoPlayPending = false
                            )
                            currentMap + (taskId to updated)
                        }
                        autoPlayConnections.remove(taskId)
                        break
                    }
                }
            } finally {
                runningJobs.remove(taskId)
            }
        }
        runningJobs[taskId] = job
        return job
    }

    private fun triggerAutoPlayIfReady(taskId: String) {
        val task = _activeTasks.value[taskId] ?: return
        if (!task.isAutoPlayPending) return
        val isCompleted = task.stage == RipStage.COMPLETED || task.completed
        val hasPlayableId = task.resultingTrackId != null || task.track.id > 0
        if (isCompleted && hasPlayableId) {
            val playerConnection = autoPlayConnections.remove(taskId) ?: return
            updateActiveTasks { map ->
                val current = map[taskId] ?: return@updateActiveTasks map
                map + (taskId to current.copy(isAutoPlayPending = false))
            }
            val resolvedId = task.resultingTrackId?.toIntOrNull() ?: task.track.id
            val playableTrack = task.track.copy(
                id = resolvedId,
                is_cached = true
            )
            playerConnection.playTrack(playableTrack)
        }
    }

    // Backwards-compatible aliases
    suspend fun startRip(
        track: TrackSummaryDto,
        provider: String? = null,
        codec: String? = null,
        token: String? = null
    ): Result<String> = ripTrack(track, provider, codec, token)

    suspend fun startRip(
        track: UncachedTrackDto,
        provider: String? = null,
        codec: String? = null,
        token: String? = null
    ): Result<String> = ripTrack(track, provider, codec, token)

    fun startRipAsync(
        track: TrackSummaryDto,
        provider: String? = null,
        codec: String? = null,
        token: String? = null,
        onResult: ((Result<String>) -> Unit)? = null
    ): Job = coroutineScope.launch {
        val res = ripTrack(track, provider, codec, token)
        onResult?.invoke(res)
    }

    fun cancelRipAsync(
        taskId: String,
        token: String? = null,
        onResult: ((Result<Unit>) -> Unit)? = null
    ): Job = coroutineScope.launch {
        val res = cancelRip(taskId, token)
        onResult?.invoke(res)
    }

    fun retryRipAsync(
        taskId: String,
        token: String? = null,
        onResult: ((Result<String>) -> Unit)? = null
    ): Job = coroutineScope.launch {
        val res = retryRip(taskId, token)
        onResult?.invoke(res)
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
