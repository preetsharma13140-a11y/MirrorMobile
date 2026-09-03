package com.mirror.app.core.ui

import com.mirror.app.core.ui.Mirror

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import mirror.composeapp.generated.resources.Res
import mirror.composeapp.generated.resources.episodes_cd_watched
import org.jetbrains.compose.resources.stringResource

@Composable
fun MirrorWatchedBadge(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.Mirror
    Box(
        modifier = modifier
            .size(MirrorTokens.Icon.md)
            .clip(tokens.shapes.avatar)
            .background(tokens.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(Res.string.episodes_cd_watched),
            tint = tokens.colors.onAccent,
            modifier = Modifier.size(MirrorTokens.Icon.xs),
        )
    }
}

@Composable
fun MirrorAnimatedWatchedBadge(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        MirrorWatchedBadge()
    }
}

@Composable
fun BoxScope.MirrorPosterWatchedOverlay(
    isWatched: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = MirrorTokens.Space.s6,
) {
    MirrorAnimatedWatchedBadge(
        isVisible = isWatched,
        modifier = modifier
            .align(Alignment.TopEnd)
            .padding(padding),
    )
}
