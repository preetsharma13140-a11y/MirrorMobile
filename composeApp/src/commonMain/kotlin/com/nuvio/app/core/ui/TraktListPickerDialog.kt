package com.mirror.app.core.ui

import com.mirror.app.core.ui.Mirror

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mirror.app.features.trakt.TraktListTab
import mirror.composeapp.generated.resources.Res
import mirror.composeapp.generated.resources.action_cancel
import mirror.composeapp.generated.resources.action_save
import mirror.composeapp.generated.resources.compose_trakt_list_picker_loading
import mirror.composeapp.generated.resources.compose_trakt_list_picker_subtitle
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraktListPickerDialog(
    visible: Boolean,
    title: String,
    tabs: List<TraktListTab>,
    membership: Map<String, Boolean>,
    isPending: Boolean,
    errorMessage: String?,
    onToggle: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val tokens = MaterialTheme.Mirror

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = tokens.colors.surfaceDialog,
            shape = RoundedCornerShape(MirrorTokens.Radius.xl),
        ) {
            Column(
                modifier = Modifier.padding(tokens.spacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.listGap),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                )
                Text(
                    text = stringResource(Res.string.compose_trakt_list_picker_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.danger,
                    )
                }

                if (isPending && tabs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MirrorTokens.Space.s80 + MirrorTokens.Space.s80 + MirrorTokens.Space.s80 + MirrorTokens.Space.s40),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.listGap),
                        ) {
                            MirrorLoadingIndicator(
                                modifier = Modifier.size(tokens.icons.lg),
                            )
                            Text(
                                text = stringResource(Res.string.compose_trakt_list_picker_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.colors.textMuted,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MirrorTokens.Space.s80 + MirrorTokens.Space.s80 + MirrorTokens.Space.s80 + MirrorTokens.Space.s40),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
                    ) {
                        items(items = tabs, key = { it.key }) { tab ->
                            val selected = membership[tab.key] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (selected) {
                                            tokens.colors.accent.copy(alpha = tokens.opacity.selected)
                                        } else {
                                            tokens.colors.surfaceCard.copy(alpha = tokens.opacity.medium)
                                        },
                                        shape = tokens.shapes.compactCard,
                                    )
                                    .clickable(enabled = !isPending) { onToggle(tab.key) }
                                    .padding(horizontal = MirrorTokens.Space.s14, vertical = tokens.spacing.listGap),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = tab.title,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = tokens.colors.textPrimary,
                                )
                                if (selected) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = tokens.colors.accent,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MirrorTokens.Space.s10, Alignment.End),
                ) {
                    Button(
                        onClick = onDismiss,
                        enabled = !isPending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.colors.surfaceCard,
                            contentColor = tokens.colors.textPrimary,
                        ),
                    ) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                    Button(
                        onClick = onSave,
                        enabled = !isPending,
                    ) {
                        if (isPending) {
                            MirrorLoadingIndicator(
                                color = tokens.colors.onAccent,
                                modifier = Modifier.size(tokens.icons.sm),
                            )
                        } else {
                            Text(stringResource(Res.string.action_save))
                        }
                    }
                }
            }
        }
    }
}
