package com.mirror.app.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal expect val MirrorPlatformExtraTopPadding: Dp
internal expect val MirrorPlatformExtraBottomPadding: Dp
internal expect val MirrorBottomNavigationExtraVerticalPadding: Dp
@Composable
internal expect fun MirrorBottomNavigationBarInsets(): WindowInsets

/** Physical display-safe top inset, excluding any enclosing native toolbar. */
@Composable
internal expect fun platformPhysicalTopInset(): Dp

internal val LocalMirrorBottomNavigationOverlayPadding = staticCompositionLocalOf { 0.dp }

/** CompositionLocal providing the shared [MirrorNavBarScrollState] so child screens can attach the nestedScrollConnection. */
val LocalMirrorNavBarScrollState = staticCompositionLocalOf<MirrorNavBarScrollState?> { null }

@Composable
internal fun MirrorSafeBottomPadding(extra: Dp = 0.dp): Dp {
	val navigationBarBottom = MirrorBottomNavigationBarInsets()
		.asPaddingValues()
		.calculateBottomPadding()
	return navigationBarBottom.coerceAtLeast(MirrorPlatformExtraBottomPadding) +
		LocalMirrorBottomNavigationOverlayPadding.current +
		extra
}
