package com.mirror.app.features.search

import com.mirror.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object SearchHistoryStorage {
    private const val payloadKey = "search_history_payload"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }
}
