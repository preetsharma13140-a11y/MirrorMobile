package com.mirror.app.features.profiles

import co.touchlab.kermit.Logger
import com.mirror.app.core.network.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class StoredAvatarCatalogPayload(
    val items: List<AvatarCatalogItem> = emptyList(),
)

object AvatarRepository {
    private val log = Logger.withTag("AvatarRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val timeSource = TimeSource.Monotonic

    private val _avatars = MutableStateFlow<List<AvatarCatalogItem>>(emptyList())
    val avatars: StateFlow<List<AvatarCatalogItem>> = _avatars.asStateFlow()

    private var loaded = false
    private var cacheHydrated = false
    private var fetchInFlight = false
    // Monotonic mark of the last successful network fetch; null means never fetched
    private var lastFetchMark: TimeSource.Monotonic.ValueTimeMark? = null
    private val REFRESH_COOLDOWN = 5.minutes

    suspend fun fetchAvatars() {
        hydrateFromCacheIfNeeded()
        if (loaded && _avatars.value.isNotEmpty()) return
        doFetch()
    }

    suspend fun refreshAvatars() {
        hydrateFromCacheIfNeeded()
        // Skip redundant network refresh if we fetched recently and already have data
        val mark = lastFetchMark
        if (_avatars.value.isNotEmpty() && mark != null && mark.elapsedNow() < REFRESH_COOLDOWN) return
        doFetch()
    }

    private fun hydrateFromCacheIfNeeded() {
        if (cacheHydrated) return
        cacheHydrated = true

        val payload = AvatarStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) return

        val stored = runCatching {
            json.decodeFromString<StoredAvatarCatalogPayload>(payload)
        }.getOrNull() ?: return

        val items = stored.items
            .filter { it.isActive }
            .sortedWith(compareBy({ it.category }, { it.sortOrder }))
        if (items.isEmpty()) return

        _avatars.value = items
        loaded = true
    }

    private suspend fun doFetch() {
        if (fetchInFlight) return
        fetchInFlight = true
        runCatching {
            val result = SupabaseProvider.client.postgrest.rpc("get_avatar_catalog")
            val items = result.decodeList<AvatarCatalogItem>()
            val activeItems = items.filter { it.isActive }.sortedWith(
                compareBy({ it.category }, { it.sortOrder }),
            )
            _avatars.value = activeItems
            loaded = true
            lastFetchMark = timeSource.markNow()
            AvatarStorage.savePayload(
                json.encodeToString(
                    StoredAvatarCatalogPayload(items = activeItems),
                ),
            )
        }.onFailure { e ->
            log.e(e) { "Failed to fetch avatar catalog" }
        }.also {
            fetchInFlight = false
        }
    }
}
