package com.mirror.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mirror.app.core.auth.AuthStorage
import com.mirror.app.core.diagnostics.SentryInitializer
import com.mirror.app.core.deeplink.handleAppUrl
import com.mirror.app.core.storage.PlatformLocalAccountDataCleaner
import com.mirror.app.core.sync.SyncClientIdentityStorage
import com.mirror.app.features.addons.AddonStorage
import com.mirror.app.features.collection.CollectionMobileSettingsStorage
import com.mirror.app.features.collection.CollectionStorage
import com.mirror.app.features.debrid.DebridSettingsStorage
import com.mirror.app.features.downloads.DownloadsLiveStatusPlatform
import com.mirror.app.features.downloads.DownloadsPlatformDownloader
import com.mirror.app.features.downloads.DownloadsStorage
import com.mirror.app.features.library.LibraryDisplaySettingsStorage
import com.mirror.app.features.library.LibraryStorage
import com.mirror.app.features.details.MetaScreenSettingsStorage
import com.mirror.app.features.home.HomeCatalogSettingsStorage
import com.mirror.app.features.mdblist.MdbListSettingsStorage
import com.mirror.app.features.notifications.EpisodeReleaseNotificationPlatform
import com.mirror.app.features.notifications.EpisodeReleaseNotificationsStorage
import com.mirror.app.features.player.PlayerSettingsStorage
import com.mirror.app.features.player.PlayerTrackPreferenceStorage
import com.mirror.app.features.player.ExternalPlayerPlatform
import com.mirror.app.features.player.SubtitleFileCache
import com.mirror.app.features.player.PlayerPictureInPictureManager
import com.mirror.app.features.player.PipRemoteActionReceiver
import com.mirror.app.features.p2p.P2pSettingsStorage
import com.mirror.app.features.p2p.P2pStreamingEngine
import com.mirror.app.features.plugins.PluginStorage
import com.mirror.app.features.profiles.AvatarStorage
import com.mirror.app.features.profiles.ProfilePinCacheStorage
import com.mirror.app.features.profiles.ProfileStorage
import com.mirror.app.features.details.SeasonViewModeStorage
import com.mirror.app.features.search.SearchHistoryStorage
import com.mirror.app.features.settings.SentrySettingsStorage
import com.mirror.app.features.settings.ThemeSettingsStorage
import com.mirror.app.features.trakt.TraktAuthStorage
import com.mirror.app.features.trakt.TraktCommentsStorage
import com.mirror.app.features.trakt.TraktLibraryStorage
import com.mirror.app.features.trakt.TraktSettingsStorage
import com.mirror.app.features.tmdb.TmdbSettingsStorage
import com.mirror.app.features.updater.AndroidAppUpdaterPlatform
import com.mirror.app.core.network.NetworkWarmup
import com.mirror.app.core.ui.CardDepthStyleStorage
import com.mirror.app.core.ui.PosterCardStyleStorage
import com.mirror.app.features.watched.WatchedStorage
import com.mirror.app.features.streams.StreamLinkCacheStorage
import com.mirror.app.features.streams.StreamBadgeSettingsStorage
import com.mirror.app.features.streams.BingeGroupCacheStorage
import com.mirror.app.features.watchprogress.ContinueWatchingEnrichmentStorage
import com.mirror.app.features.watchprogress.ContinueWatchingPreferencesStorage
import com.mirror.app.features.watchprogress.ResumePromptStorage
import com.mirror.app.features.watchprogress.WatchProgressStorage

class MainActivity : AppCompatActivity() {
    private var pipRemoteActionReceiver: PipRemoteActionReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(
                scrim = 0xFF020404.toInt(),
            ),
        )
        ThemeSettingsStorage.initialize(applicationContext)
        SentrySettingsStorage.initialize(applicationContext)
        SentryInitializer.start(application)
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.MIRROR_background)
        pipRemoteActionReceiver = PipRemoteActionReceiver.register(this)
        SyncClientIdentityStorage.initialize(applicationContext)
        AddonStorage.initialize(applicationContext)
        AuthStorage.initialize(applicationContext)
        LibraryStorage.initialize(applicationContext)
        WatchedStorage.initialize(applicationContext)
        MetaScreenSettingsStorage.initialize(applicationContext)
        HomeCatalogSettingsStorage.initialize(applicationContext)
        PlayerSettingsStorage.initialize(applicationContext)
        PlayerTrackPreferenceStorage.initialize(applicationContext)
        P2pSettingsStorage.initialize(applicationContext)
        P2pStreamingEngine.initialize(applicationContext)
        ExternalPlayerPlatform.initialize(applicationContext)
        SubtitleFileCache.initialize(applicationContext)
        ProfileStorage.initialize(applicationContext)
        AvatarStorage.initialize(applicationContext)
        ProfilePinCacheStorage.initialize(applicationContext)
        SearchHistoryStorage.initialize(applicationContext)
        SeasonViewModeStorage.initialize(applicationContext)
        PosterCardStyleStorage.initialize(applicationContext)
        CardDepthStyleStorage.initialize(applicationContext)
        DebridSettingsStorage.initialize(applicationContext)
        TmdbSettingsStorage.initialize(applicationContext)
        MdbListSettingsStorage.initialize(applicationContext)
        TraktAuthStorage.initialize(applicationContext)
        TraktCommentsStorage.initialize(applicationContext)
        TraktLibraryStorage.initialize(applicationContext)
        TraktSettingsStorage.initialize(applicationContext)
        LibraryDisplaySettingsStorage.initialize(applicationContext)
        ContinueWatchingPreferencesStorage.initialize(applicationContext)
        ResumePromptStorage.initialize(applicationContext)
        ContinueWatchingEnrichmentStorage.initialize(applicationContext)
        EpisodeReleaseNotificationsStorage.initialize(applicationContext)
        WatchProgressStorage.initialize(applicationContext)
        StreamLinkCacheStorage.initialize(applicationContext)
        StreamBadgeSettingsStorage.initialize(applicationContext)
        BingeGroupCacheStorage.initialize(applicationContext)
        PluginStorage.initialize(applicationContext)
        CollectionMobileSettingsStorage.initialize(applicationContext)
        CollectionStorage.initialize(applicationContext)
        DownloadsStorage.initialize(applicationContext)
        DownloadsPlatformDownloader.initialize(applicationContext)
        DownloadsLiveStatusPlatform.initialize(applicationContext)
        AndroidAppUpdaterPlatform.initialize(applicationContext)
        NetworkWarmup.warmUp()
        PlatformLocalAccountDataCleaner.initialize(applicationContext)
        EpisodeReleaseNotificationPlatform.initialize(applicationContext)
        EpisodeReleaseNotificationPlatform.bindActivity(this)
        handleIncomingAppIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingAppIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        PlayerPictureInPictureManager.onUserLeaveHint(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PlayerPictureInPictureManager.onPictureInPictureModeChanged(this, isInPictureInPictureMode)
    }

    override fun onDestroy() {
        EpisodeReleaseNotificationPlatform.unbindActivity(this)
        val receiver = pipRemoteActionReceiver
        if (receiver != null) {
            runCatching { unregisterReceiver(receiver) }
            pipRemoteActionReceiver = null
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (EpisodeReleaseNotificationPlatform.handlePermissionRequestResult(requestCode, grantResults)) {
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun handleIncomingAppIntent(intent: Intent?) {
        val appUrl = intent?.dataString?.trim().orEmpty()
        if (appUrl.isBlank()) return
        handleAppUrl(appUrl)
    }
}
