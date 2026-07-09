package com.robbdeeze.nuviotv.ui.screens.settings

import com.robbdeeze.nuviotv.ui.theme.NuvioTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.robbdeeze.nuviotv.R
import com.robbdeeze.nuviotv.domain.model.ExperienceMode
import com.robbdeeze.nuviotv.ui.components.NuvioDialog

@Composable
internal fun ExperienceModeConfirmationDialog(
    targetMode: ExperienceMode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEssential = targetMode == ExperienceMode.ESSENTIAL
    val confirmFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        confirmFocusRequester.requestFocus()
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(
            if (isEssential) {
                R.string.experience_mode_confirm_essential_title
            } else {
                R.string.experience_mode_confirm_advanced_title
            }
        ),
        subtitle = if (isEssential) {
            stringResource(R.string.experience_mode_confirm_essential_subtitle)
        } else {
            stringResource(R.string.experience_mode_confirm_advanced_subtitle)
        },
        suppressFirstKeyUp = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.colors(
                    containerColor = NuvioTheme.colors.BackgroundCard,
                    contentColor = NuvioTheme.colors.TextPrimary
                )
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(confirmFocusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = NuvioTheme.colors.BackgroundCard,
                    contentColor = NuvioTheme.colors.TextPrimary
                )
            ) {
                Text(
                    stringResource(
                        if (isEssential) {
                            R.string.experience_mode_switch_to_essential
                        } else {
                            R.string.experience_mode_switch_to_advanced
                        }
                    )
                )
            }
        }
    }
}
