package com.mirror.app.core.network

import co.touchlab.kermit.Logger
import com.mirror.app.core.diagnostics.SentryNetworkBreadcrumbInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Pre-warms TCP+TLS connections to the key API hosts used by the app.
 * Called once on app start so the first movie detail open is instant.
 */
object NetworkWarmup {
    private val log = Logger.withTag("NetworkWarmup")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Lightweight client just for warmup — short timeouts, no retries
    // Does NOT force NO_PROXY so it respects the system proxy on restrictive networks
    private val warmupClient = OkHttpClient.Builder()
        .dns(IPv4FirstDns())
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false)
        .retryOnConnectionFailure(true)
        .addInterceptor(SentryNetworkBreadcrumbInterceptor())
        .build()

    // Hosts to warm up — HEAD request establishes TCP+TLS but downloads nothing
    private val warmupUrls = listOf(
        "https://api.themoviedb.org",          // TMDB metadata (movie details)
        "https://image.tmdb.org",              // TMDB images
        "https://api.trakt.tv",                // Trakt ratings/comments
    )

    fun warmUp() {
        scope.launch {
            warmupUrls.forEach { url ->
                launch {
                    runCatching {
                        val request = Request.Builder()
                            .url(url)
                            .head()
                            .build()
                        warmupClient.newCall(request).execute().use { /* just establish the connection */ }
                        log.d { "Warmed up: $url" }
                    }.onFailure { e ->
                        log.d { "Warmup skipped for $url: ${e.message}" }
                    }
                }
            }
        }
    }
}
