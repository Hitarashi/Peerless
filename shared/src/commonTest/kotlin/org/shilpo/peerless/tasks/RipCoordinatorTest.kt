package org.shilpo.peerless.tasks

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.shilpo.peerless.model.PlaybackInfo
import org.shilpo.peerless.model.RepeatMode
import org.shilpo.peerless.model.RipStage
import org.shilpo.peerless.model.RipTaskResponse
import org.shilpo.peerless.model.TaskProgressEvent
import org.shilpo.peerless.model.Track
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.PeerlessApiClient
import org.shilpo.peerless.player.PlaybackStatus
import org.shilpo.peerless.player.PlayerConnection
import org.shilpo.peerless.player.SignalPathSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeApiClient : PeerlessApiClient() {
    var createRipCallCount = 0
    var createRipTaskHandler: suspend (String, String, String?, String?) -> Result<RipTaskResponse> =
        { _, _, _, _ ->
            Result.success(RipTaskResponse(task_id = "task_default_123", status = "queued"))
        }

    var streamEventsHandler: (String, String?) -> Flow<TaskProgressEvent> = { _, _ ->
        flowOf()
    }

    val cancelledTasks = mutableListOf<String>()
    var cancelRipHandler: suspend (String, String?) -> Result<Unit> = { _, _ ->
        Result.success(Unit)
    }

    override suspend fun createRipTask(
        provider: String,
        trackId: String,
        codec: String?,
        token: String?
    ): Result<RipTaskResponse> {
        createRipCallCount++
        return createRipTaskHandler(provider, trackId, codec, token)
    }

    override fun streamTaskEvents(taskId: String, token: String?): Flow<TaskProgressEvent> =
        streamEventsHandler(taskId, token)

    override suspend fun cancelRipTask(taskId: String, token: String?): Result<Unit> {
        cancelledTasks += taskId
        return cancelRipHandler(taskId, token)
    }
}

private class TestPlayerConnection : PlayerConnection {
    val playedTracks = mutableListOf<Track>()

