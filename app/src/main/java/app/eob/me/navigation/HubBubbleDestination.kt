package app.eob.me.navigation

import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings

/**
 * Six hub launcher bubbles (2 rows × 3 columns) on the main screen.
 */
enum class HubBubbleDestination(
    val route: String,
    val emoji: String
) {
    ProviderDirectory(EobRoute.ProviderDirectory.route, "🏥"),
    EobHistory(EobRoute.History.route, "📋"),
    Appeals(EobRoute.Appeal.route, "✉️"),
    CptTracker(EobRoute.CptCount.route, "🔢"),
    YearlyExpense(EobRoute.YearlyExpense.route, "📊"),
    InsuranceNews(EobRoute.News.route, "📰");

    fun title(language: AppLanguage): String = when (this) {
        ProviderDirectory -> "Provider Directory"
        EobHistory -> EobStrings.t(language, "history")
        Appeals -> EobStrings.t(language, "appeal")
        CptTracker -> EobStrings.t(language, "cptCount")
        YearlyExpense -> "Yearly Expense"
        InsuranceNews -> EobStrings.t(language, "news")
    }

    fun subtitle(language: AppLanguage): String = when (this) {
        ProviderDirectory -> "Care facilities from your EOBs"
        EobHistory -> "Smart card grid & billing detail"
        Appeals -> "Generate appeal letters"
        CptTracker -> "Procedure code analytics"
        YearlyExpense -> "Annual spend breakdown"
        InsuranceNews -> "Carrier alerts & updates"
    }

    companion object {
        val gridRows: List<List<HubBubbleDestination>> = listOf(
            listOf(ProviderDirectory, EobHistory, Appeals),
            listOf(CptTracker, YearlyExpense, InsuranceNews)
        )
    }
}
