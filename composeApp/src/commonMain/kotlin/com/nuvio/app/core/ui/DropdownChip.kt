package com.mirror.app.core.ui

import com.mirror.app.core.ui.Mirror

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

data class MirrorDropdownOption(
    val key: String,
    val label: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirrorDropdownChip(
    title: String,
    label: String,
    selectedKey: String?,
    options: List<MirrorDropdownOption>,
    enabled: Boolean = true,
    onSelected: (MirrorDropdownOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.Mirror
    var isSheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .clip(tokens.shapes.compactCard)
            .background(tokens.colors.surface)
            .then(
                if (enabled) {
                    Modifier.clickable { isSheetVisible = true }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = MirrorTokens.Space.s12, vertical = tokens.components.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(MirrorTokens.Space.s6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) tokens.colors.textPrimary else tokens.colors.textDisabled,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(MirrorTokens.Icon.sm + MirrorTokens.Space.s2),
            tint = if (enabled) tokens.colors.textMuted else tokens.colors.borderDefault,
        )
    }

    if (isSheetVisible) {
        MirrorDropdownOptionsSheet(
            title = title,
            options = options,
            selectedKey = selectedKey,
            sheetState = sheetState,
            onDismiss = {
                coroutineScope.launch {
                    dismissMirrorBottomSheet(
                        sheetState = sheetState,
                        onDismiss = { isSheetVisible = false },
                    )
                }
            },
            onSelected = { option ->
                onSelected(option)
                coroutineScope.launch {
                    dismissMirrorBottomSheet(
                        sheetState = sheetState,
                        onDismiss = { isSheetVisible = false },
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MirrorDropdownOptionsSheet(
    title: String,
    options: List<MirrorDropdownOption>,
    selectedKey: String?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelected: (MirrorDropdownOption) -> Unit,
) {
    val tokens = MaterialTheme.Mirror
    MirrorModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MirrorSafeBottomPadding(tokens.spacing.screenHorizontal)),
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = tokens.spacing.screenHorizontal, vertical = MirrorTokens.Space.s14),
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
            )
            MirrorBottomSheetDivider()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = tokens.breakpoints.largePhone),
            ) {
                itemsIndexed(options) { index, option ->
                    MirrorBottomSheetActionRow(
                        title = option.label,
                        onClick = { onSelected(option) },
                        trailingContent = {
                            if (option.key == selectedKey) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = tokens.colors.accent,
                                    modifier = Modifier.size(tokens.icons.md),
                                )
                            }
                        },
                    )
                    if (index < options.lastIndex) {
                        MirrorBottomSheetDivider()
                    }
                }
            }
        }
    }
}
