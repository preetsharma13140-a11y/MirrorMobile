package com.mirror.app.features.trakt

import com.mirror.app.core.time.parseZonedIsoDateTimeToEpochMs

internal fun parseTraktIsoDateTimeToEpochMs(value: String): Long? =
    parseZonedIsoDateTimeToEpochMs(value)
