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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier

@Composable
fun PaywallDialog(
    message: String,
    onPurchaseClicked: (SubscriptionTier, BillingInterval) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PaywallScreen(
            message = message,
            onPurchaseClicked = onPurchaseClicked,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaywallScreen(
    message: String,
    onPurchaseClicked: (SubscriptionTier, BillingInterval) -> Unit,
    onDismiss: () -> Unit
) {
    var isAnnual by remember { mutableStateOf(true) }
    var selectedTier by remember { mutableStateOf(SubscriptionTier.Gold) }
    val billingInterval = if (isAnnual) BillingInterval.ANNUAL else BillingInterval.MONTHLY

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Close")
                }
            }
            Text(
                text = "Upgrade EOBme",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            TierSelectorCard(
                title = "Silver Tier",
                price = SubscriptionCatalog.displayPrice(SubscriptionTier.Silver, billingInterval),
                features = SubscriptionCatalog.features(SubscriptionTier.Silver),
                isSelected = selectedTier == SubscriptionTier.Silver,
                onClick = { selectedTier = SubscriptionTier.Silver }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TierSelectorCard(
                title = "Gold Tier",
                price = SubscriptionCatalog.displayPrice(SubscriptionTier.Gold, billingInterval),
                features = SubscriptionCatalog.features(SubscriptionTier.Gold),
                isSelected = selectedTier == SubscriptionTier.Gold,
                isRecommended = true,
                onClick = { selectedTier = SubscriptionTier.Gold }
            )

            Spacer(modifier = Modifier.weight(1f))

            val finalPrice = SubscriptionCatalog.checkoutPrice(selectedTier, billingInterval)

            Button(
                onClick = { onPurchaseClicked(selectedTier, billingInterval) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Subscribe for $finalPrice",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TierSelectorCard(
    title: String,
    price: String,
    features: List<String>,
    isSelected: Boolean,
    isRecommended: Boolean = false,
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
                if (isRecommended) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(
                            text = "RECOMMENDED",
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
            Text(
                text = price,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
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
    }
}
