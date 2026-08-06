package app.eob.me.ui.components.dx

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.dx.CptCategory
import app.eob.me.data.dx.DxCptEntry
import app.eob.me.data.dx.ReverseDxSearchState
import app.eob.me.data.dx.ReverseDxUiState
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandGlow
import app.eob.me.ui.theme.EobCyberBackgroundDeep
import app.eob.me.ui.theme.EobCyberTextPrimary
import app.eob.me.ui.theme.EobCyberTextSecondary

private val SheetBackground = EobCyberBackgroundDeep
private val BentoSurface = Color(0xFF0D1B2E)
private val SilverBorder = Color(0xFFC0C8D4)
private val GlowAccent = EobBrandGlow.copy(alpha = 0.55f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverseDxCptBottomSheet(
    language: AppLanguage,
    visible: Boolean,
    state: ReverseDxUiState,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onLaunchScannerClicked: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    if (!visible) return

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 640.dp)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = EobStrings.t(language, "reverseDxSheetTitle"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = EobCyberTextPrimary
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(EobStrings.t(language, "reverseDxSearchLabel")) },
                placeholder = { Text(EobStrings.t(language, "reverseDxSearchHint")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = EobCyberTextPrimary,
                    unfocusedTextColor = EobCyberTextPrimary,
                    focusedBorderColor = GlowAccent,
                    unfocusedBorderColor = SilverBorder.copy(alpha = 0.65f),
                    focusedLabelColor = EobCyberTextSecondary,
                    unfocusedLabelColor = EobCyberTextSecondary,
                    cursorColor = GlowAccent
                ),
                shape = RoundedCornerShape(14.dp)
            )

            when (val searchState = state.searchState) {
                ReverseDxSearchState.Idle -> {
                    Text(
                        text = EobStrings.t(language, "reverseDxIdleHint"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EobCyberTextSecondary
                    )
                }
                ReverseDxSearchState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GlowAccent)
                    }
                }
                is ReverseDxSearchState.Results -> {
                    DxResultsHeader(language, searchState.entry)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(searchState.entry.categories, key = { it.name }) { category ->
                            CptCategoryBentoCard(language, category)
                        }
                    }
                }
                is ReverseDxSearchState.ThresholdExceeded -> {
                    ThresholdOrNotFoundContent(
                        language = language,
                        header = EobStrings.tf(
                            language,
                            "reverseDxHeaderFormat",
                            searchState.entry.dxCode,
                            searchState.entry.description
                        ),
                        warning = EobStrings.t(language, "reverseDxThresholdWarning"),
                        categories = searchState.entry.categories.take(3),
                        onLaunchScannerClicked = onLaunchScannerClicked
                    )
                }
                is ReverseDxSearchState.NotFound -> {
                    ThresholdOrNotFoundContent(
                        language = language,
                        header = EobStrings.tf(language, "reverseDxNotFoundHeaderFormat", searchState.query),
                        warning = EobStrings.t(language, "reverseDxNotFoundWarning"),
                        categories = emptyList(),
                        onLaunchScannerClicked = onLaunchScannerClicked
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DxResultsHeader(language: AppLanguage, entry: DxCptEntry) {
    Text(
        text = EobStrings.tf(language, "reverseDxHeaderFormat", entry.dxCode, entry.description),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = EobCyberTextPrimary
    )
    Text(
        text = EobStrings.tf(language, "reverseDxMatchCountFormat", entry.totalPotentialMatches),
        style = MaterialTheme.typography.bodySmall,
        color = EobCyberTextSecondary
    )
}

@Composable
private fun CptCategoryBentoCard(language: AppLanguage, category: CptCategory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = GlowAccent, spotColor = GlowAccent)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BentoSurface, BentoSurface.copy(alpha = 0.92f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(BorderStroke(1.5.dp, SilverBorder.copy(alpha = 0.85f)), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = EobCyberTextPrimary
        )
        Text(
            text = EobStrings.tf(language, "reverseDxCptRangeFormat", category.range),
            style = MaterialTheme.typography.bodyMedium,
            color = EobCyberTextSecondary
        )
    }
}

@Composable
private fun ThresholdOrNotFoundContent(
    language: AppLanguage,
    header: String,
    warning: String,
    categories: List<CptCategory>,
    onLaunchScannerClicked: () -> Unit
) {
    Text(
        text = header,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = EobCyberTextPrimary
    )
    Text(
        text = warning,
        style = MaterialTheme.typography.bodyMedium,
        color = EobCyberTextSecondary
    )
    if (categories.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categories.forEach { category ->
                Column(
                    modifier = Modifier
                        .width(148.dp)
                        .background(BentoSurface, RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.dp, SilverBorder.copy(alpha = 0.75f)), RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = EobCyberTextPrimary,
                        maxLines = 2
                    )
                    Text(
                        text = category.range,
                        style = MaterialTheme.typography.labelSmall,
                        color = EobCyberTextSecondary,
                        maxLines = 2
                    )
                }
            }
        }
    }
    ScannerCtaButton(language = language, onClick = onLaunchScannerClicked)
}

@Composable
private fun ScannerCtaButton(language: AppLanguage, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = GlowAccent, spotColor = GlowAccent),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EobBrandBlue)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = EobStrings.t(language, "reverseDxScannerCtaTitle"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = EobStrings.t(language, "reverseDxScannerCtaSubtitle"),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}
