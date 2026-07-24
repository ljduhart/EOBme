package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.eob.me.billing.PaywallPricing
import app.eob.me.data.AppLanguage
import app.eob.me.data.BillingInterval
import app.eob.me.data.EobStrings
import app.eob.me.data.SubscriptionTier
import app.eob.me.ui.components.SubscriptionTierComparisonPanel

@Composable
fun PaywallDialog(
    language: AppLanguage,
    message: String,
    currentSubscriptionTier: SubscriptionTier,
    paywallPricing: PaywallPricing,
    restorePurchasesLabel: String,
    alreadySubscribedLabel: String,
    onPurchaseClicked: (SubscriptionTier, BillingInterval) -> Unit,
    onRestorePurchasesClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PaywallScreen(
            language = language,
            message = message,
            currentSubscriptionTier = currentSubscriptionTier,
            paywallPricing = paywallPricing,
            restorePurchasesLabel = restorePurchasesLabel,
            alreadySubscribedLabel = alreadySubscribedLabel,
            onPurchaseClicked = onPurchaseClicked,
            onRestorePurchasesClicked = onRestorePurchasesClicked,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaywallScreen(
    language: AppLanguage,
    message: String,
    currentSubscriptionTier: SubscriptionTier,
    paywallPricing: PaywallPricing,
    restorePurchasesLabel: String,
    alreadySubscribedLabel: String,
    onPurchaseClicked: (SubscriptionTier, BillingInterval) -> Unit,
    onRestorePurchasesClicked: () -> Unit,
    onDismiss: () -> Unit
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
    val purchaseBlocked = selectedTier == SubscriptionTier.Free ||
        selectedTier == currentSubscriptionTier
    val isDowngrade = currentSubscriptionTier.rank() > selectedTier.rank()
    val contextMessage = when {
        message.isNotBlank() -> message
        isDowngrade && !purchaseBlocked -> EobStrings.t(language, "billingDowngradeNextCycle")
        else -> ""
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(text = EobStrings.t(language, "billingPaywallClose"))
                }
            }
            Text(
                text = EobStrings.t(language, "billingPaywallTitle"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = EobStrings.t(language, "billingManageSubscriptionHint"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            if (contextMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contextMessage,
                    color = if (message.isNotBlank()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SubscriptionTierComparisonPanel(
                language = language,
                currentSubscriptionTier = currentSubscriptionTier,
                paywallPricing = paywallPricing,
                isAnnual = isAnnual,
                onAnnualSelected = { isAnnual = it },
                selectedTier = selectedTier,
                onTierSelected = { selectedTier = it },
                tierNotice = "",
                modifier = Modifier.weight(1f)
            )

            val finalPrice = paywallPricing.checkoutPrice(selectedTier, billingInterval)

            Button(
                onClick = {
                    if (!purchaseBlocked) {
                        onPurchaseClicked(selectedTier, billingInterval)
                    }
                },
                enabled = !purchaseBlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        purchaseBlocked -> alreadySubscribedLabel
                        isDowngrade -> EobStrings.t(language, "billingChangePlan")
                        else -> EobStrings.tf(language, "billingSubscribeForPrice", finalPrice)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onRestorePurchasesClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = restorePurchasesLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
