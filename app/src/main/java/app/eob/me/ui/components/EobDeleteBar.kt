package app.eob.me.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings

@Composable
fun EobDeleteBar(
    language: AppLanguage,
    selectedRecord: EobRecord?,
    onDeleteEob: (EobRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val record = selectedRecord ?: return
    OutlinedButton(
        onClick = { onDeleteEob(record) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${EobStrings.t(language, "deleteEob")}: ${record.providerName}",
            color = MaterialTheme.colorScheme.error
        )
    }
}
