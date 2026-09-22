package org.shilpo.peerless.library

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.shilpo.peerless.model.TrackSummaryDto
import org.shilpo.peerless.network.PeerlessApiClient

interface FavoritesManager {
    val favorites: StateFlow<List<TrackSummaryDto>>
    val favoriteIds: StateFlow<Set<Int>>
    val isLoading: StateFlow<Boolean>

    fun isFavorite(trackId: Int): Boolean
    suspend fun refreshFavorites()
    fun toggleFavorite(track: TrackSummaryDto)
}

class RealFavoritesManager(
    private val apiClient: PeerlessApiClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : FavoritesManager {

    private val _favorites = MutableStateFlow<List<TrackSummaryDto>>(emptyList())
    override val favorites: StateFlow<List<TrackSummaryDto>> = _favorites.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    override val favoriteIds: StateFlow<Set<Int>> = _favoriteIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    override fun isFavorite(trackId: Int): Boolean {
        return _favoriteIds.value.contains(trackId)
    }

    override suspend fun refreshFavorites() {
        _isLoading.value = true
        apiClient.getFavorites().onSuccess { list ->
            _favorites.value = list
            _favoriteIds.value = list.map { it.id }.toSet()
            _isLoading.value = false
        }.onFailure {
            _isLoading.value = false
        }
    }

    override fun toggleFavorite(track: TrackSummaryDto) {
        val currentlyFavorite = isFavorite(track.id)
        if (currentlyFavorite) {
            _favoriteIds.value = _favoriteIds.value - track.id
            _favorites.value = _favorites.value.filter { it.id != track.id }
            scope.launch {
                val res = apiClient.removeFavorite(track.id)
                if (res.isFailure) {
                    _favoriteIds.value = _favoriteIds.value + track.id
                    _favorites.value = _favorites.value + track
                }
            }
        } else {
            _favoriteIds.value = _favoriteIds.value + track.id
            _favorites.value = listOf(track) + _favorites.value.filter { it.id != track.id }
            scope.launch {
                val res = apiClient.addFavorite(track.id)
                if (res.isFailure) {
                    _favoriteIds.value = _favoriteIds.value - track.id
                    _favorites.value = _favorites.value.filter { it.id != track.id }
                }
            }
        }
    }
}

val LocalFavoritesManager = staticCompositionLocalOf<FavoritesManager?> { null }
