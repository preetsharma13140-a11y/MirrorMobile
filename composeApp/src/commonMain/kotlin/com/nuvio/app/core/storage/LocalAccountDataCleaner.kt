package com.mirror.app.core.storage

import com.mirror.app.core.build.AppFeaturePolicy
import com.mirror.app.core.sync.SyncManager
import com.mirror.app.core.sync.ProfileSettingsSync
import com.mirror.app.features.addons.AddonRepository
import com.mirror.app.features.catalog.CatalogRepository
import com.mirror.app.features.collection.CollectionMobileSettingsRepository
import com.mirror.app.features.collection.CollectionRepository
import com.mirror.app.features.details.MetaDetailsRepository
import com.mirror.app.features.details.MetaScreenSettingsRepository
import com.mirror.app.features.home.HomeCatalogSettingsRepository
import com.mirror.app.features.home.HomeRepository
import com.mirror.app.features.library.LibraryRepository
import com.mirror.app.features.library.LibraryDisplaySettingsRepository
import com.mirror.app.features.notifications.EpisodeReleaseNotificationsRepository
import com.mirror.app.features.player.PlayerLaunchStore
import com.mirror.app.features.player.PlayerSettingsRepository
import com.mirror.app.features.p2p.P2pSettingsRepository
import com.mirror.app.features.plugins.PluginRepository
import com.mirror.app.features.player.SubtitleRepository
import com.mirror.app.features.profiles.ProfileRepository
import com.mirror.app.features.search.SearchRepository
import com.mirror.app.features.settings.ThemeSettingsRepository
import com.mirror.app.features.streams.StreamContextStore
import com.mirror.app.features.streams.StreamBadgeSettingsRepository
import com.mirror.app.features.streams.StreamLaunchStore
import com.mirror.app.features.streams.StreamsRepository
import com.mirror.app.features.trakt.TraktAuthRepository
import com.mirror.app.features.trakt.TraktSettingsRepository
import com.mirror.app.core.ui.CardDepthStyleRepository
import com.mirror.app.core.ui.PosterCardStyleRepository
import com.mirror.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.mirror.app.features.watchprogress.ContinueWatchingEnrichmentCache
import com.mirror.app.features.watchprogress.WatchProgressRepository
import com.mirror.app.features.watchprogress.WatchProgressSourceCoordinator
import com.mirror.app.features.watched.WatchedRepository

internal object LocalAccountDataCleaner {
    fun wipe() {
        SyncManager.cancelAccountSync()
        WatchProgressSourceCoordinator.clearLocalState()
        ProfileSettingsSync.clearAccountState()
        ContinueWatchingEnrichmentCache.clearLocalState()
        WatchProgressRepository.clearLocalState()
        WatchedRepository.clearLocalState()
        LibraryRepository.runAccountStorageWipe {
            PlatformLocalAccountDataCleaner.wipe()
        }

        ProfileRepository.clearInMemory()
        AddonRepository.clearLocalState()
        if (AppFeaturePolicy.pluginsEnabled) {
            PluginRepository.clearLocalState()
        }
        HomeRepository.clear()
        HomeCatalogSettingsRepository.clearLocalState()
        MetaScreenSettingsRepository.clearLocalState()
        LibraryRepository.clearLocalState()
        LibraryDisplaySettingsRepository.clearLocalState()
        ContinueWatchingPreferencesRepository.clearLocalState()
        EpisodeReleaseNotificationsRepository.clearLocalState()
        CollectionMobileSettingsRepository.clearLocalState()
        CollectionRepository.clearLocalState()
        ThemeSettingsRepository.clearLocalState()
        PosterCardStyleRepository.clearLocalState()
        CardDepthStyleRepository.clearLocalState()
        TraktAuthRepository.clearLocalState()
        TraktSettingsRepository.clearLocalState()
        PlayerSettingsRepository.clearLocalState()
        StreamBadgeSettingsRepository.clearLocalState()
        P2pSettingsRepository.clearLocalState()
        CatalogRepository.clear()
        StreamsRepository.clear()
        MetaDetailsRepository.clear()
        SearchRepository.reset()
        SubtitleRepository.clear()
        PlayerLaunchStore.clear()
        StreamLaunchStore.clear()
        StreamContextStore.clear()
    }
}

internal expect object PlatformLocalAccountDataCleaner {
    fun wipe()
}
