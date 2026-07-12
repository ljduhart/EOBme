package app.eob.me.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings

@Composable
fun LogoutConfirmDialog(
    language: AppLanguage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = EobStrings.t(language, "logoutConfirmTitle"),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = EobStrings.t(language, "logoutConfirmMessage"),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(EobStrings.t(language, "logoutConfirmYes"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(EobStrings.t(language, "logoutConfirmNo"))
            }
        }
    )
}
