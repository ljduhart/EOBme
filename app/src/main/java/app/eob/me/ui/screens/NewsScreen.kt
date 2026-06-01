package app.eob.me.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.InsuranceArticle
import app.eob.me.data.NewsRelease
import app.eob.me.ui.components.HolographicGlassCard
import app.eob.me.ui.components.home.HomeInsuranceNewsSection
import app.eob.me.ui.components.home.InsuranceArticleReaderOverlay
import app.eob.me.ui.components.home.currentNewsYear

@Composable
fun NewsScreen(
    language: AppLanguage,
    insuranceArticles: List<InsuranceArticle>,
    selectedInsuranceArticle: InsuranceArticle?,
    onInsuranceArticleSelected: (InsuranceArticle) -> Unit,
    onDismissInsuranceArticle: () -> Unit,
    newsItems: List<NewsRelease>,
    onDeleteNews: (NewsRelease) -> Unit,
    modifier: Modifier = Modifier
) {
    val readerOpen = selectedInsuranceArticle != null

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .then(if (readerOpen) Modifier.blur(10.dp) else Modifier),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            item {
                HomeInsuranceNewsSection(
                    language = language,
                    articles = insuranceArticles,
                    year = currentNewsYear(),
                    onArticleSelected = onInsuranceArticleSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = EobStrings.t(language, "insuranceIntelligence"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = EobStrings.t(language, "insuranceIntelligenceSubtitle"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = EobStrings.t(language, "insuranceIntelligenceTip"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (newsItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = EobStrings.t(language, "insuranceNewsAllClear"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(newsItems, key = { "${it.company}|${it.headline}|${it.date}" }) { news ->
                    HolographicGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = news.company.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            IconButton(
                                onClick = { onDeleteNews(news) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("❌", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = news.headline,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = news.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = news.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
