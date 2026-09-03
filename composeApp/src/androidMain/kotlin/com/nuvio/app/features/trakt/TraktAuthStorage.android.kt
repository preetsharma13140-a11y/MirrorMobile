package com.mirror.app.features.trakt

import android.content.Context
import android.content.SharedPreferences
import com.mirror.app.core.storage.ProfileScopedKey

internal actual object TraktAuthStorage {
    private const val preferencesName = "MIRROR_trakt_auth"
    private const val payloadKey = "trakt_auth_payload"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(profileId: Int): String? =
        preferences?.getString(ProfileScopedKey.of(payloadKey, profileId), null)

    actual fun savePayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(payloadKey, profileId), payload)
            ?.apply()
    }
}
