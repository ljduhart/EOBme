package app.eob.me.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.billing.PaywallPricing
import app.eob.me.data.AppLanguage
import app.eob.me.data.BillingInterval
import app.eob.me.data.EobStrings
import app.eob.me.data.SubscriptionTier
import app.eob.me.ui.components.SubscriptionTierComparisonPanel

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
    val isDowngrade = currentSubscriptionTier.rank() > selectedTier.rank()
    val canPurchaseSelected = selectedTier != SubscriptionTier.Free && selectedTier != currentSubscriptionTier

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
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
            SubscriptionTierComparisonPanel(
                language = language,
                currentSubscriptionTier = currentSubscriptionTier,
                paywallPricing = paywallPricing,
                isAnnual = isAnnual,
                onAnnualSelected = { isAnnual = it },
                selectedTier = selectedTier,
                onTierSelected = { tier ->
                    selectedTier = tier
                    onTierSelected(tier, billingInterval)
                },
                tierNotice = tierNotice
            )
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
