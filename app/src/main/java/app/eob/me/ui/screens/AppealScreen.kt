package app.eob.me.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppealTarget
import app.eob.me.data.AppLanguage
import app.eob.me.data.DoctorDisputeStrategy
import app.eob.me.data.InsuranceAppealStrategy
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.UserProfile
import app.eob.me.data.VeryfiExtractedData

private val AppealPaperColor = Color.White
private val AppealPaperText = Color(0xFF1A1A1A)

private val AppealDocumentTypography = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 15.sp,
    lineHeight = 24.sp,
    color = AppealPaperText
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppealScreen(
    language: AppLanguage,
    profile: UserProfile,
    selectedRecord: EobRecord?,
    selectedTarget: AppealTarget,
    selectedDisputeStrategy: DoctorDisputeStrategy,
    selectedInsuranceAppealStrategy: InsuranceAppealStrategy,
    appealLetter: String,
    appealLetterEditingEnabled: Boolean,
    veryfiExtractedData: VeryfiExtractedData? = null,
    onAppealTargetSwitched: (AppealTarget) -> Unit,
    onDisputeStrategySwitched: (DoctorDisputeStrategy) -> Unit,
    onRegenerate: () -> Unit,
    onEditLetter: (String) -> Unit,
    onEnableEditing: () -> Unit,
    onSaveLetter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val documentAnimationKey = if (appealLetterEditingEnabled) {
        "editing"
    } else {
        val veryfiKey = veryfiExtractedData?.let { data ->
            "${data.dateOfService}_${data.copay}_${data.cptCodes.joinToString(",")}"
        }.orEmpty()
        "${selectedTarget.name}_${selectedDisputeStrategy.name}_${selectedInsuranceAppealStrategy.name}_${veryfiKey}_$appealLetter"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .padding(bottom = 108.dp)
        ) {
            Text(
                text = EobStrings.t(language, "appealGeneratorTitle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (selectedRecord == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = EobStrings.t(language, "appealSelectClaimHint"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = EobStrings.tf(language, "appealingProvider", selectedRecord.providerName),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = EobStrings.tf(language, "appealServiceDate", selectedRecord.serviceDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AppealTargetSelector(
                    language = language,
                    selectedTarget = selectedTarget,
                    onTargetSelected = onAppealTargetSwitched
                )

                AnimatedVisibility(
                    visible = false,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DoctorDisputeStrategySelector(
                        language = language,
                        selectedStrategy = selectedDisputeStrategy,
                        onStrategySelected = onDisputeStrategySwitched
                    )
                }

                AppealInsightHud(
                    language = language,
                    record = selectedRecord,
                    selectedTarget = selectedTarget,
                    selectedDisputeStrategy = selectedDisputeStrategy,
                    selectedInsuranceAppealStrategy = selectedInsuranceAppealStrategy,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )

                AnimatedContent(
                    targetState = documentAnimationKey,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width } + fadeOut())
                    },
                    label = "appeal_document",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { animationKey ->
                    key(animationKey) {
                        AppealPaperDocument(
                            language = language,
                            appealLetter = appealLetter,
                            appealLetterEditingEnabled = appealLetterEditingEnabled,
                            onEditLetter = onEditLetter,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (selectedRecord != null) {
            AppealActionBar(
                language = language,
                appealLetter = appealLetter,
                appealLetterEditingEnabled = appealLetterEditingEnabled,
                onRegenerate = onRegenerate,
                onEnableEditing = onEnableEditing,
                onSaveLetter = onSaveLetter,
                onCopy = {
                    copyAppealLetterToClipboard(
                        context = context,
                        language = language,
                        appealLetter = appealLetter
                    )
                },
                onSend = {
                    shareAppealLetter(
                        context = context,
                        language = language,
                        appealLetter = appealLetter,
                        selectedTarget = selectedTarget
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppealTargetSelector(
    language: AppLanguage,
    selectedTarget: AppealTarget,
    onTargetSelected: (AppealTarget) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        AppealTarget.entries.forEachIndexed { index, target ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = AppealTarget.entries.size
                ),
                onClick = { onTargetSelected(target) },
                selected = selectedTarget == target,
                label = {
                    Text(
                        text = EobStrings.t(language, target.labelKey()),
                        maxLines = 1
                    )
                }
            )
        }
    }
}

@Composable
private fun DoctorDisputeStrategySelector(
    language: AppLanguage,
    selectedStrategy: DoctorDisputeStrategy,
    onStrategySelected: (DoctorDisputeStrategy) -> Unit
) {
    val chipScrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(chipScrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DoctorDisputeStrategy.entries.forEach { strategy ->
            FilterChip(
                selected = selectedStrategy == strategy,
                onClick = { onStrategySelected(strategy) },
                label = {
                    Text(
                        text = EobStrings.t(language, strategy.labelKey()),
                        maxLines = 1
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun AppealInsightHud(
    language: AppLanguage,
    record: EobRecord,
    selectedTarget: AppealTarget,
    selectedDisputeStrategy: DoctorDisputeStrategy,
    selectedInsuranceAppealStrategy: InsuranceAppealStrategy,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = resolveAppealInsight(
                    language = language,
                    record = record,
                    target = selectedTarget,
                    strategy = selectedDisputeStrategy,
                    insuranceStrategy = selectedInsuranceAppealStrategy
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppealPaperDocument(
    language: AppLanguage,
    appealLetter: String,
    appealLetterEditingEnabled: Boolean,
    onEditLetter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(8.5f / 11f)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(4.dp),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            ),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = AppealPaperColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            if (appealLetterEditingEnabled) {
                BasicTextField(
                    value = appealLetter,
                    onValueChange = onEditLetter,
                    textStyle = AppealDocumentTypography,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            innerTextField()
                        }
                    }
                )
            } else {
                Text(
                    text = appealLetter.ifBlank { EobStrings.t(language, "appealDraftPlaceholder") },
                    style = AppealDocumentTypography,
                    color = if (appealLetter.isBlank()) {
                        AppealPaperText.copy(alpha = 0.45f)
                    } else {
                        AppealPaperText
                    }
                )
            }
        }
    }
}

@Composable
fun AppealActionBar(
    language: AppLanguage,
    appealLetter: String,
    appealLetterEditingEnabled: Boolean,
    onRegenerate: () -> Unit,
    onEnableEditing: () -> Unit,
    onSaveLetter: () -> Unit,
    onCopy: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val letterReady = appealLetter.isNotBlank()
    val dockScrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 10.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(dockScrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppealDockAction(
                label = EobStrings.t(language, "appealRegenerate"),
                enabled = true,
                onClick = onRegenerate
            )
            AppealDockAction(
                label = EobStrings.t(language, "appealEditLetter"),
                enabled = !appealLetterEditingEnabled && letterReady,
                onClick = onEnableEditing
            )
            AppealDockAction(
                label = EobStrings.t(language, "appealSaveLetter"),
                enabled = appealLetterEditingEnabled,
                onClick = onSaveLetter
            )
            AppealDockAction(
                label = EobStrings.t(language, "appealCopy"),
                enabled = letterReady,
                onClick = onCopy
            )
            AppealDockAction(
                label = EobStrings.t(language, "appealSend"),
                enabled = letterReady,
                onClick = onSend
            )
        }
    }
}

@Composable
private fun AppealDockAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = 40.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

internal fun resolveAppealInsight(
    language: AppLanguage,
    record: EobRecord,
    target: AppealTarget,
    strategy: DoctorDisputeStrategy,
    insuranceStrategy: InsuranceAppealStrategy
): String {
    val insightBody = when (target) {
        AppealTarget.DOCTOR -> EobStrings.t(language, strategy.insightKey())
        AppealTarget.INSURANCE -> EobStrings.t(language, insuranceStrategy.insightKey())
    }

    return EobStrings.tf(language, "appealInsightSentence", insightBody)
}

private fun copyAppealLetterToClipboard(
    context: Context,
    language: AppLanguage,
    appealLetter: String
) {
    if (appealLetter.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText(EobStrings.t(language, "appealLetter"), appealLetter)
    )
}

private fun shareAppealLetter(
    context: Context,
    language: AppLanguage,
    appealLetter: String,
    selectedTarget: AppealTarget
) {
    if (appealLetter.isBlank()) return
    val subject = when (selectedTarget) {
        AppealTarget.DOCTOR -> EobStrings.t(language, "appealSendSubjectDoctor")
        AppealTarget.INSURANCE -> EobStrings.t(language, "appealSendSubjectInsurance")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, appealLetter)
    }
    context.startActivity(
        Intent.createChooser(sendIntent, EobStrings.t(language, "appealSend"))
    )
}
