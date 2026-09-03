package com.mirror.app.core.ui

import com.mirror.app.core.ui.Mirror

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import mirror.composeapp.generated.resources.Res
import mirror.composeapp.generated.resources.action_back
import mirror.composeapp.generated.resources.action_ok
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.mirror.app.navigation.LocalNativeNavigationBarHidden
import com.mirror.app.navigation.LocalUseNativeNavigation

@Composable
fun MirrorScreen(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = MaterialTheme.Mirror.spacing.screenHorizontal,
    topPadding: Dp? = null,
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val tokens = MaterialTheme.Mirror
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.background),
        contentPadding = PaddingValues(
            start = horizontalPadding,
            top = topPadding ?: tokens.spacing.screenTop + statusBarTop + MirrorPlatformExtraTopPadding,
            end = horizontalPadding,
            bottom = MirrorSafeBottomPadding(tokens.spacing.screenBottom),
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.listGap),
        content = content,
    )
}

internal fun Modifier.MirrorConsumePointerEvents(): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Final).changes.forEach { change ->
                    change.consume()
                }
            }
        }
    }

@Composable
fun MirrorSurfaceCard(
    modifier: Modifier = Modifier,
    tonalElevation: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.Mirror
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tokens.colors.surface,
        shape = tokens.shapes.card,
        tonalElevation = tonalElevation.dp,
        shadowElevation = tokens.elevation.flat,
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.cardPadding),
            content = content,
        )
    }
}

@Composable
fun MirrorScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    includeStatusBarPadding: Boolean = true,
    topPadding: Dp? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tokens = MaterialTheme.Mirror
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val nativeDetailNavigation = LocalUseNativeNavigation.current &&
        !LocalNativeNavigationBarHidden.current &&
        onBack != null
    if (nativeDetailNavigation) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = MirrorTokens.Space.s4),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
        return
    }
    val resolvedTopPadding = topPadding ?: if (includeStatusBarPadding) statusBarTop else MirrorTokens.Space.none
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(tokens.colors.background)
                .MirrorConsumePointerEvents(),
        ) {}
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = resolvedTopPadding, bottom = MirrorTokens.Space.s4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                            tint = tokens.colors.textPrimary,
                        )
                    }
                }
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_header_title",
                ) { currentTitle ->
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.displayLarge,
                        color = tokens.colors.textPrimary,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MirrorTokens.Space.s2),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
fun MirrorSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.Mirror.colors.textMuted,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun MirrorActionLabel(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Text(
        text = text,
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        ),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.Mirror.colors.accent,
    )
}

@Composable
fun MirrorIconActionButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.Mirror.colors.textPrimary,
    onClick: () -> Unit = {},
) {
    val tokens = MaterialTheme.Mirror
    IconButton(
        modifier = modifier
            .background(
                color = tokens.colors.background.copy(alpha = 0.001f),
                shape = tokens.shapes.avatar,
            ),
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

@Composable
fun MirrorBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.Mirror.shapes.avatar,
    containerColor: Color = MaterialTheme.Mirror.colors.surface,
    contentColor: Color = MaterialTheme.Mirror.colors.textPrimary,
    buttonSize: Dp = MirrorTokens.Space.s40,
    iconSize: Dp = MirrorTokens.Icon.md,
    contentDescription: String = stringResource(Res.string.action_back),
) {
    if (LocalUseNativeNavigation.current && !LocalNativeNavigationBarHidden.current) return

    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(shape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun MirrorPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val tokens = MaterialTheme.Mirror
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(MirrorTokens.Space.s48 + MirrorTokens.Space.s4),
        enabled = enabled,
        shape = tokens.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = tokens.colors.accent,
            contentColor = tokens.colors.onAccent,
            disabledContainerColor = tokens.colors.accent.copy(alpha = tokens.opacity.disabled),
            disabledContentColor = tokens.colors.onAccent.copy(alpha = tokens.opacity.disabled),
        ),
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "buttonText",
        ) { animatedText ->
            Text(
                text = animatedText,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MirrorInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable (() -> Unit))? = null,
) {
    val tokens = MaterialTheme.Mirror
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(MirrorTokens.Radius.lg),
        placeholder = {
            Text(
                text = placeholder,
                color = tokens.colors.textMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.textPrimary),
        trailingIcon = trailingContent,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = tokens.colors.borderFocus,
            unfocusedBorderColor = tokens.colors.borderDefault,
            focusedContainerColor = tokens.colors.surfaceCard,
            unfocusedContainerColor = tokens.colors.surfaceCard,
            cursorColor = tokens.colors.accent,
        ),
    )
}

