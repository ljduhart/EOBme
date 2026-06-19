package app.eob.me.ui.screens

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.InsuranceArticle
import app.eob.me.data.InsuranceNewsCarrierHubItem
import app.eob.me.data.MajorInsuranceCarrier
import app.eob.me.data.NewsRelease
import app.eob.me.ui.components.home.InsuranceArticleReaderOverlay

private val CarrierSelectedBackground = Color(0xFFE0F2F1)
private val CarrierSelectedBorder = Color(0xFF00695C)
private val NewsCardBackground = Color(0xFFE0F7FA)
private val InfoBannerBackground = Color(0xFFE0F7FA)
private val PulseDotTeal = Color(0xFF00897B)
private val SourcePillText = Color(0xFFC62828)
private val AmberLightbulb = Color(0xFFFFB300)

fun openCustomTab(context: Context, url: String) {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return
    val uri = Uri.parse(trimmed)
    val scheme = uri.scheme?.lowercase()
    if (scheme != "https" && scheme != "http") return
    CustomTabsIntent.Builder().build().launchUrl(context, uri)
}

@Composable
fun NewsScreen(
    language: AppLanguage,
    carrierHubItems: List<InsuranceNewsCarrierHubItem>,
    selectedCarrier: MajorInsuranceCarrier,
    onCarrierSelected: (MajorInsuranceCarrier) -> Unit,
    selectedInsuranceArticle: InsuranceArticle?,
    onDismissInsuranceArticle: () -> Unit,
    newsItems: List<NewsRelease>,
    onDeleteNews: (NewsRelease) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val readerOpen = selectedInsuranceArticle != null

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (readerOpen) Modifier.blur(10.dp) else Modifier),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            item(key = "carrier_hub") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = carrierHubItems,
                        key = { it.carrier.name }
                    ) { item ->
                        CarrierCard(
                            language = language,
                            item = item,
                            isSelected = item.carrier == selectedCarrier,
                            onClick = { onCarrierSelected(item.carrier) }
                        )
                    }
                }
            }

            item(key = "carrier_filter_hint") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Text(
                        text = EobStrings.t(language, "insuranceNewsCarrierFilterHint"),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            item(key = "intelligence_header") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = EobStrings.t(language, "insuranceIntelligence"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = EobStrings.t(language, "insuranceIntelligenceSubtitle"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "intelligence_tip") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = InfoBannerBackground),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = AmberLightbulb,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = EobStrings.t(language, "insuranceIntelligenceTip"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (newsItems.isEmpty()) {
                item(key = "news_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = EobStrings.t(language, "insuranceNewsAllClear"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = newsItems,
                    key = { _, news -> "${news.company}|${news.headline}|${news.date}" }
                ) { index, news ->
                    NewsBriefingCard(
                        language = language,
                        news = news,
                        showReadMore = index == 0,
                        onDismiss = { onDeleteNews(news) },
                        onReadMore = {
                            val url = news.resolvedArticleUrl()
                            if (url.isNotBlank()) {
                                openCustomTab(context, url)
                            }
                        },
                        onReadFullBriefing = {
                            val url = news.resolvedArticleUrl()
                            if (url.isNotBlank()) {
                                openCustomTab(context, url)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        selectedInsuranceArticle?.let { article ->
            InsuranceArticleReaderOverlay(
                language = language,
                article = article,
                onDismiss = onDismissInsuranceArticle,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun CarrierCard(
    language: AppLanguage,
    item: InsuranceNewsCarrierHubItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "carrier_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val backgroundColor = if (isSelected) {
        CarrierSelectedBackground
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val border = if (isSelected) {
        BorderStroke(2.dp, CarrierSelectedBorder)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    Card(
        modifier = modifier
            .width(108.dp)
            .height(124.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = border
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(10.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(PulseDotTeal)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = carrierIcon(item.carrier),
                    contentDescription = item.carrier.displayName,
                    tint = if (isSelected) CarrierSelectedBorder else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = item.carrier.hubShortName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = EobStrings.tf(language, "insuranceNewsMonthlyBriefingsCount", item.monthlyBriefingCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NewsBriefingCard(
    language: AppLanguage,
    news: NewsRelease,
    showReadMore: Boolean,
    onDismiss: () -> Unit,
    onReadMore: () -> Unit,
    onReadFullBriefing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val articleUrl = news.resolvedArticleUrl()
    val canOpenArticle = articleUrl.isNotBlank()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NewsCardBackground),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White
                ) {
                    Text(
                        text = news.company.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = SourcePillText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = EobStrings.t(language, "deleteNews"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = news.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = news.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = news.displaySummary(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canOpenArticle) {
                    if (showReadMore) {
                        OutlinedButton(
                            onClick = onReadMore,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(EobStrings.t(language, "insuranceNewsReadMore"))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    TextButton(onClick = onReadFullBriefing) {
                        Text(EobStrings.t(language, "insuranceNewsReadFullBriefing"))
                    }
                }
            }
        }
    }
}

private fun carrierIcon(carrier: MajorInsuranceCarrier): ImageVector {
    return when (carrier) {
        MajorInsuranceCarrier.UnitedHealthcare -> Icons.Rounded.HealthAndSafety
        MajorInsuranceCarrier.Medicare -> Icons.Rounded.LocalHospital
        MajorInsuranceCarrier.Aetna -> Icons.Rounded.VolunteerActivism
        MajorInsuranceCarrier.BlueCross -> Icons.Rounded.MedicalServices
        MajorInsuranceCarrier.Medicaid -> Icons.Rounded.People
    }
}
