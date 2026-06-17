package app.eob.me.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings

/**
 * Premium bento wrapper: outer [Card] stays sharp; only inner content blurs when locked.
 * Designed for the hub bento second row (Y-T-D, News, Appeal).
 */
@Composable
fun PremiumBentoBox(
    isUnlocked: Boolean,
    onLockedClick: () -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (isUnlocked) {
            content()
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.blur(16.dp)) {
                    content()
                }
                Surface(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onLockedClick),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = EobStrings.t(language, "premiumUpgradeToUnlock"),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
