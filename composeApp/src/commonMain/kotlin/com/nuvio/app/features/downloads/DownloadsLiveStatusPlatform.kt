package com.mirror.app.features.downloads

internal expect object DownloadsLiveStatusPlatform {
    fun onItemsChanged(items: List<DownloadItem>)
}
