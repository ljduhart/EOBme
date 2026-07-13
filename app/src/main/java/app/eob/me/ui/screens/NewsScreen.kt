package app.eob.me.ui.screens

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.InsuranceBriefingAssets
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

private val InsuranceNewsDarkModeText = Color.Black

@Composable
private fun insuranceNewsReadableTextColor(): Color =
    if (isSystemInDarkTheme()) InsuranceNewsDarkModeText else MaterialTheme.colorScheme.onSurface

@Composable
private fun insuranceNewsTitleColor(): Color =
    if (isSystemInDarkTheme()) InsuranceNewsDarkModeText else MaterialTheme.colorScheme.onBackground

@Composable
private fun newsCardContainerColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else NewsCardBackground

@Composable
private fun infoBannerContainerColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else InfoBannerBackground

@Composable
private fun carrierCardBackground(isSelected: Boolean): Color = when {
    isSelected && isSystemInDarkTheme() ->
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    isSelected -> CarrierSelectedBackground
    isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceVariant
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun carrierCardBorderColor(isSelected: Boolean): Color =
    if (isSelected) CarrierSelectedBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

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
                        color = insuranceNewsReadableTextColor(),
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
                        fontWeight = FontWeight.Bold,
                        color = insuranceNewsTitleColor()
                    )
                    Text(
                        text = EobStrings.t(language, "insuranceIntelligenceSubtitle"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = insuranceNewsReadableTextColor().copy(alpha = 0.88f)
                    )
                }
            }

            item(key = "intelligence_tip") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = infoBannerContainerColor()),
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
                            color = insuranceNewsReadableTextColor()
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
                            color = insuranceNewsReadableTextColor().copy(alpha = 0.78f)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = newsItems,
                    key = { _, news -> "${news.company}|${news.headline}|${news.date}" }
                ) { index, news ->
                    SwipeableNewsBriefingCard(
                        language = language,
                        news = news,
                        showReadMore = index == 0,
                        onDelete = { onDeleteNews(news) },
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

    val backgroundColor = carrierCardBackground(isSelected)
    val border = BorderStroke(
        width = if (isSelected) 2.dp else 1.dp,
        color = carrierCardBorderColor(isSelected)
    )

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
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(InsuranceBriefingAssets.logoResId(item.carrier)),
                    contentDescription = item.carrier.displayName,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = EobStrings.t(language, "insuranceNewsMonthlyBriefingsLabel"),
                    style = MaterialTheme.typography.labelSmall,
                    color = insuranceNewsReadableTextColor().copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
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
    onReadMore: () -> Unit,
    onReadFullBriefing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val articleUrl = news.resolvedArticleUrl()
    val canOpenArticle = articleUrl.isNotBlank()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = newsCardContainerColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            Text(
                text = news.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = insuranceNewsReadableTextColor()
            )

            Text(
                text = news.date,
                style = MaterialTheme.typography.labelSmall,
                color = insuranceNewsReadableTextColor().copy(alpha = 0.78f)
            )

            Text(
                text = news.displaySummary(),
                style = MaterialTheme.typography.bodyMedium,
                color = insuranceNewsReadableTextColor().copy(alpha = 0.9f)
            )

            if (canOpenArticle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNewsBriefingCard(
    language: AppLanguage,
    news: NewsRelease,
    showReadMore: Boolean,
    onDelete: () -> Unit,
    onReadMore: () -> Unit,
    onReadFullBriefing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirm = true
                false
            } else {
                true
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = EobStrings.t(language, "deleteNewsConfirmTitle"),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = EobStrings.t(language, "deleteNewsConfirmMessage"),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text(EobStrings.t(language, "deleteNewsConfirmYes"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(EobStrings.t(language, "deleteNewsConfirmNo"))
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xFFE53935),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = EobStrings.t(language, "deleteNews"),
                        tint = Color.White,
                        modifier = Modifier.padding(end = 24.dp)
                    )
                }
            }
        },
        content = {
            NewsBriefingCard(
                language = language,
                news = news,
                showReadMore = showReadMore,
                onReadMore = onReadMore,
                onReadFullBriefing = onReadFullBriefing
            )
        }
    )
}
