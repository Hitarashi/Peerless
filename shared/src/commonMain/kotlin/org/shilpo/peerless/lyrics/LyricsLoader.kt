package org.shilpo.peerless.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.shilpo.peerless.model.LyricsResponse
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.PeerlessApiClient

sealed interface LyricsLoadState {
    data object Idle : LyricsLoadState
    data object Loading : LyricsLoadState
    data class Available(val lyrics: LyricsResponse) : LyricsLoadState
    data object NotFound : LyricsLoadState
    data class Error(val message: String?) : LyricsLoadState
}

/** Loads lyrics for the active track once and ignores responses from tracks that are no longer active. */
class LyricsLoader(
    private val apiClient: PeerlessApiClient,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<LyricsLoadState>(LyricsLoadState.Idle)
    val state: StateFlow<LyricsLoadState> = _state.asStateFlow()

    private var activeTrackId: Int? = null
    private var activeServerUrl: String? = null
    private var requestGeneration = 0L
    private var requestJob: Job? = null

    fun selectTrack(trackId: Int?, serverUrl: String = apiClient.baseUrl) {
        if (activeTrackId == trackId && activeServerUrl == serverUrl) return
        requestJob?.cancel()
        requestJob = null
        activeTrackId = trackId
        activeServerUrl = serverUrl
        requestGeneration += 1
        _state.value = LyricsLoadState.Idle
    }

    fun loadIfNeeded(track: TrackSummaryDto, serverUrl: String = apiClient.baseUrl) {
        if (activeTrackId != track.id || activeServerUrl != serverUrl) selectTrack(
            track.id,
            serverUrl
        )
        if (_state.value != LyricsLoadState.Idle) return

        if (track.id <= 0) {
            _state.value = LyricsLoadState.NotFound
            return
        }

        launchRequest(track, refresh = false)
    }

    fun retry(track: TrackSummaryDto, serverUrl: String = apiClient.baseUrl) {
        if (activeTrackId != track.id || activeServerUrl != serverUrl) selectTrack(
            track.id,
            serverUrl
        )
        if (track.id <= 0) {
            _state.value = LyricsLoadState.NotFound
            return
        }

        launchRequest(track, refresh = true)
    }

    private fun launchRequest(track: TrackSummaryDto, refresh: Boolean) {
        requestJob?.cancel()
        requestGeneration += 1
        val generation = requestGeneration
        _state.value = LyricsLoadState.Loading
        requestJob = scope.launch {
            val result = apiClient.getLyrics(track.id, refresh = refresh)
            if (generation != requestGeneration || activeTrackId != track.id) return@launch

            _state.value = result.fold(
                onSuccess = { response -> response.toLoadState(track) },
                onFailure = { error ->
                    if (error is CancellationException) {
                        LyricsLoadState.Idle
                    } else {
                        LyricsLoadState.Error(error.message)
                    }
                }
            )
        }
    }
}

private fun LyricsResponse.toLoadState(track: TrackSummaryDto): LyricsLoadState {
    val fallback = "${track.title.trim()} - ${track.artist.trim()}"
    val responseText = plain_text?.trim()
    val isServerFallback = format.equals("plain", ignoreCase = true) &&
            responseText.equals(fallback, ignoreCase = true) &&
            lines.size <= 1 &&
            lines.all { it.text.trim().equals(fallback, ignoreCase = true) }

    val hasLyrics = lines.any { it.text.isNotBlank() } || !responseText.isNullOrBlank()
    return if (isServerFallback || !hasLyrics) {
        LyricsLoadState.NotFound
    } else {
        LyricsLoadState.Available(this)
    }
}
