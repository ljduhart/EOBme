package app.eob.me.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.eob.me.billing.PaywallPricing
import app.eob.me.data.AppLanguage
import app.eob.me.data.BillingInterval
import app.eob.me.data.EobStrings
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionTierComparisonPanel(
    language: AppLanguage,
    currentSubscriptionTier: SubscriptionTier,
    paywallPricing: PaywallPricing,
    isAnnual: Boolean,
    onAnnualSelected: (Boolean) -> Unit,
    selectedTier: SubscriptionTier,
    onTierSelected: (SubscriptionTier) -> Unit,
    tierNotice: String,
    modifier: Modifier = Modifier
) {
    val billingInterval = if (isAnnual) BillingInterval.ANNUAL else BillingInterval.MONTHLY
    val scrollState = rememberScrollState()
    val alreadyOwned = selectedTier == currentSubscriptionTier && selectedTier != SubscriptionTier.Free
    val isDowngrade = currentSubscriptionTier.rank() > selectedTier.rank()

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PrimaryTabRow(
            selectedTabIndex = if (isAnnual) 1 else 0,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = !isAnnual,
                onClick = { onAnnualSelected(false) },
                text = { Text(EobStrings.t(language, "billingIntervalMonthly")) }
            )
            Tab(
                selected = isAnnual,
                onClick = { onAnnualSelected(true) },
                text = { Text(EobStrings.t(language, "billingIntervalAnnual")) }
            )
        }

        SubscriptionTierCard(
            title = "Free Tier",
            price = SubscriptionCatalog.displayPrice(SubscriptionTier.Free, billingInterval),
            features = SubscriptionCatalog.features(SubscriptionTier.Free),
            isSelected = false,
            isCurrentPlan = currentSubscriptionTier == SubscriptionTier.Free,
            enabled = false,
            onClick = {}
        )

        SubscriptionTierCard(
            title = "Silver Tier",
            price = paywallPricing.displayPrice(SubscriptionTier.Silver, billingInterval),
            features = SubscriptionCatalog.features(SubscriptionTier.Silver),
            isSelected = selectedTier == SubscriptionTier.Silver,
            isCurrentPlan = currentSubscriptionTier == SubscriptionTier.Silver,
            enabled = currentSubscriptionTier != SubscriptionTier.Silver,
            onClick = { onTierSelected(SubscriptionTier.Silver) }
        )

        SubscriptionGoldTierCard(
            price = paywallPricing.displayPrice(SubscriptionTier.Gold, billingInterval),
            standardFeatures = SubscriptionCatalog.goldStandardFeatures(),
            highlightFeatures = SubscriptionCatalog.goldHighlightFeatures(),
            highlightsTitle = EobStrings.t(language, "billingGoldHighlightsTitle"),
            isSelected = selectedTier == SubscriptionTier.Gold,
            isCurrentPlan = currentSubscriptionTier == SubscriptionTier.Gold,
            enabled = currentSubscriptionTier != SubscriptionTier.Gold,
            onClick = { onTierSelected(SubscriptionTier.Gold) }
        )

        when {
            tierNotice.isNotBlank() -> {
                Text(
                    text = tierNotice,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            alreadyOwned -> {
                Text(
                    text = EobStrings.t(language, "billingAlreadyPurchasedByUser"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            isDowngrade -> {
                Text(
                    text = EobStrings.t(language, "billingDowngradeNextCycle"),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
internal fun SubscriptionTierCard(
    title: String,
    price: String,
    features: List<String>,
    isSelected: Boolean,
    isCurrentPlan: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SubscriptionTierCardHeader(
                title = title,
                isCurrentPlan = isCurrentPlan,
                isRecommended = false
            )
            Text(
                text = price,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            SubscriptionFeatureChecklist(features = features)
        }
    }
}

@Composable
internal fun SubscriptionGoldTierCard(
    price: String,
    standardFeatures: List<String>,
    highlightFeatures: List<String>,
    highlightsTitle: String,
    isSelected: Boolean,
    isCurrentPlan: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SubscriptionTierCardHeader(
                title = "Gold Tier",
                isCurrentPlan = isCurrentPlan,
                isRecommended = true
            )
            Text(
                text = price,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SubscriptionFeatureChecklist(features = standardFeatures)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = highlightsTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SubscriptionFeatureChecklist(features = highlightFeatures)
                }
            }
        }
    }
}

@Composable
private fun SubscriptionTierCardHeader(
    title: String,
    isCurrentPlan: Boolean,
    isRecommended: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        when {
            isCurrentPlan -> {
                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                    Text(text = "CURRENT", modifier = Modifier.padding(4.dp))
                }
            }
            isRecommended -> {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(text = "RECOMMENDED", modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SubscriptionFeatureChecklist(features: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        features.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
