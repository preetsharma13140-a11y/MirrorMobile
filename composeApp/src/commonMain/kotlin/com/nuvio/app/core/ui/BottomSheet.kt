package com.mirror.app.core.ui

import com.mirror.app.core.ui.Mirror

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirrorModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.Mirror.colors.surfaceSheet,
    contentColor: Color = MaterialTheme.Mirror.colors.textPrimary,
    shape: Shape = RoundedCornerShape(topStart = MirrorTokens.Space.s28, topEnd = MirrorTokens.Space.s28),
    showDragHandle: Boolean = true,
    fullHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (usesNativeMirrorBottomSheet) {
        MirrorNativeModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            showDragHandle = showDragHandle,
            fullHeight = fullHeight,
            content = content,
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = shape,
            dragHandle = if (showDragHandle) {
                { MirrorBottomSheetDragHandle() }
            } else {
                null
            },
            content = content,
        )
    }
}

@Composable
fun MirrorBottomSheetDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.Mirror.colors.borderSubtle,
    )
}

@Composable
fun MirrorBottomSheetActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MaterialTheme.Mirror
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.screenHorizontal, vertical = tokens.spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(MirrorTokens.Space.s14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.colors.accent,
                modifier = Modifier.size(MirrorTokens.Icon.md),
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = tokens.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailingContent?.invoke(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun dismissMirrorBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    if (usesNativeMirrorBottomSheet) {
        dismissNativeMirrorBottomSheet()
    } else if (sheetState.isVisible) {
        sheetState.hide()
    }
    onDismiss()
}

@Composable
private fun MirrorBottomSheetDragHandle() {
    val tokens = MaterialTheme.Mirror
    Box(
        modifier = Modifier
            .padding(top = MirrorTokens.Space.s10, bottom = MirrorTokens.Space.s6)
            .size(width = MirrorTokens.Space.s56 - MirrorTokens.Space.s2, height = MirrorTokens.Space.s5)
            .clip(tokens.shapes.chip)
            .background(tokens.colors.borderDefault),
    )
}
