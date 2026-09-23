package org.shilpo.peerless.tasks

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.shilpo.peerless.model.ActiveRipTask
import org.shilpo.peerless.model.RipStage
import org.shilpo.peerless.model.RipTaskSnapshotDto
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.model.UncachedTrackDto
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.player.playTrack

val LocalRipCoordinator = staticCompositionLocalOf<RipCoordinator?> { null }

open class RipCoordinator(
    val apiClient: PeerlessApiClient = PeerlessApiClient(),
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val serverTaskSnapshots: StateFlow<List<RipTaskSnapshotDto>?>? = null
) {
    // This map is only a UI projection of task state reported by the server.
    private val _activeTasks = MutableStateFlow<Map<String, ActiveRipTask>>(emptyMap())
    val activeTasks: StateFlow<Map<String, ActiveRipTask>> = _activeTasks.asStateFlow()

    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    private val autoPlayConnections = mutableMapOf<String, PlayerConnection>()

    init {
        serverTaskSnapshots?.let { snapshots ->
            coroutineScope.launch {
                snapshots.collect { current ->
                    if (current != null) applyServerSnapshots(current)
                }
            }
        }
    }

    private fun updateServerProjection(transform: (Map<String, ActiveRipTask>) -> Map<String, ActiveRipTask>) {
        _activeTasks.update { current ->
            val updated = transform(current)
            _activeCount.value = updated.values.count { !it.isFinished }
            updated
        }
    }

    /** Replace the UI projection with the server's live task snapshot. */
    suspend fun refreshServerTasks(token: String? = null): Result<List<RipTaskSnapshotDto>> {
        val result = apiClient.listRipTasks(token)
        result.onSuccess { snapshots ->
            applyServerSnapshots(snapshots)
        }
        return result
    }

    private fun applyServerSnapshots(snapshots: List<RipTaskSnapshotDto>) {
        val current = _activeTasks.value
        val next = snapshots.filterNot { it.isTerminal }.associate { snapshot ->
            snapshot.task_id to snapshot.toActiveRipTask(current[snapshot.task_id])
        }
        (current.keys - next.keys).forEach { taskId ->
            autoPlayConnections.remove(taskId)
        }
        updateServerProjection { next }
        next.keys.forEach(::triggerAutoPlayIfReady)
    }

    suspend fun clearServerProjection() {
        autoPlayConnections.clear()
        updateServerProjection { emptyMap() }
    }

    suspend fun ripTrack(
        track: TrackSummaryDto,
        provider: String? = null,
        codec: String? = null,
        token: String? = null
    ): Result<String> {
        val reqProvider = (provider ?: track.provider).ifBlank { "apple" }
        val reqCodec = codec ?: track.codec.ifBlank { "alac" }
        val createResult = apiClient.createRipTask(
            provider = reqProvider,
            trackId = track.track_id,
            codec = reqCodec,
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration = track.duration,
            token = token
        )

        if (createResult.isFailure) return createResult.map { it.task_id }
        val taskId = createResult.getOrThrow().task_id
        // The REST snapshot immediately reconciles the UI; WebSocket events maintain it live.
        refreshServerTasks(token)
        return Result.success(taskId)
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
        if (ripResult.isFailure) return ripResult

        val taskId = ripResult.getOrThrow()
        autoPlayConnections[taskId] = playerConnection
        updateServerProjection { current ->
            val task = current[taskId] ?: return@updateServerProjection current
            current + (taskId to task.copy(isAutoPlayPending = true))
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
        val result = apiClient.cancelRipTask(taskId, token)
        if (result.isSuccess) refreshServerTasks(token)
        return result
    }

    private fun triggerAutoPlayIfReady(taskId: String) {
        val task = _activeTasks.value[taskId] ?: return
        if (!task.isAutoPlayPending) return
        val isCompleted = task.stage == RipStage.COMPLETED
        val hasPlayableId = task.resultingTrackId != null || task.track.id > 0
        if (isCompleted && hasPlayableId) {
            val playerConnection = autoPlayConnections.remove(taskId) ?: return
            updateServerProjection { current ->
                val existing = current[taskId] ?: return@updateServerProjection current
                current + (taskId to existing.copy(isAutoPlayPending = false))
            }
            val resolvedId = task.resultingTrackId?.toIntOrNull() ?: task.track.id
            playerConnection.playTrack(task.track.copy(id = resolvedId, is_cached = true))
        }
    }

    private fun RipTaskSnapshotDto.toActiveRipTask(existing: ActiveRipTask? = null): ActiveRipTask {
        val taskTrack = TrackSummaryDto(
            id = result_track_id ?: existing?.track?.id ?: 0,
            provider = provider,
            track_id = source_track_id,
            title = title ?: existing?.track?.title.orEmpty(),
            artist = artist ?: existing?.track?.artist.orEmpty(),
            album = album ?: existing?.track?.album.orEmpty(),
            duration = duration ?: existing?.track?.duration ?: 0,
            codec = codec ?: existing?.track?.codec ?: "alac",
            is_cached = is_cached ?: (completed && stage == "completed"),
            artwork_url = existing?.track?.artwork_url
        )
        return ActiveRipTask(
            taskId = task_id,
            track = taskTrack,
            stage = RipStage.fromStage(stage),
            percent = percent ?: existing?.percent ?: 0f,
            speed = speed,
            completed = completed,
            error = error,
            resultingTrackId = result_track_id?.toString() ?: existing?.resultingTrackId,
            isOwner = is_owner,
            isAutoPlayPending = existing?.isAutoPlayPending ?: false
        )
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
    ): Job = coroutineScope.launch { onResult?.invoke(ripTrack(track, provider, codec, token)) }

    fun cancelRipAsync(
        taskId: String,
        token: String? = null,
        onResult: ((Result<Unit>) -> Unit)? = null
    ): Job = coroutineScope.launch { onResult?.invoke(cancelRip(taskId, token)) }

    fun getTaskForTrack(track: TrackSummaryDto): ActiveRipTask? =
        _activeTasks.value.values.firstOrNull { task ->
            (task.track.id > 0 && task.track.id == track.id) ||
                    (task.track.provider.equals(
                        track.provider,
                        ignoreCase = true
                    ) && task.track.track_id == track.track_id)
        }

    fun getTaskForTrack(track: UncachedTrackDto): ActiveRipTask? =
        _activeTasks.value.values.firstOrNull { task ->
            task.track.provider.equals(
                track.provider,
                ignoreCase = true
            ) && task.track.track_id == track.track_id
        }

    fun getTask(taskId: String): ActiveRipTask? = _activeTasks.value[taskId]

    fun getTaskFlow(taskId: String): Flow<ActiveRipTask?> = activeTasks.map { it[taskId] }

    companion object {
        private var defaultInstance: RipCoordinator? = null

        fun getInstance(
            apiClient: PeerlessApiClient = PeerlessApiClient(),
            coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
            serverTaskSnapshots: StateFlow<List<RipTaskSnapshotDto>?>? = null
        ): RipCoordinator = defaultInstance ?: RipCoordinator(
            apiClient = apiClient,
            coroutineScope = coroutineScope,
            serverTaskSnapshots = serverTaskSnapshots
        ).also {
            defaultInstance = it
        }
    }
}