@Composable
fun MirrorInfoBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.Mirror
    Box(
        modifier = modifier
            .background(
                color = tokens.colors.surfaceCard,
                shape = tokens.shapes.chip,
            )
            .padding(horizontal = MirrorTokens.Space.s10, vertical = MirrorTokens.Space.s6),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MirrorInlineMetadata(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.Mirror.colors.textMuted,
        )
        Spacer(modifier = Modifier.width(MirrorTokens.Space.s6))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.Mirror.colors.textPrimary,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MirrorStatusModal(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isBusy: Boolean = false,
    confirmText: String = stringResource(Res.string.action_ok),
    dismissText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    if (!isVisible) return
    val tokens = MaterialTheme.Mirror

    BasicAlertDialog(
        onDismissRequest = {
            if (!isBusy) {
                onDismiss?.invoke() ?: onConfirm()
            }
        },
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = tokens.colors.surfaceDialog,
            shape = tokens.shapes.dialog,
        ) {
            Column(
                modifier = Modifier.padding(tokens.spacing.dialogPadding),
            ) {
                if (isBusy) {
                    MirrorLoadingIndicator(
                        color = tokens.colors.accent,
                    )
                    Spacer(modifier = Modifier.height(MirrorTokens.Space.s16))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(tokens.spacing.controlGap))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textMuted,
                )
                Spacer(modifier = Modifier.height(MirrorTokens.Space.s18))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (!isBusy && dismissText != null && onDismiss != null) {
                        Button(
                            onClick = onDismiss,
                            shape = tokens.shapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.colors.surfaceCard,
                                contentColor = tokens.colors.textPrimary,
                            ),
                        ) {
                            Text(dismissText)
                        }
                        Spacer(modifier = Modifier.width(MirrorTokens.Space.s10))
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !isBusy,
                        shape = tokens.shapes.button,
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

@Composable
fun MirrorToastHost(
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.Mirror
    val toast by MirrorToastController.currentToast.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val visibilityState = remember { MutableTransitionState(false) }
    var renderedToast by remember { mutableStateOf<MirrorToastMessage?>(null) }

    LaunchedEffect(toast?.id) {
        val currentToast = toast
        if (currentToast != null) {
            renderedToast = currentToast
            visibilityState.targetState = true
            delay(currentToast.durationMillis)
            MirrorToastController.dismiss(currentToast.id)
        } else {
            visibilityState.targetState = false
        }
    }

    LaunchedEffect(
        visibilityState.currentState,
        visibilityState.targetState,
        visibilityState.isIdle,
    ) {
        if (visibilityState.isIdle && !visibilityState.currentState && !visibilityState.targetState) {
            renderedToast = null
        }
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
    ) {
        val currentToast = renderedToast ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarTop + tokens.spacing.listGap)
                .padding(horizontal = tokens.spacing.screenHorizontal),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(MirrorTokens.Radius.xl),
                color = tokens.colors.surfacePopover,
                tonalElevation = tokens.elevation.raised,
                shadowElevation = tokens.elevation.overlay,
            ) {
                Text(
                    text = currentToast.message,
                    modifier = Modifier.padding(horizontal = MirrorTokens.Space.s16, vertical = MirrorTokens.Space.s12),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textPrimary,
                )
            }
        }
    }
}

data class MirrorToastMessage(
    val id: Long,
    val message: String,
    val durationMillis: Long,
)

object MirrorToastController {
    private val _currentToast = MutableStateFlow<MirrorToastMessage?>(null)
    val currentToast = _currentToast.asStateFlow()
    private var nextToastId = 0L

    fun show(
        message: String,
        durationMillis: Long = 2500L,
    ) {
        nextToastId += 1L
        _currentToast.value = MirrorToastMessage(
            id = nextToastId,
            message = message,
            durationMillis = durationMillis,
        )
    }

    fun dismiss(id: Long? = null) {
        val activeToast = _currentToast.value ?: return
        if (id == null || activeToast.id == id) {
            _currentToast.value = null
        }
    }
}
