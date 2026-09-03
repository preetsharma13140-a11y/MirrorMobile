package com.mirror.app.features.profiles

internal expect object AvatarStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}