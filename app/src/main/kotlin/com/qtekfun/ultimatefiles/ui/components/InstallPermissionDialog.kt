package com.qtekfun.ultimatefiles.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qtekfun.ultimatefiles.R

/** Shown before leaving for the system settings, so the jump there does not come out of nowhere. */
@Composable
fun InstallPermissionDialog(fileName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.install_permission_title)) },
        text = { Text(stringResource(R.string.install_permission_needed, fileName)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.install_permission_open_settings)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
