package com.mirror.app.features.watchprogress

expect object CurrentDateProvider {
    fun todayIsoDate(): String
    fun localStartOfDayEpochMs(isoDate: String): Long?
}
