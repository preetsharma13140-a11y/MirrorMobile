package com.mirror.app.features.player.skip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mirror.composeapp.generated.resources.Res
import mirror.composeapp.generated.resources.player_skip
import mirror.composeapp.generated.resources.player_skip_intro
import mirror.composeapp.generated.resources.player_skip_outro
import mirror.composeapp.generated.resources.player_skip_recap
import org.jetbrains.compose.resources.stringResource

@Composable
fun SkipIntroButton(
    interval: SkipInterval?,
    dismissed: Boolean,
    controlsVisible: Boolean,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastType by remember { mutableStateOf(interval?.type) }
    if (interval != null) lastType = interval.type
    val shouldShow = interval != null && (!dismissed || controlsVisible)

    var autoHidden by remember { mutableStateOf(false) }
    var manuallyDismissed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(interval?.startTime, interval?.type) {
        autoHidden = false
        manuallyDismissed = false
        progress.snapTo(0f)
    }

    LaunchedEffect(dismissed) {
        if (!dismissed) {
            if (!manuallyDismissed) {
                autoHidden = false
                progress.snapTo(0f)
            }
        } else {
            manuallyDismissed = true
        }
    }

    LaunchedEffect(shouldShow, autoHidden, controlsVisible) {
        // No auto-hide timer — the button stays visible for the entire duration of the
        // intro/outro/recap interval. It disappears naturally when the position exits
        // the interval (activeSkipInterval becomes null). Only auto-hide if dismissed.
        if (!shouldShow || autoHidden) return@LaunchedEffect
        if (!controlsVisible) {
            // Still show the button even when controls are hidden — just no progress bar
            progress.snapTo(0f)
        }
    }

    val isVisible = shouldShow && (!autoHidden || controlsVisible)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f),
        modifier = modifier,
    ) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .clip(shape)
                .background(Color(0xFF1E1E1E).copy(alpha = 0.85f))
                .clickable { onSkip() },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = skipLabel(lastType),
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Color.White.copy(alpha = if (controlsVisible || autoHidden || dismissed) 0f else 0.15f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .height(3.dp)
                        .background(
                            Color(0xFF1E1E1E).copy(
                                alpha = if (controlsVisible || autoHidden || dismissed) 0f else 0.85f,
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun skipLabel(type: String?): String =
    when (type?.lowercase()) {
        "intro", "op", "mixed-op" -> stringResource(Res.string.player_skip_intro)
        "outro", "ed", "mixed-ed", "credits" -> stringResource(Res.string.player_skip_outro)
        "recap" -> stringResource(Res.string.player_skip_recap)
        else -> stringResource(Res.string.player_skip)
    }
