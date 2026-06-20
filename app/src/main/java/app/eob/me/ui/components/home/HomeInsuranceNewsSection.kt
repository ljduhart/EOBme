package app.eob.me.ui.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.InsuranceArticle
import app.eob.me.data.MajorInsuranceCarrier
import java.util.Calendar

@Composable
fun HomeInsuranceNewsSection(
    language: AppLanguage,
    articles: List<InsuranceArticle>,
    year: Int,
    onArticleSelected: (InsuranceArticle) -> Unit,
    modifier: Modifier = Modifier
) {
    val articlesByCarrier = remember(articles) { articles.groupBy { it.carrier } }
    val currentMonthIndex = remember { Calendar.getInstance().get(Calendar.MONTH) }

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
                text = EobStrings.tf(language, "insuranceNewsTitle", year),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = EobStrings.t(language, "insuranceNewsSubtitle"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            MajorInsuranceCarrier.entries.forEach { carrier ->
                val currentMonthArticle = articlesByCarrier[carrier]
                    ?.firstOrNull { it.monthIndex == currentMonthIndex }
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
                            text = EobStrings.t(language, "insuranceNewsMonthlyBriefings"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        currentMonthArticle?.let { article ->
                            Text(
                                text = article.headline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

fun currentNewsYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
