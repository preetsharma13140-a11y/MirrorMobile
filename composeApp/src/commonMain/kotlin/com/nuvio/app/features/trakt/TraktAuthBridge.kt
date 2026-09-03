package com.mirror.app.features.trakt

fun handleTraktAuthCallbackUrl(url: String) {
    TraktAuthRepository.onAuthCallbackReceived(url)
}
