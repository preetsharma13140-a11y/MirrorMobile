package com.mirror.app.features.library

internal expect object LibraryClock {
    fun nowEpochMs(): Long
}
