package com.mirror.app.features.downloads

internal expect object DownloadsClock {
    fun nowEpochMs(): Long
}
