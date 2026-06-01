package app.eob.me.ui.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.InsuranceArticle
import app.eob.me.data.MajorInsuranceCarrier
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeInsuranceNewsSection(
    articles: List<InsuranceArticle>,
    year: Int,
    onArticleSelected: (InsuranceArticle) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCarrier by remember { mutableStateOf<MajorInsuranceCarrier?>(null) }
    val articlesByCarrier = remember(articles) { articles.groupBy { it.carrier } }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Insurance news • $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Updates from United Healthcare, Medicare, Aetna, Blue Cross, and Medicaid.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            val currentMonthIndex = remember { Calendar.getInstance().get(Calendar.MONTH) }

            MajorInsuranceCarrier.entries.forEach { carrier ->
                val carrierArticles = articlesByCarrier[carrier].orEmpty()
                val currentMonthArticle = carrierArticles.firstOrNull { it.monthIndex == currentMonthIndex }
                    ?: carrierArticles.minByOrNull { it.monthIndex }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentMonthArticle?.let(onArticleSelected)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = carrier.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap to read • 12 monthly briefings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.clickable {
                                expandedCarrier = if (expandedCarrier == carrier) null else carrier
                            }
                        )
                        if (expandedCarrier == carrier) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                carrierArticles.sortedBy { it.monthIndex }.forEach { article ->
                                    AssistChip(
                                        onClick = { onArticleSelected(article) },
                                        label = { Text(article.monthLabel.take(3)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun currentNewsYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
