package com.mirror.app.features.tmdb

import co.touchlab.kermit.Logger
import com.mirror.app.core.network.SupabaseConfig
import com.mirror.app.features.addons.httpGetText
import com.mirror.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object TmdbService {
    private val log = Logger.withTag("TmdbService")
    private val json = Json { ignoreUnknownKeys = true }
    private val imdbToTmdbCache = linkedMapOf<String, String>()
    private val tmdbToImdbCache = linkedMapOf<String, String>()
    private val cacheMutex = Mutex()

    /** Warm up TMDB API connection on app start to reduce first-request latency */
    suspend fun warmup() {
        val apiKey = currentApiKey() ?: return
        try {
            // Make a lightweight request to warm up the connection
            fetch<TmdbExternalIdsResponse>(endpoint = "movie/550/external_ids", apiKey = apiKey)
            log.d { "TMDB API warmed up successfully" }
        } catch (e: Throwable) {
            log.w { "TMDB warmup failed (non-critical): ${e.message}" }
        }
    }

    suspend fun ensureTmdbId(videoId: String, mediaType: String): String? {
        val apiKey = currentApiKey() ?: return null

        val normalized = videoId
            .removePrefix("tmdb:")
            .removePrefix("movie:")
            .removePrefix("series:")
            .substringBefore(':')
            .substringBefore('/')
            .trim()

        if (normalized.isBlank()) return null
        if (normalized.all(Char::isDigit)) return normalized
        if (!normalized.startsWith("tt", ignoreCase = true)) return null

        return imdbToTmdb(imdbId = normalized, mediaType = mediaType, apiKey = apiKey)
    }

    suspend fun tmdbToImdb(tmdbId: Int, mediaType: String): String? {
        val apiKey = currentApiKey() ?: return null

        val cacheKey = "$tmdbId:${normalizeMediaType(mediaType)}"
        cacheMutex.withLock {
            tmdbToImdbCache[cacheKey]?.let { return it }
        }

        val endpoint = when (normalizeMediaType(mediaType)) {
            "tv" -> "tv/$tmdbId/external_ids"
            else -> "movie/$tmdbId/external_ids"
        }
        val body = fetch<TmdbExternalIdsResponse>(endpoint = endpoint, apiKey = apiKey) ?: return null
        val imdbId = body.imdbId?.trim()?.takeIf(String::isNotBlank) ?: return null

        cacheMutex.withLock {
            tmdbToImdbCache[cacheKey] = imdbId
            imdbToTmdbCache["$imdbId:${normalizeMediaType(mediaType)}"] = tmdbId.toString()
        }
        return imdbId
    }

    private suspend fun imdbToTmdb(imdbId: String, mediaType: String, apiKey: String): String? {
        val normalizedType = normalizeMediaType(mediaType)
        val cacheKey = "$imdbId:$normalizedType"
        cacheMutex.withLock {
            imdbToTmdbCache[cacheKey]?.let { return it }
        }

        val body = fetch<TmdbFindResponse>(
            endpoint = "find/$imdbId",
            apiKey = apiKey,
            query = mapOf("external_source" to "imdb_id"),
        ) ?: return null

        val resultId = when (normalizedType) {
            "movie" -> body.movieResults.firstOrNull()?.id
            "tv" -> body.tvResults.firstOrNull()?.id
            else -> body.movieResults.firstOrNull()?.id ?: body.tvResults.firstOrNull()?.id
        }?.takeIf { it > 0 }?.toString()

        if (resultId != null) {
            cacheMutex.withLock {
                imdbToTmdbCache[cacheKey] = resultId
                tmdbToImdbCache["$resultId:$normalizedType"] = imdbId
            }
        } else {
            log.d { "No TMDB ID found for $imdbId ($normalizedType)" }
        }

        return resultId
    }

    private suspend inline fun <reified T> fetch(
        endpoint: String,
        apiKey: String,
        query: Map<String, String> = emptyMap(),
    ): T? {
        val url = buildTmdbUrl(endpoint = endpoint, apiKey = apiKey, query = query)
        // Always send the Supabase anon key so the edge function is not open to the public
        val headers = mapOf("Authorization" to "Bearer ${SupabaseConfig.ANON_KEY}")
        return runCatching {
            json.decodeFromString<T>(httpGetTextWithHeaders(url, headers))
        }.onFailure { error ->
            log.w { "TMDB proxy request failed for $endpoint: ${error.message}" }
        }.getOrNull()
    }

    private fun currentApiKey(): String? =
        TmdbSettingsRepository.snapshot().apiKey.trim().takeIf(String::isNotBlank)

    internal fun normalizeMediaType(mediaType: String): String =
        when (mediaType.trim().lowercase()) {
            "movie", "film" -> "movie"
            "tv", "series", "show", "tvshow" -> "tv"
            else -> mediaType.trim().lowercase()
        }
}

internal fun buildTmdbUrl(
    endpoint: String,
    apiKey: String,
    query: Map<String, String> = emptyMap(),
): String {
    // Route through Supabase Edge Function proxy to avoid rate limits.
    // The proxy holds the TMDB key server-side — no key in the APK.
    val proxyBase = "${SupabaseConfig.URL}/functions/v1/tmdb-proxy"
    val sb = StringBuilder(proxyBase)
    sb.append("?endpoint=")
    sb.append(endpoint.removePrefix("/").encodeURLParam())
    query.forEach { (key, value) ->
        if (value.isNotBlank()) {
            sb.append("&")
            sb.append(key.encodeURLParam())
            sb.append("=")
            sb.append(value.encodeURLParam())
        }
    }
    return sb.toString()
}

/** Simple percent-encoding for URL query parameter values. */
private fun String.encodeURLParam(): String = buildString {
    for (c in this@encodeURLParam) {
        when {
            c.isLetterOrDigit() || c in "-._~" -> append(c)
            else -> append('%').append(c.code.toString(16).uppercase().padStart(2, '0'))
        }
    }
}

/** Returns the Authorization header value for the given key, or null since proxy handles it. */
internal fun tmdbAuthHeader(apiKey: String): String? = null // Proxy handles auth now

@Serializable
private data class TmdbFindResponse(
    @SerialName("movie_results") val movieResults: List<TmdbExternalResult> = emptyList(),
    @SerialName("tv_results") val tvResults: List<TmdbExternalResult> = emptyList(),
)

@Serializable
private data class TmdbExternalResult(
    val id: Int,
)

@Serializable
private data class TmdbExternalIdsResponse(
    @SerialName("imdb_id") val imdbId: String? = null,
)
