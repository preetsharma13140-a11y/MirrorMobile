package com.mirror.app.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

data class MirrorTypeScale(
    val labelXs: TextStyle,
    val labelSm: TextStyle,
    val bodySm: TextStyle,
    val bodyMd: TextStyle,
    val bodyLg: TextStyle,
    val titleSm: TextStyle,
    val titleMd: TextStyle,
    val titleLg: TextStyle,
    val displaySm: TextStyle,
    val displayMd: TextStyle,
)

internal val LocalMirrorTypeScale = staticCompositionLocalOf {
    MirrorTypeScale(
        labelXs = TextStyle(fontSize = MirrorTokens.Type.labelXs, lineHeight = MirrorTokens.LineHeight.labelXs, fontWeight = FontWeight.Medium),
        labelSm = TextStyle(fontSize = MirrorTokens.Type.labelSm, lineHeight = MirrorTokens.LineHeight.labelSm, fontWeight = FontWeight.Medium),
        bodySm = TextStyle(fontSize = MirrorTokens.Type.bodySm, lineHeight = MirrorTokens.LineHeight.bodySm, fontWeight = FontWeight.Normal),
        bodyMd = TextStyle(fontSize = MirrorTokens.Type.bodyMd, lineHeight = MirrorTokens.LineHeight.bodyMd, fontWeight = FontWeight.Normal),
        bodyLg = TextStyle(fontSize = MirrorTokens.Type.bodyLg, lineHeight = MirrorTokens.LineHeight.bodyLg, fontWeight = FontWeight.Medium),
        titleSm = TextStyle(fontSize = MirrorTokens.Type.titleSm, lineHeight = MirrorTokens.LineHeight.titleSm, fontWeight = FontWeight.Bold),
        titleMd = TextStyle(fontSize = MirrorTokens.Type.titleMd, lineHeight = MirrorTokens.LineHeight.titleMd, fontWeight = FontWeight.Bold),
        titleLg = TextStyle(fontSize = MirrorTokens.Type.titleLg, lineHeight = MirrorTokens.LineHeight.titleLg, fontWeight = FontWeight.Bold),
        displaySm = TextStyle(fontSize = MirrorTokens.Type.displaySm, lineHeight = MirrorTokens.LineHeight.displaySm, fontWeight = FontWeight.ExtraBold),
        displayMd = TextStyle(fontSize = MirrorTokens.Type.displayMd, lineHeight = MirrorTokens.LineHeight.displayMd, fontWeight = FontWeight.ExtraBold),
    )
}

val MaterialTheme.MirrorTypeScale: MirrorTypeScale
    @Composable
    get() = LocalMirrorTypeScale.current
