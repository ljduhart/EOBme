package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.UserProfile
import app.eob.me.navigation.HubBubbleDestination
import app.eob.me.ui.components.HubBubbleButton

/**
 * Main hub: 6 navigation bubbles in a 2×3 grid (MVVM view — state from [app.eob.me.viewmodel.EobViewModel]).
 */
@Composable
fun HomeScreen(
    language: AppLanguage,
    profile: UserProfile,
    recordCount: Int,
    firebaseStatusLine: String,
    uploadNotice: String,
    onBubbleSelected: (HubBubbleDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Welcome, ${profile.firstName.ifBlank { "Member" }}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$recordCount ${if (recordCount == 1) "EOB" else "EOBs"} • $firebaseStatusLine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        if (uploadNotice.isNotBlank()) {
            item {
                Text(
                    text = uploadNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HubBubbleDestination.gridRows.forEach { row ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { destination ->
                        HubBubbleButton(
                            emoji = destination.emoji,
                            title = destination.title(language),
                            subtitle = destination.subtitle(language),
                            onClick = { onBubbleSelected(destination) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
