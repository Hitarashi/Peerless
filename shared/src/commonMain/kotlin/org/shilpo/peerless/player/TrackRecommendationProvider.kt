package org.shilpo.peerless.player

import org.shilpo.peerless.model.Track

fun interface TrackRecommendationProvider {
    suspend fun recommend(
        seed: Track,
        excludedTrackKeys: Set<String>,
        limit: Int
    ): List<Track>
}

fun recommendationTrackKey(track: Track): String = recommendationTrackKey(track.artist, track.title)

fun recommendationTrackKey(artist: String, title: String): String =
    "${normalizeRecommendationText(artist)}|${normalizeRecommendationText(title)}"

private fun normalizeRecommendationText(value: String): String = buildString {
    value.lowercase().forEach { character ->
        append(if (character.isLetterOrDigit()) character else ' ')
    }
}.trim().replace(Regex("\\s+"), " ")
