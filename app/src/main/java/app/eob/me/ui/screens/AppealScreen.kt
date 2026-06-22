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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.UserProfile
import app.eob.me.data.VeryfiExtractedData

private val AppealCanvasBackground = Color(0xFF2D2D2D)
private val AppealPaperText = Color(0xFF1A1A1A)
private val AppealPillButtonBackground = Color(0xFFE5E5EA)
private val AppealHeroCyan = Color(0xFF00E5FF)
private val AppealHeroTeal = Color(0xFF00695C)
private val AppealHeroGlow = Color(0xFF00BCD4)
private val AppealDoctorAccent = Color(0xFF5C6BC0)
private val AppealDoctorChipSelected = Color(0xFFFFB74D)

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
        "${selectedTarget.name}_${selectedDisputeStrategy.name}_${veryfiKey}_$appealLetter"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppealCanvasBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .padding(bottom = 100.dp)
        ) {
            Text(
                text = EobStrings.t(language, "appealGeneratorTitle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = EobStrings.tf(language, "appealingProvider", selectedRecord.providerName),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Text(
                        text = EobStrings.tf(language, "appealServiceDate", selectedRecord.serviceDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }

                AppealTargetSelector(
                    language = language,
                    selectedTarget = selectedTarget,
                    onTargetSelected = onAppealTargetSwitched
                )

                AnimatedVisibility(
                    visible = selectedTarget == AppealTarget.DOCTOR,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DoctorDisputeStrategySelector(
                        language = language,
                        selectedStrategy = selectedDisputeStrategy,
                        onStrategySelected = onDisputeStrategySwitched
                    )
                }

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
                ) { _ ->
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
                        appealLetter = appealLetter
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                    selectedContainerColor = AppealDoctorChipSelected.copy(alpha = 0.35f),
                    selectedLabelColor = AppealDoctorAccent,
                    containerColor = Color.White.copy(alpha = 0.12f),
                    labelColor = Color.White.copy(alpha = 0.88f)
                )
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
            .aspectRatio(8.5f / 11f),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
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
                    modifier = Modifier.fillMaxWidth()
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 48.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppealActionPillButton(
                    label = EobStrings.t(language, "appealEditLetter"),
                    enabled = !appealLetterEditingEnabled && letterReady,
                    onClick = onEnableEditing
                )
                AppealActionPillButton(
                    label = EobStrings.t(language, "appealSaveLetter"),
                    enabled = appealLetterEditingEnabled,
                    onClick = onSaveLetter
                )
                AppealActionPillButton(
                    label = EobStrings.t(language, "appealCopy"),
                    enabled = letterReady,
                    onClick = onCopy
                )
                AppealActionPillButton(
                    label = EobStrings.t(language, "appealSend"),
                    enabled = letterReady,
                    onClick = onSend
                )
            }
        }

        AppealRegenerateHeroButton(
            label = EobStrings.t(language, "appealRegenerate"),
            onClick = onRegenerate,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
private fun AppealRegenerateHeroButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = AppealHeroGlow.copy(alpha = 0.55f),
                spotColor = AppealHeroGlow.copy(alpha = 0.85f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AppealHeroCyan, AppealHeroTeal)
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AppealActionPillButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) {
        AppealPaperText
    } else {
        AppealPaperText.copy(alpha = 0.38f)
    }

    Surface(
        shape = CircleShape,
        color = AppealPillButtonBackground,
        shadowElevation = 0.dp,
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
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
    appealLetter: String
) {
    if (appealLetter.isBlank()) return
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, EobStrings.t(language, "appealLetter"))
        putExtra(Intent.EXTRA_TEXT, appealLetter)
    }
    context.startActivity(
        Intent.createChooser(sendIntent, EobStrings.t(language, "appealSend"))
    )
}
