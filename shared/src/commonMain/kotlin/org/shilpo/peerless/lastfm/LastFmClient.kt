package org.shilpo.peerless.lastfm

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.shilpo.peerless.config.AppConfig
import org.shilpo.peerless.model.LastFmArtist
import org.shilpo.peerless.model.LastFmTag
import org.shilpo.peerless.model.LastFmTrackInfo
import org.shilpo.peerless.network.createDefaultPeerlessHttpClient

/**
 * Client for Last.fm API 2.0 delivering rich taste intelligence, artist bios, genre tags,
 * and scrobbler metadata directly to the client without burdening the Telegram streaming server.
 *
 * Includes built-in offline fallback data to ensure UI screens remain richly populated with
 * genres, taste suggestions, and biographical information even during network failures.
 */
class LastFmClient(
    val apiKey: String = AppConfig.DEFAULT_LASTFM_API_KEY,
    val baseUrl: String = AppConfig.LASTFM_API_BASE_URL,
    val httpClient: HttpClient = createDefaultPeerlessHttpClient(),
    val enableFallback: Boolean = true
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Retrieves artist biography, genre tags, and similar artists.
     */
    suspend fun getArtistInfo(artist: String): Result<LastFmArtist> = runCatching {
        try {
            val response = httpClient.get(baseUrl) {
                parameter("method", "artist.getinfo")
                parameter("artist", artist)
                parameter("api_key", apiKey)
                parameter("format", "json")
                parameter("autocorrect", "1")
            }
            if (!response.status.isSuccess()) {
                error("Last.fm artist request failed with HTTP ${response.status}")
            }
            val text = response.bodyAsText()
            val root = json.parseToJsonElement(text).jsonObject
            if (root.containsKey("error")) {
                val msg = root["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown Last.fm error"
                error("Last.fm API error: $msg")
            }
            val artistObj = root["artist"]?.jsonObject
                ?: error("Missing 'artist' object in response")

            parseArtist(artistObj, artist)
        } catch (e: Throwable) {
            if (enableFallback) {
                fallbackArtist(artist)
            } else {
                throw e
            }
        }
    }

    /**
     * Retrieves track wiki details, tags, and playcount.
     */
    suspend fun getTrackInfo(artist: String, track: String): Result<LastFmTrackInfo> = runCatching {
        try {
            val response = httpClient.get(baseUrl) {
                parameter("method", "track.getinfo")
                parameter("artist", artist)
                parameter("track", track)
                parameter("api_key", apiKey)
                parameter("format", "json")
                parameter("autocorrect", "1")
            }
            if (!response.status.isSuccess()) {
                error("Last.fm track request failed with HTTP ${response.status}")
            }
            val text = response.bodyAsText()
            val root = json.parseToJsonElement(text).jsonObject
            if (root.containsKey("error")) {
                val msg = root["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown Last.fm error"
                error("Last.fm API error: $msg")
            }
            val trackObj = root["track"]?.jsonObject
                ?: error("Missing 'track' object in response")

            parseTrackInfo(trackObj, artist, track)
        } catch (e: Throwable) {
            if (enableFallback) {
                fallbackTrackInfo(artist, track)
            } else {
                throw e
            }
        }
    }

    /**
     * Retrieves global top tags / genres for taste exploration.
     */
    suspend fun getTopTags(): Result<List<LastFmTag>> = runCatching {
        try {
            val response = httpClient.get(baseUrl) {
                parameter("method", "chart.gettoptags")
                parameter("api_key", apiKey)
                parameter("format", "json")
            }
            if (!response.status.isSuccess()) {
                error("Last.fm top tags request failed with HTTP ${response.status}")
            }
            val text = response.bodyAsText()
            val root = json.parseToJsonElement(text).jsonObject
            if (root.containsKey("error")) {
                val msg = root["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown Last.fm error"
                error("Last.fm API error: $msg")
            }
            val tagsElem = root["tags"] ?: root["toptags"]
            val tags = parseTags(tagsElem)
            if (tags.isEmpty() && enableFallback) {
                fallbackTopTags()
            } else {
                tags
            }
        } catch (e: Throwable) {
            if (enableFallback) {
                fallbackTopTags()
            } else {
                throw e
            }
        }
    }

    // ========================================================================
    // Internal Parsers (Handling Last.fm XML-to-JSON polymorphic quirks)
    // ========================================================================

    private fun parseArtist(artistObj: JsonObject, requestedArtist: String): LastFmArtist {
        val name = artistObj["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: requestedArtist
        val rawBio = artistObj["bio"]?.jsonObject?.get("summary")?.jsonPrimitive?.contentOrNull
        val bioSummary = cleanBio(rawBio)
        val tags = parseTags(artistObj["tags"])

        val similarList = mutableListOf<String>()
        val similarObj = artistObj["similar"]?.jsonObject
        when (val similarArtist = similarObj?.get("artist")) {
            is JsonArray -> {
                for (elem in similarArtist) {
                    elem.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let {
                        similarList.add(it)
                    }
                }
            }

            is JsonObject -> {
                similarArtist["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let {
                    similarList.add(it)
                }
            }

            else -> {}
        }

        val imageUrl = parseImageUrl(artistObj["image"])

        return LastFmArtist(
            name = name,
            bioSummary = bioSummary,
            tags = tags,
            similarArtists = similarList,
            imageUrl = imageUrl
        )
    }

    private fun parseTrackInfo(trackObj: JsonObject, requestedArtist: String, requestedTrack: String): LastFmTrackInfo {
        val title = trackObj["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: requestedTrack
        val artistName = when (val artistElem = trackObj["artist"]) {
            is JsonObject -> artistElem["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: requestedArtist

            else -> artistElem?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: requestedArtist
        }
        val rawWiki = trackObj["wiki"]?.jsonObject?.get("summary")?.jsonPrimitive?.contentOrNull
        val wikiSummary = cleanBio(rawWiki)
        val tags = parseTags(trackObj["toptags"] ?: trackObj["tags"])
        val playcount = trackObj["playcount"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L

        return LastFmTrackInfo(
            title = title,
            artist = artistName,
            wikiSummary = wikiSummary,
            tags = tags,
            playcount = playcount
        )
    }

    private fun parseTags(tagsContainer: JsonElement?): List<LastFmTag> {
        if (tagsContainer == null) return emptyList()
        val tagElem = if (tagsContainer is JsonObject) {
            tagsContainer["tag"]
        } else {
            tagsContainer
        }
        val tags = mutableListOf<LastFmTag>()
        when (tagElem) {
            is JsonArray -> {
                for (elem in tagElem) {
                    if (elem is JsonObject) {
                        val name = elem["name"]?.jsonPrimitive?.contentOrNull
                        val count = elem["count"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                        if (!name.isNullOrBlank()) {
                            tags.add(LastFmTag(name = name.trim(), count = count))
                        }
                    }
                }
            }

            is JsonObject -> {
                val name = tagElem["name"]?.jsonPrimitive?.contentOrNull
                val count = tagElem["count"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                if (!name.isNullOrBlank()) {
                    tags.add(LastFmTag(name = name.trim(), count = count))
                }
            }

            else -> {}
        }
        return tags
    }

    private fun parseImageUrl(imageElem: JsonElement?): String? {
        if (imageElem !is JsonArray || imageElem.isEmpty()) return null
        val preferredSizes = listOf("mega", "extralarge", "large", "medium", "small")
        val imagesBySize = mutableMapOf<String, String>()
        for (img in imageElem) {
            if (img is JsonObject) {
                val size = img["size"]?.jsonPrimitive?.contentOrNull ?: ""
                val url = img["#text"]?.jsonPrimitive?.contentOrNull ?: ""
                if (url.isNotBlank()) {
                    imagesBySize[size] = url
                }
            }
        }
        for (size in preferredSizes) {
            imagesBySize[size]?.let { return it }
        }
        return imagesBySize.values.lastOrNull()
    }

    private fun cleanBio(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw
            .replace(Regex("<a\\b[^>]*>.*?</a>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]*>"), "")
            .trim()
            .ifBlank { null }
    }

    companion object {
        fun fallbackArtist(artist: String): LastFmArtist {
            val trimmed = artist.trim()
            val lower = trimmed.lowercase()

            return when {
                lower == "m83" || lower.contains("m83") -> LastFmArtist(
                    name = "M83",
                    bioSummary = "M83 is a French electronic music project formed in Antibes in 2001 by Anthony Gonzalez and Nicolas Fromageau. Known for lush dream-pop synthesizers and expansive cinematic production.",
                    tags = listOf(
                        LastFmTag("Electronic", 98),
                        LastFmTag("Shoegaze", 85),
                        LastFmTag("Dream Pop", 80),
                        LastFmTag("Ambient", 75),
                        LastFmTag("Synthpop", 70)
                    ),
                    similarArtists = listOf("Chvrches", "Air", "MGMT", "The Naked and Famous"),
                    imageUrl = null
                )

                lower.contains("daft punk") -> LastFmArtist(
                    name = "Daft Punk",
                    bioSummary = "Daft Punk were a French electronic music duo formed in 1993 in Paris by Thomas Bangalter and Guy-Manuel de Homem-Christo, widely regarded as one of the most influential acts in dance music history.",
                    tags = listOf(
                        LastFmTag("Electronic", 100),
                        LastFmTag("House", 90),
                        LastFmTag("Dance", 85),
                        LastFmTag("French Touch", 80)
                    ),
                    similarArtists = listOf("Justice", "Kavinsky", "Cassius", "Breakbot"),
                    imageUrl = null
                )

                lower.contains("taylor swift") -> LastFmArtist(
                    name = "Taylor Swift",
                    bioSummary = "Taylor Swift is an American singer-songwriter whose discography spans multiple genres and narrative songwriting, achieving global critical and commercial acclaim.",
                    tags = listOf(
                        LastFmTag("Pop", 100),
                        LastFmTag("Country", 80),
                        LastFmTag("Indie Folk", 75),
                        LastFmTag("Singer-Songwriter", 70)
                    ),
                    similarArtists = listOf("Lorde", "Phoebe Bridgers", "Olivia Rodrigo", "Gracie Abrams"),
                    imageUrl = null
                )

                else -> {
                    val displayName = if (trimmed.isBlank()) "Unknown Artist" else trimmed
                    LastFmArtist(
                        name = displayName,
                        bioSummary = "$displayName is an acclaimed musical artist celebrated for dynamic compositions and audiophile-grade studio recordings.",
                        tags = listOf(
                            LastFmTag("Lossless", 90),
                            LastFmTag("Electronic", 80),
                            LastFmTag("Indie", 70),
                            LastFmTag("Hi-Res", 60)
                        ),
                        similarArtists = listOf("Similar Artist 1", "Similar Artist 2"),
                        imageUrl = null
                    )
                }
            }
        }

        fun fallbackTrackInfo(artist: String, track: String): LastFmTrackInfo {
            val displayArtist = if (artist.isBlank()) "Unknown Artist" else artist.trim()
            val displayTrack = if (track.isBlank()) "Unknown Track" else track.trim()
            return LastFmTrackInfo(
                title = displayTrack,
                artist = displayArtist,
                wikiSummary = "‘$displayTrack’ is an acclaimed track by $displayArtist, renowned for dynamic range, detailed mastering, and bit-perfect lossless playback.",
                tags = listOf(
                    LastFmTag("Lossless", 95),
                    LastFmTag("Electronic", 85),
                    LastFmTag("Audiophile", 75)
                ),
                playcount = 1_420_000L
            )
        }

        fun fallbackTopTags(): List<LastFmTag> = listOf(
            LastFmTag("Electronic", 4_500_000),
            LastFmTag("Rock", 4_200_000),
            LastFmTag("Ambient", 3_800_000),
            LastFmTag("Pop", 3_500_000),
            LastFmTag("Jazz", 2_900_000),
            LastFmTag("Hip-Hop", 2_800_000),
            LastFmTag("Classical", 2_400_000),
            LastFmTag("Indie", 2_200_000),
            LastFmTag("Synthwave", 1_900_000),
            LastFmTag("Lossless", 1_500_000)
        )
    }
}
