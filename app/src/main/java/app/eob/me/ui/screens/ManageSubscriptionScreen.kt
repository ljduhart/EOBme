package app.eob.me.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun ManageSubscriptionScreen(
    language: AppLanguage,
    currentSubscriptionTier: SubscriptionTier,
    paywallPricing: PaywallPricing,
    tierNotice: String,
    showSubscribeAction: Boolean,
    showCancelSubscriptionAction: Boolean,
    showResubscribeAction: Boolean,
    onTierSelected: (SubscriptionTier, BillingInterval) -> Unit,
    onSubscribeSelectedTier: (SubscriptionTier, BillingInterval) -> Unit,
    onCancelSubscription: () -> Unit,
    onResubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnnual by remember { mutableStateOf(true) }
    var selectedTier by remember(currentSubscriptionTier) {
        mutableStateOf(
            when (currentSubscriptionTier) {
                SubscriptionTier.Gold -> SubscriptionTier.Gold
                SubscriptionTier.Silver -> SubscriptionTier.Silver
                SubscriptionTier.Free -> SubscriptionTier.Silver
            }
        )
    }
    val billingInterval = if (isAnnual) BillingInterval.ANNUAL else BillingInterval.MONTHLY
    val scrollState = rememberScrollState()
    val alreadyOwned = selectedTier == currentSubscriptionTier && selectedTier != SubscriptionTier.Free
    val isDowngrade = currentSubscriptionTier.rank() > selectedTier.rank()
    val canPurchaseSelected = selectedTier != SubscriptionTier.Free && selectedTier != currentSubscriptionTier

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = EobStrings.t(language, "billingManageSubscriptionPageTitle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = EobStrings.t(language, "billingManageSubscriptionHint"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrimaryTabRow(
                selectedTabIndex = if (isAnnual) 1 else 0,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = !isAnnual,
                    onClick = { isAnnual = false },
                    text = { Text("Monthly") }
                )
                Tab(
                    selected = isAnnual,
                    onClick = { isAnnual = true },
                    text = { Text("Annual (Save up to 25%)") }
                )
            }

            ManageTierCard(
                title = "Free Tier",
                price = SubscriptionCatalog.displayPrice(SubscriptionTier.Free, billingInterval),
                features = SubscriptionCatalog.features(SubscriptionTier.Free),
                isSelected = false,
                isCurrentPlan = currentSubscriptionTier == SubscriptionTier.Free,
                enabled = false,
                onClick = {}
            )

            ManageTierCard(
                title = "Silver Tier",
                price = paywallPricing.displayPrice(SubscriptionTier.Silver, billingInterval),
                features = SubscriptionCatalog.features(SubscriptionTier.Silver),
                isSelected = selectedTier == SubscriptionTier.Silver,
                isCurrentPlan = currentSubscriptionTier == SubscriptionTier.Silver,
                enabled = true,
                onClick = {
                    selectedTier = SubscriptionTier.Silver
                    onTierSelected(SubscriptionTier.Silver, billingInterval)
                }
            )

            ManageGoldTierCard(
                price = paywallPricing.displayPrice(SubscriptionTier.Gold, billingInterval),
                standardFeatures = SubscriptionCatalog.goldStandardFeatures(),
                highlightFeatures = SubscriptionCatalog.goldHighlightFeatures(),
                highlightsTitle = EobStrings.t(language, "billingGoldHighlightsTitle"),
                isSelected = selectedTier == SubscriptionTier.Gold,
                isCurrentPlan = currentSubscriptionTier == SubscriptionTier.Gold,
                onClick = {
                    selectedTier = SubscriptionTier.Gold
                    onTierSelected(SubscriptionTier.Gold, billingInterval)
                }
            )

            if (tierNotice.isNotBlank()) {
                Text(
                    text = tierNotice,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (alreadyOwned) {
                Text(
                    text = EobStrings.t(language, "billingAlreadyPurchasedByUser"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (isDowngrade) {
                Text(
                    text = EobStrings.t(language, "billingDowngradeNextCycle"),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (canPurchaseSelected) {
                val checkoutPrice = paywallPricing.checkoutPrice(selectedTier, billingInterval)
                val subscribeLabel = when {
                    isDowngrade -> EobStrings.t(language, "billingChangePlan")
                    showSubscribeAction -> "${EobStrings.t(language, "billingSubscribe")} · $checkoutPrice"
                    else -> "${EobStrings.t(language, "billingChangePlan")} · $checkoutPrice"
                }
                Button(
                    onClick = { onSubscribeSelectedTier(selectedTier, billingInterval) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = subscribeLabel, fontWeight = FontWeight.SemiBold)
                }
            }

            if (showResubscribeAction) {
                Button(
                    onClick = onResubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = EobStrings.t(language, "billingResubscribe"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (showCancelSubscriptionAction) {
                OutlinedButton(
                    onClick = onCancelSubscription,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = EobStrings.t(language, "billingCancelSubscription"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = EobStrings.t(language, "billingCancelSubscriptionHint"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ManageTierCard(
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
            TierCardHeader(title = title, isCurrentPlan = isCurrentPlan, isRecommended = false)
            Text(
                text = price,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            FeatureChecklist(features = features)
        }
    }
}

@Composable
private fun ManageGoldTierCard(
    price: String,
    standardFeatures: List<String>,
    highlightFeatures: List<String>,
    highlightsTitle: String,
    isSelected: Boolean,
    isCurrentPlan: Boolean,
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
            .clickable(onClick = onClick),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TierCardHeader(title = "Gold Tier", isCurrentPlan = isCurrentPlan, isRecommended = true)
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
                    FeatureChecklist(features = standardFeatures)
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
                    FeatureChecklist(features = highlightFeatures)
                }
            }
        }
    }
}

@Composable
private fun TierCardHeader(
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
private fun FeatureChecklist(features: List<String>) {
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
