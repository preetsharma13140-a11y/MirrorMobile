package com.mirror.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mirror.app.core.format.formatReleaseDateForDisplay
import com.mirror.app.core.ui.MirrorPosterCard
import com.mirror.app.core.ui.MirrorPosterShape
import com.mirror.app.core.ui.rememberPosterCardStyleUiState
import com.mirror.app.features.home.MetaPreview
import com.mirror.app.features.home.PosterShape

@Composable
fun HomePosterCard(
    item: MetaPreview,
    modifier: Modifier = Modifier,
    useLandscapeBackdropMode: Boolean = false,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val isLandscapeMode = useLandscapeBackdropMode || posterCardStyle.catalogLandscapeModeEnabled

    MirrorPosterCard(
        title = item.name,
        imageUrl = if (isLandscapeMode) (item.banner ?: item.poster) else item.poster,
        modifier = modifier,
        shape = if (isLandscapeMode) MirrorPosterShape.Landscape else item.posterShape.toMirrorPosterShape(),
        detailLine = if (isLandscapeMode || posterCardStyle.hideLabelsEnabled) null else item.releaseInfo?.let { formatReleaseDateForDisplay(it) },
        showTitleBelow = !posterCardStyle.hideLabelsEnabled,
        bottomLeftLogoUrl = if (isLandscapeMode) item.logo else null,
        bottomLeftText = if (isLandscapeMode && item.logo.isNullOrBlank() && !posterCardStyle.hideLabelsEnabled) item.name else null,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

private fun PosterShape.toMirrorPosterShape(): MirrorPosterShape =
    when (this) {
        PosterShape.Poster -> MirrorPosterShape.Poster
        PosterShape.Square -> MirrorPosterShape.Square
        PosterShape.Landscape -> MirrorPosterShape.Landscape
    }
