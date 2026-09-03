package com.mirror.app.features.watchprogress

internal expect object WatchProgressClock {
    fun nowEpochMs(): Long
}
