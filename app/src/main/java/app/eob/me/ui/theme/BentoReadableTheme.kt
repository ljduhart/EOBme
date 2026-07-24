package app.eob.me.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware surfaces and text for bento cards on Expense Analytics and Account settings.
 * In dark mode, cards use cyber surfaces with neon-blue readable text.
 */
object BentoReadableTheme {
    fun accountCardSurface(darkModeEnabled: Boolean): Color =
        if (darkModeEnabled) EobCyberSurfaceVariant else EobBentoCardSurface

    fun expenseCardSurface(darkModeEnabled: Boolean): Color =
        if (darkModeEnabled) EobCyberSurfaceVariant else EobExpenseBentoSurface

    @Composable
    fun primaryText(darkModeEnabled: Boolean): Color =
        if (darkModeEnabled) EobCyberAccent else MaterialTheme.colorScheme.onSurface

    @Composable
    fun secondaryText(darkModeEnabled: Boolean): Color =
        if (darkModeEnabled) EobCyberAccentBright else MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun emphasisText(darkModeEnabled: Boolean): Color =
        if (darkModeEnabled) EobCyberAccentBright else MaterialTheme.colorScheme.onSurface
}
