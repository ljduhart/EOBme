package app.eob.me.ui.components.clinical

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.ClinicalNotesUiState
import app.eob.me.data.ClinicalProviderOption
import app.eob.me.data.EobStrings
import app.eob.me.data.local.entity.ClinicalNote
import app.eob.me.ui.theme.EobBrandGlow
import app.eob.me.ui.theme.EobCyberAccent
import app.eob.me.ui.theme.EobCyberBackgroundDeep
import app.eob.me.ui.theme.EobCyberSurface
import app.eob.me.ui.theme.EobCyberTextPrimary
import app.eob.me.ui.theme.EobCyberTextSecondary
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val NotesBackground = EobCyberBackgroundDeep
private val BentoSurface = Color(0xFF0D1B2E)
private val SilverBorder = Color(0xFFC0C8D4)
private val GlowAccent = EobBrandGlow.copy(alpha = 0.55f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalNotesBottomSheet(
    language: AppLanguage,
    visible: Boolean,
    state: ClinicalNotesUiState,
    onDismiss: () -> Unit,
    onSelectProvider: (Int) -> Unit,
    onQuestionsChange: (String) -> Unit,
    onAnswersChange: (String) -> Unit,
    onToggleSpeech: () -> Unit,
    onSaveNote: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    if (!visible) return

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NotesBackground,
        dragHandle = {
            Spacer(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .background(SilverBorder.copy(alpha = 0.65f), RoundedCornerShape(50))
            )
        }
    ) {
        ClinicalNotesSheetContent(
            language = language,
            state = state,
            onSelectProvider = onSelectProvider,
            onQuestionsChange = onQuestionsChange,
            onAnswersChange = onAnswersChange,
            onToggleSpeech = onToggleSpeech,
            onSaveNote = onSaveNote,
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .imePadding()
                .padding(bottom = 12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClinicalNotesSheetContent(
    language: AppLanguage,
    state: ClinicalNotesUiState,
    onSelectProvider: (Int) -> Unit,
    onQuestionsChange: (String) -> Unit,
    onAnswersChange: (String) -> Unit,
    onToggleSpeech: () -> Unit,
    onSaveNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = EobCyberTextPrimary,
        unfocusedTextColor = EobCyberTextPrimary,
        focusedBorderColor = EobCyberAccent,
        unfocusedBorderColor = SilverBorder.copy(alpha = 0.5f),
        focusedContainerColor = EobCyberSurface,
        unfocusedContainerColor = EobCyberSurface
    )

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = EobStrings.t(language, "clinicalNotesSheetTitle"),
                color = EobCyberTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ProviderDropdownChip(
                language = language,
                providers = state.providers,
                selectedProviderId = state.selectedProviderId,
                onSelectProvider = onSelectProvider
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClinicalNoteBentoCard(
                    modifier = Modifier.weight(1f),
                    title = EobStrings.t(language, "clinicalNotesQuestionsTitle"),
                    content = {
                        OutlinedTextField(
                            value = state.questionsToAsk,
                            onValueChange = onQuestionsChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp),
                            colors = fieldColors,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            placeholder = {
                                Text(EobStrings.t(language, "clinicalNotesQuestionsHint"))
                            }
                        )
                    }
                )
                ClinicalNoteBentoCard(
                    modifier = Modifier.weight(1f),
                    title = EobStrings.t(language, "clinicalNotesAnswersTitle"),
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpeechMicButton(
                                    language = language,
                                    isListening = state.isListening,
                                    onClick = onToggleSpeech
                                )
                            }
                            OutlinedTextField(
                                value = state.providerAnswers,
                                onValueChange = onAnswersChange,
                                readOnly = state.isListening,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                colors = fieldColors,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                placeholder = {
                                    Text(EobStrings.t(language, "clinicalNotesAnswersHint"))
                                }
                            )
                        }
                    }
                )
            }
        }
        item {
            if (state.errorMessage.isNotBlank()) {
                Text(
                    text = EobStrings.t(language, state.errorMessage),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
            Button(
                onClick = onSaveNote,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(EobStrings.t(language, "clinicalNotesSave"))
            }
        }
        if (state.savedNotesForProvider.isNotEmpty()) {
            item {
                Text(
                    text = EobStrings.t(language, "clinicalNotesRecentTitle"),
                    color = EobCyberTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(state.savedNotesForProvider, key = { it.noteId }) { note ->
                SavedClinicalNoteRow(language = language, note = note)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDropdownChip(
    language: AppLanguage,
    providers: List<ClinicalProviderOption>,
    selectedProviderId: Int?,
    onSelectProvider: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = providers.firstOrNull { it.providerId == selectedProviderId }?.displayLabel
        ?: EobStrings.t(language, "clinicalNotesSelectProvider")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(EobStrings.t(language, "clinicalNotesProviderLabel")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = EobCyberTextPrimary,
                unfocusedTextColor = EobCyberTextPrimary,
                focusedBorderColor = GlowAccent,
                unfocusedBorderColor = SilverBorder.copy(alpha = 0.65f),
                focusedContainerColor = BentoSurface,
                unfocusedContainerColor = BentoSurface
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = GlowAccent)
                .border(BorderStroke(1.5.dp, SilverBorder.copy(alpha = 0.85f)), RoundedCornerShape(12.dp))
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = BentoSurface
        ) {
            providers.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayLabel,
                            color = EobCyberTextPrimary
                        )
                    },
                    onClick = {
                        onSelectProvider(option.providerId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ClinicalNoteBentoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = GlowAccent)
            .background(
                brush = Brush.verticalGradient(listOf(BentoSurface, NotesBackground)),
                shape = RoundedCornerShape(16.dp)
            )
            .border(BorderStroke(1.5.dp, SilverBorder.copy(alpha = 0.85f)), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = EobCyberAccent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
        content()
    }
}

@Composable
private fun SpeechMicButton(
    language: AppLanguage,
    isListening: Boolean,
    onClick: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "clinical_mic_pulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_glow_alpha"
    )
    val tint = if (isListening) {
        EobCyberAccent.copy(alpha = glowAlpha)
    } else {
        EobCyberTextSecondary
    }
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = EobStrings.t(language, "clinicalNotesMicContentDescription"),
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = if (isListening) 1.08f else 1f
                scaleY = if (isListening) 1.08f else 1f
            }
        )
    }
}

@Composable
private fun SavedClinicalNoteRow(
    language: AppLanguage,
    note: ClinicalNote
) {
    val dateLabel = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        .format(Date(note.dateCreated))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, SilverBorder.copy(alpha = 0.45f)), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = dateLabel, color = EobCyberTextSecondary, fontWeight = FontWeight.Medium)
        if (note.questionsToAsk.isNotBlank()) {
            Text(
                text = EobStrings.tf(language, "clinicalNotesSavedQuestionsLine", note.questionsToAsk),
                color = EobCyberTextPrimary
            )
        }
        if (note.providerAnswers.isNotBlank()) {
            Text(
                text = EobStrings.tf(language, "clinicalNotesSavedAnswersLine", note.providerAnswers),
                color = EobCyberTextPrimary
            )
        }
    }
}
