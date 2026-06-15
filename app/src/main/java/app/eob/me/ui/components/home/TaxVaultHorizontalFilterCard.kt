package app.eob.me.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.TaxVaultBudgetSummary
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.asCurrency
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobBrandCyan
import app.eob.me.ui.theme.EobCyberGlassFill
import app.eob.me.ui.theme.EobCyberTextPrimary
import app.eob.me.ui.theme.EobCyberTextSecondary
import app.eob.me.ui.theme.EobLightTextPrimary
import app.eob.me.ui.theme.EobLightTextSecondary

@Composable
fun TaxVaultHorizontalFilterCard(
    language: AppLanguage,
    darkModeEnabled: Boolean,
    filterState: TaxVaultFilterState,
    budgetSummary: TaxVaultBudgetSummary,
    onFilterSelected: (TaxVaultFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryText = if (darkModeEnabled) EobCyberTextPrimary else EobLightTextPrimary
    val secondaryText = if (darkModeEnabled) EobCyberTextSecondary else EobLightTextSecondary
    val cardShape = RoundedCornerShape(18.dp)
    val glassFill = if (darkModeEnabled) {
        EobCyberGlassFill
    } else {
        Color.White.copy(alpha = 0.82f)
    }
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            EobBrandCyan.copy(alpha = 0.45f),
            EobBrandBlue.copy(alpha = 0.18f),
            EobBrandCyan.copy(alpha = 0.28f)
        )
    )
    val statusText = when (filterState) {
        TaxVaultFilterState.OFF -> EobStrings.t(language, "taxVaultStatusOff")
        TaxVaultFilterState.HSA -> EobStrings.t(language, "taxVaultStatusHsa")
        TaxVaultFilterState.FSA -> EobStrings.t(language, "taxVaultStatusFsa")
    }
    val budgetReadout = when (filterState) {
        TaxVaultFilterState.OFF -> EobStrings.t(language, "taxVaultBudgetInactive")
        else -> EobStrings.tf(
            language,
            "taxVaultBudgetReadout",
            budgetSummary.eligibleAmount.asCurrency(),
            budgetSummary.allocationLimit.asCurrency()
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(glassFill)
            .border(width = 1.dp, brush = borderBrush, shape = cardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultGlyph(darkModeEnabled = darkModeEnabled)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = EobStrings.t(language, "taxVaultFilterTitle"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1.2f)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TaxVaultFilterChip(
                label = EobStrings.t(language, "taxVaultFilterOff"),
                selected = filterState == TaxVaultFilterState.OFF,
                darkModeEnabled = darkModeEnabled,
                onClick = { onFilterSelected(TaxVaultFilterState.OFF) }
            )
            TaxVaultFilterChip(
                label = EobStrings.t(language, "taxVaultFilterHsa"),
                selected = filterState == TaxVaultFilterState.HSA,
                darkModeEnabled = darkModeEnabled,
                onClick = { onFilterSelected(TaxVaultFilterState.HSA) }
            )
            TaxVaultFilterChip(
                label = EobStrings.t(language, "taxVaultFilterFsa"),
                selected = filterState == TaxVaultFilterState.FSA,
                darkModeEnabled = darkModeEnabled,
                onClick = { onFilterSelected(TaxVaultFilterState.FSA) }
            )
        }

        Text(
            text = budgetReadout,
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = primaryText,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TaxVaultFilterChip(
    label: String,
    selected: Boolean,
    darkModeEnabled: Boolean,
    onClick: () -> Unit
) {
    val selectedContainer = if (darkModeEnabled) {
        EobBrandBlue.copy(alpha = 0.35f)
    } else {
        EobBrandBlue.copy(alpha = 0.14f)
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedContainer,
            selectedLabelColor = if (darkModeEnabled) EobCyberTextPrimary else EobLightTextPrimary,
            containerColor = Color.Transparent,
            labelColor = if (darkModeEnabled) EobCyberTextSecondary else EobLightTextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = EobBrandCyan.copy(alpha = 0.25f),
            selectedBorderColor = EobBrandCyan.copy(alpha = 0.7f)
        )
    )
}

@Composable
private fun VaultGlyph(darkModeEnabled: Boolean) {
    val vaultBlue = if (darkModeEnabled) Color(0xFF1A4F9C) else Color(0xFF2B6CB0)
    val vaultHighlight = if (darkModeEnabled) Color(0xFF4DA3FF) else Color(0xFF63B3ED)
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(vaultHighlight.copy(alpha = 0.35f), vaultBlue.copy(alpha = 0.9f))
                )
            )
            .border(1.dp, vaultHighlight.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
    }
}
