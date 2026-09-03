package com.mirror.app.core.sync

import kotlinx.coroutines.flow.Flow

internal expect object AppForegroundMonitor {
    fun events(): Flow<Unit>
}