    override val currentTrack: StateFlow<Track?> = MutableStateFlow(null)
    override val playbackInfo: StateFlow<PlaybackInfo?> = MutableStateFlow(null)
    override val signalPath: StateFlow<SignalPathSnapshot?> = MutableStateFlow(null)
    override val status: StateFlow<PlaybackStatus> = MutableStateFlow(PlaybackStatus.IDLE)
    override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false)
    override val queue: StateFlow<List<Track>> = MutableStateFlow(emptyList())
    override val currentIndex: StateFlow<Int> = MutableStateFlow(0)
    override val shuffleMode: StateFlow<Boolean> = MutableStateFlow(false)
    override val repeatMode: StateFlow<RepeatMode> = MutableStateFlow(RepeatMode.OFF)
    override val canSkipNext: StateFlow<Boolean> = MutableStateFlow(false)
    override val canSkipPrevious: StateFlow<Boolean> = MutableStateFlow(false)
    override val volume: StateFlow<Float> = MutableStateFlow(1f)
    override val positionMs: StateFlow<Long> = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = MutableStateFlow(0L)
    override val bufferedPositionMs: StateFlow<Long> = MutableStateFlow(0L)
    override val outputLatencyMs: StateFlow<Long> = MutableStateFlow(0L)

    override fun play(track: Track, queue: List<Track>) {
        playedTracks += track
    }

    override fun playQueueItem(index: Int) {}
    override fun startRadio(track: Track) {}
    override fun play() {}
    override fun pause() {}
    override fun togglePlayPause() {}
    override fun playNext() {}
    override fun playPrevious() {}
    override fun seekTo(positionMs: Long) {}
    override fun setVolume(volume: Float) {}
    override fun setShuffleMode(enabled: Boolean) {}
    override fun toggleShuffle() {}
    override fun setRepeatMode(mode: RepeatMode) {}
    override fun cycleRepeatMode() {}
    override fun addToQueue(track: Track) {}
    override fun playNextInQueue(track: Track) {}
    override fun moveInQueue(fromIndex: Int, toIndex: Int) {}
    override fun removeAt(index: Int) {}
    override fun clearQueue() {}
    override fun stopAndDismiss() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class RipCoordinatorTest {

    private fun sampleTrack(id: Int = 0, trackId: String = "track_123") = TrackSummaryDto(
        id = id,
        provider = "apple",
        track_id = trackId,
        title = "Test Song",
        artist = "Test Artist",
        album = "Test Album",
        duration = 240,
        codec = "alac",
        is_cached = false
    )

    @Test
    fun testCancelRipTaskInApiClient() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/tasks/task_test_99", request.url.encodedPath)
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("Bearer sample_token", request.headers[HttpHeaders.Authorization])
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = PeerlessApiClient(
            baseUrl = "https://server.peerless.test",
            httpClient = HttpClient(mockEngine)
        )
        val result = client.cancelRipTask("task_test_99", "sample_token")
        assertTrue(result.isSuccess)
    }

    @Test
    fun testRipTrackLifecycleAndActiveCount() = runTest {
        val fakeApi = FakeApiClient()
        val sseEvents = MutableSharedFlow<TaskProgressEvent>(extraBufferCapacity = 64)
        fakeApi.streamEventsHandler = { _, _ -> sseEvents }

        val coordinator = RipCoordinator(
            apiClient = fakeApi,
            coroutineScope = this
        )

        assertEquals(0, coordinator.activeCount.value)

        val track = sampleTrack()
        val result = coordinator.ripTrack(track)
        assertTrue(result.isSuccess)
        val taskId = result.getOrThrow()
        assertEquals("task_default_123", taskId)

        runCurrent()
        assertEquals(1, coordinator.activeCount.value)
        val initialTask = coordinator.activeTasks.value[taskId]
        assertNotNull(initialTask)
        assertEquals(RipStage.QUEUED, initialTask.stage)
        assertTrue(initialTask.isOwner)
        assertFalse(initialTask.isAutoPlayPending)

        // Progress event: downloading
        sseEvents.emit(
            TaskProgressEvent(
                task_id = taskId,
                stage = "downloading",
                percent = 45f,
                speed = "1.5 MB/s"
            )
        )
        runCurrent()
        val downloadingTask = coordinator.activeTasks.value[taskId]
        assertNotNull(downloadingTask)
        assertEquals(RipStage.DOWNLOADING, downloadingTask.stage)
        assertEquals(45f, downloadingTask.percent)
        assertEquals(1, coordinator.activeCount.value)

        // Progress event: completed
        sseEvents.emit(
            TaskProgressEvent(
                task_id = taskId,
                stage = "completed",
                percent = 100f,
                track_id = "501",
                completed = true
            )
        )
        runCurrent()
        // Auto cleaned upon completion
        assertNull(coordinator.activeTasks.value[taskId])
        assertEquals(0, coordinator.activeCount.value)
    }

    @Test
    fun testRipTrackProgressionWithAutoCleanDisabled() = runTest {
        val fakeApi = FakeApiClient()
        val sseEvents = MutableSharedFlow<TaskProgressEvent>(extraBufferCapacity = 64)
        fakeApi.streamEventsHandler = { _, _ -> sseEvents }

        val coordinator = RipCoordinator(
            apiClient = fakeApi,
            coroutineScope = this,
            autoCleanCompleted = false
        )

        val track = sampleTrack(trackId = "track_retain")
        val res = coordinator.ripTrack(track)
        val taskId = res.getOrThrow()
        runCurrent()

        sseEvents.emit(
            TaskProgressEvent(
                task_id = taskId,
                stage = "completed",
                percent = 100f,
                track_id = "999",
                completed = true
            )
        )
        runCurrent()

        val completedTask = coordinator.activeTasks.value[taskId]
        assertNotNull(completedTask)
        assertEquals(RipStage.COMPLETED, completedTask.stage)
        assertTrue(completedTask.isCompleted)
        assertEquals(0, coordinator.activeCount.value)
    }

    @Test
    fun testRipTrackDeduplication() = runTest {
        val fakeApi = FakeApiClient()
        val sseEvents = MutableSharedFlow<TaskProgressEvent>(extraBufferCapacity = 64)
        fakeApi.streamEventsHandler = { _, _ -> sseEvents }

        val coordinator = RipCoordinator(
            apiClient = fakeApi,
            coroutineScope = this
        )

        val track = sampleTrack(trackId = "shared_track_id")
        val res1 = coordinator.ripTrack(track, provider = "apple")
        assertTrue(res1.isSuccess)
        assertEquals(1, fakeApi.createRipCallCount)

        // Second rip for the same provider and trackId while first is running
        val res2 = coordinator.ripTrack(track, provider = "apple")
        assertTrue(res2.isSuccess)
        assertEquals(res1.getOrThrow(), res2.getOrThrow())
        // Should not have called createRipTask again
        assertEquals(1, fakeApi.createRipCallCount)

        coordinator.dismissTask(res1.getOrThrow())
    }

    @Test
    fun testRipAndPlayAutoPlaysOnCompletion() = runTest {
        val fakeApi = FakeApiClient()
        val sseEvents = MutableSharedFlow<TaskProgressEvent>(extraBufferCapacity = 64)
        fakeApi.streamEventsHandler = { _, _ -> sseEvents }

        val player = TestPlayerConnection()
        val coordinator = RipCoordinator(
            apiClient = fakeApi,
            coroutineScope = this
        )

        val track = sampleTrack(id = 0, trackId = "track_autoplay")
        val res = coordinator.ripAndPlay(track, player)
        assertTrue(res.isSuccess)
        val taskId = res.getOrThrow()

        runCurrent()
        val task = coordinator.activeTasks.value[taskId]
        assertNotNull(task)
        assertTrue(task.isAutoPlayPending)
        assertEquals(0, player.playedTracks.size)

        // Emit completed with resulting track_id
        sseEvents.emit(
            TaskProgressEvent(
                task_id = taskId,
                stage = "completed",
                track_id = "888",
                completed = true
            )
        )
        runCurrent()

        assertEquals(1, player.playedTracks.size)
        val played = player.playedTracks.first()
        assertEquals(888, played.id)
        assertTrue(played.isCached)

        // Task auto-cleaned upon done
        assertNull(coordinator.activeTasks.value[taskId])
    }

    @Test
    fun testCancelRip() = runTest {
        val fakeApi = FakeApiClient()
        val sseEvents = MutableSharedFlow<TaskProgressEvent>()
        fakeApi.streamEventsHandler = { _, _ -> sseEvents }

        val coordinator = RipCoordinator(
            apiClient = fakeApi,
            coroutineScope = this
        )

        val track = sampleTrack()
        val res = coordinator.ripTrack(track)
        val taskId = res.getOrThrow()
        runCurrent()

        assertEquals(1, coordinator.activeCount.value)

        val cancelRes = coordinator.cancelRip(taskId, token = "test_tok")
        assertTrue(cancelRes.isSuccess)
        runCurrent()

        assertEquals(listOf(taskId), fakeApi.cancelledTasks)
        val cancelledTask = coordinator.activeTasks.value[taskId]
        assertNotNull(cancelledTask)
        assertEquals(RipStage.CANCELLED, cancelledTask.stage)
        assertTrue(cancelledTask.completed)
        assertEquals(0, coordinator.activeCount.value)
    }

    @Test
    fun testRetryRip() = runTest {
        val fakeApi = FakeApiClient()
        var callCount = 0
        fakeApi.createRipTaskHandler = { _, _, _, _ ->
            callCount++
            Result.success(RipTaskResponse(task_id = "task_$callCount", status = "queued"))
        }

        val coordinator = RipCoordinator(
            apiClient = fakeApi,
            coroutineScope = this
        )

        val track = sampleTrack()
        val res1 = coordinator.ripTrack(track)
        val taskId1 = res1.getOrThrow()
        assertEquals("task_1", taskId1)
        runCurrent()

        val retryRes = coordinator.retryRip(taskId1)
        assertTrue(retryRes.isSuccess)
        val taskId2 = retryRes.getOrThrow()
        assertEquals("task_2", taskId2)

        // Old task is dismissed
        assertNull(coordinator.activeTasks.value[taskId1])
        assertNotNull(coordinator.activeTasks.value[taskId2])

        coordinator.dismissTask(taskId2)
    }

    @Test
    fun testDismissAndClearFinishedTasks() = runTest {
        val fakeApi = FakeApiClient()
        val coordinator = RipCoordinator(
            apiClient = fakeApi,
            coroutineScope = this
        )

        val track1 = sampleTrack(trackId = "t1")
        val track2 = sampleTrack(trackId = "t2")
        val track3 = sampleTrack(trackId = "t3")

        fakeApi.createRipTaskHandler = { _, id, _, _ ->
            Result.success(RipTaskResponse(task_id = id, status = "queued"))
        }

        coordinator.ripTrack(track1)
        coordinator.ripTrack(track2)
        coordinator.ripTrack(track3)
        runCurrent()

        assertEquals(3, coordinator.activeTasks.value.size)

        // Mark t1 cancelled
        coordinator.cancelRip("t1")
        // Dismiss t2 manually
        coordinator.dismissTask("t2")
        runCurrent()

        assertNull(coordinator.activeTasks.value["t2"])
        assertEquals(2, coordinator.activeTasks.value.size)

        // Clear finished tasks (t1 is cancelled, so it should be cleared; t3 is queued, so it stays)
        coordinator.clearFinishedTasks()
        runCurrent()

        assertEquals(1, coordinator.activeTasks.value.size)
        assertNotNull(coordinator.activeTasks.value["t3"])

        coordinator.dismissTask("t3")
    }

    @Test
    fun testSseDisconnectRetriesWithBackoffAndSucceeds() = runTest {
        var attempts = 0
        val fakeApi = FakeApiClient()
        fakeApi.streamEventsHandler = { _, _ ->
            flow {
                attempts++
                if (attempts == 1) {
                    // First attempt disconnects prematurely
                    emit(
                        TaskProgressEvent(
                            task_id = "task_retry",
                            stage = "downloading",
                            percent = 20f
                        )
                    )
                    // stream ends without completion
                } else {
                    // Second attempt succeeds
                    emit(
                        TaskProgressEvent(
                            task_id = "task_retry",
                            stage = "completed",
                            completed = true,
                            percent = 100f
                        )
                    )
                }
            }
        }

        val coordinator = RipCoordinator(
            apiClient = fakeApi,
            coroutineScope = this,
            initialRetryDelayMs = 100L,
            autoCleanCompleted = false
        )

        val track = sampleTrack(trackId = "task_retry")
        fakeApi.createRipTaskHandler = { _, _, _, _ ->
            Result.success(RipTaskResponse(task_id = "task_retry", status = "queued"))
        }

        coordinator.ripTrack(track)
        runCurrent()

        // After attempt 1, backoff is 100ms
        assertEquals(1, attempts)
        val inProgressTask = coordinator.activeTasks.value["task_retry"]
        assertNotNull(inProgressTask)
        assertEquals(RipStage.DOWNLOADING, inProgressTask.stage)

        // Advance past initialRetryDelayMs (100ms)
        advanceTimeBy(150L)
        runCurrent()

        assertEquals(2, attempts)
        val completedTask = coordinator.activeTasks.value["task_retry"]
        assertNotNull(completedTask)
        assertEquals(RipStage.COMPLETED, completedTask.stage)
        assertTrue(completedTask.completed)
    }

    @Test
    fun testSseDisconnectRetriesExhaustedEmitsError() = runTest {
        var attempts = 0
        val fakeApi = FakeApiClient()
        fakeApi.streamEventsHandler = { _, _ ->
            flow {
                attempts++
                emit(
                    TaskProgressEvent(
                        task_id = "task_fail",
                        stage = "failed",
                        error = "Connection reset"
                    )
                )
            }
        }

        val coordinator = RipCoordinator(
            apiClient = fakeApi,
            coroutineScope = this,
            initialRetryDelayMs = 100L,
            maxSseRetries = 3
        )

        val track = sampleTrack(trackId = "task_fail")
        fakeApi.createRipTaskHandler = { _, _, _, _ ->
            Result.success(RipTaskResponse(task_id = "task_fail", status = "queued"))
        }

        coordinator.ripTrack(track)
        runCurrent()

        // Attempt 0 (initial) + 3 retries (100ms, 200ms, 400ms)
        advanceTimeBy(1000L)
        runCurrent()

        // Initial + 3 retries = 4 attempts total
        assertEquals(4, attempts)
        val failedTask = coordinator.activeTasks.value["task_fail"]
        assertNotNull(failedTask)
        assertEquals(RipStage.ERROR, failedTask.stage)
        assertEquals("Connection reset", failedTask.error)
        assertEquals(0, coordinator.activeCount.value)
    }
}
