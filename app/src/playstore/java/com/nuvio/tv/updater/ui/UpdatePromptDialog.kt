package com.robbdeeze.nuviotv.updater.ui

import androidx.compose.runtime.Composable
import com.robbdeeze.nuviotv.updater.UpdateUiState

@Composable
fun UpdatePromptDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onIgnore: () -> Unit,
    onOpenUnknownSources: () -> Unit
) = Unit
