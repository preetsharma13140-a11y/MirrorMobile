package com.mirror.app.features.watched

actual object WatchedClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}

