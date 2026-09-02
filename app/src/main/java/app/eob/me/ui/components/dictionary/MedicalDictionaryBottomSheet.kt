package app.eob.me.ui.components.dictionary

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.local.entity.MedicalDictionaryEntity
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobCyberBackgroundDeep
import app.eob.me.ui.theme.EobCyberTextPrimary
import app.eob.me.ui.theme.EobCyberTextSecondary

private val SheetBackground = EobCyberBackgroundDeep
private val CardSurface = Color(0xFF0D1B2E)
private val CategoryPill = Color(0xFF1B3A57)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalDictionaryBottomSheet(
    language: AppLanguage,
    visible: Boolean,
    query: String,
    results: List<MedicalDictionaryEntity>,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackground,
        dragHandle = null,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = EobStrings.t(language, "medicalDictionaryTitle"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = EobCyberTextPrimary
            )
            Text(
                text = EobStrings.t(language, "medicalDictionarySubtitle"),
                style = MaterialTheme.typography.bodyMedium,
                color = EobCyberTextSecondary
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(EobStrings.t(language, "medicalDictionarySearchPlaceholder"))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Search
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EobBrandBlue,
                    unfocusedBorderColor = EobCyberTextSecondary.copy(alpha = 0.35f),
                    focusedTextColor = EobCyberTextPrimary,
                    unfocusedTextColor = EobCyberTextPrimary,
                    cursorColor = EobBrandBlue
                )
            )

            if (query.isBlank()) {
                Text(
                    text = EobStrings.t(language, "medicalDictionarySearchHint"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EobCyberTextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else if (results.isEmpty()) {
                Text(
                    text = EobStrings.t(language, "medicalDictionaryNoResults"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EobCyberTextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results, key = { it.rowid }) { entry ->
                        MedicalDictionaryResultCard(
                            language = language,
                            entry = entry
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicalDictionaryResultCard(
    language: AppLanguage,
    entry: MedicalDictionaryEntity
) {
    var expanded by remember(entry.rowid) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, EobCyberTextSecondary.copy(alpha = 0.18f)),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.term,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EobCyberTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = CategoryPill
                ) {
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = EobBrandBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = entry.pronunciation,
                style = MaterialTheme.typography.bodySmall,
                color = EobCyberTextSecondary
            )
            Text(
                text = entry.definition,
                style = MaterialTheme.typography.bodyMedium,
                color = EobCyberTextPrimary
            )
            if (expanded) {
                Text(
                    text = entry.detailedBreakdown,
                    style = MaterialTheme.typography.bodySmall,
                    color = EobCyberTextSecondary
                )
                Text(
                    text = EobStrings.t(language, "medicalDictionaryTapToCollapse"),
                    style = MaterialTheme.typography.labelSmall,
                    color = EobBrandBlue
                )
            } else {
                Text(
                    text = EobStrings.t(language, "medicalDictionaryTapToExpand"),
                    style = MaterialTheme.typography.labelSmall,
                    color = EobBrandBlue
                )
            }
        }
    }
}
