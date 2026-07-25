package app.eob.me.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.eob.me.data.AppLanguage
import app.eob.me.data.DuplicateEobWarningState
import app.eob.me.data.EobStrings

@Composable
fun DuplicateEobWarningDialog(
    language: AppLanguage,
    warningState: DuplicateEobWarningState?,
    onDiscard: () -> Unit,
    onOverwrite: () -> Unit
) {
    if (warningState == null) return
    AlertDialog(
        onDismissRequest = onDiscard,
        title = {
            Text(text = EobStrings.t(language, "duplicateEobDialogTitle"))
        },
        text = {
            Text(text = EobStrings.t(language, "duplicateEobDialogMessage"))
        },
        confirmButton = {
            TextButton(onClick = onOverwrite) {
                Text(text = EobStrings.t(language, "duplicateEobDialogUpdate"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(text = EobStrings.t(language, "duplicateEobDialogDiscard"))
            }
        }
    )
}
