package com.mirror.app.features.downloads

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadsNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val downloadId = intent.getStringExtra(extraDownloadId)?.trim().orEmpty()
        if (downloadId.isBlank()) return

        DownloadsStorage.initialize(context.applicationContext)
        DownloadsPlatformDownloader.initialize(context.applicationContext)
        DownloadsLiveStatusPlatform.initialize(context.applicationContext)
        DownloadsRepository.ensureLoaded()

        when (action) {
            actionPause -> DownloadsRepository.pauseDownload(downloadId)
            actionResume -> DownloadsRepository.resumeDownload(downloadId)
        }
    }

    companion object {
        const val actionPause = "com.mirror.app.downloads.action.PAUSE"
        const val actionResume = "com.mirror.app.downloads.action.RESUME"
        const val extraDownloadId = "download_id"
    }
}
