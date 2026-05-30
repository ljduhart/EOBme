package app.eob.me.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LANGUAGE_STAGGER_MS = 450L
private const val LANGUAGE_SELECT_DELAY_MS = 320L

/**
 * Onboarding language picker: each option appears one at a time, then intro follows selection.
 */
@Composable
fun LanguageScreen(
    modifier: Modifier = Modifier,
    onSelected: (AppLanguage) -> Unit
) {
    val languages = AppLanguage.entries
    var visibleCount by remember { mutableIntStateOf(0) }
    var isTransitioning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        languages.indices.forEach { index ->
            delay(LANGUAGE_STAGGER_MS)
            visibleCount = index + 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select your language",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Seleccione idioma · Choisissez la langue · 选择语言",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(28.dp))

        languages.forEachIndexed { index, option ->
            AnimatedVisibility(
                visible = index < visibleCount,
                enter = fadeIn(animationSpec = tween(350)) +
                    slideInVertically(
                        animationSpec = tween(400),
                        initialOffsetY = { fullHeight -> fullHeight / 3 }
                    )
            ) {
                Button(
                    onClick = {
                        if (isTransitioning) return@Button
                        isTransitioning = true
                        scope.launch {
                            delay(LANGUAGE_SELECT_DELAY_MS)
                            onSelected(option)
                        }
                    },
                    enabled = !isTransitioning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(option.displayName)
                }
            }
        }
    }
}
