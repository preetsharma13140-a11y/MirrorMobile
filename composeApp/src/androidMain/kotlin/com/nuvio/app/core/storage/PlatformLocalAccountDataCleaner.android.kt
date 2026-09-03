package com.mirror.app.core.storage

import android.content.Context

internal actual object PlatformLocalAccountDataCleaner {
    private val preferenceNames = listOf(
        "MIRROR_addons",
        "MIRROR_library",
        "MIRROR_library_display_settings",
        "MIRROR_home_catalog_settings",
        "MIRROR_player_settings",
        "torrent_settings",
        "MIRROR_profile_cache",
        "MIRROR_avatar_cache",
        "MIRROR_profile_pin_cache",
        "MIRROR_theme_settings",
        "MIRROR_poster_card_style",
        "MIRROR_debrid_settings",
        "MIRROR_mdblist_settings",
        "MIRROR_auth",
        "MIRROR_trakt_auth",
        "MIRROR_trakt_library",
        "MIRROR_trakt_settings",
        "MIRROR_watched",
        "MIRROR_stream_link_cache",
        "MIRROR_stream_badge_settings",
        "MIRROR_continue_watching_preferences",
        "MIRROR_cw_enrichment",
        "MIRROR_episode_release_notifications",
        "MIRROR_episode_release_notifications_platform",
        "MIRROR_watch_progress",
        "MIRROR_collection_mobile_settings",
        "MIRROR_collections",
        "MIRROR_plugins",
    )

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun wipe() {
        val context = appContext ?: return
        preferenceNames.forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }
}
