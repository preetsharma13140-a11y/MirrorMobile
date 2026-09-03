package com.mirror.app.features.details

internal expect object MetaScreenSettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}