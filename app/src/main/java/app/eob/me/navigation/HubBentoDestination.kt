package app.eob.me.navigation

import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings

/**
 * Six main-hub bento tiles (3 columns × 2 rows). Routes map to existing feature screens.
 */
enum class HubBentoDestination(
    val route: String,
    val titleKey: String
) {
    ProviderDirectory(EobRoute.ProviderDirectory.route, "bentoProviderDirectory"),
    EobHistory(EobRoute.History.route, "bentoEobHistory"),
    CptTracker(EobRoute.CptCount.route, "bentoCptTracker"),
    YtdExpense(EobRoute.YearlyExpense.route, "bentoYtdExpense"),
    InsuranceNews(EobRoute.News.route, "bentoInsuranceNews"),
    AppealGenerator(EobRoute.Appeal.route, "bentoAppealGenerator");

    fun title(language: AppLanguage): String = EobStrings.t(language, titleKey)

    companion object {
        /** Row 1: Provider Directory, EOB History, CPT Tracker. Row 2: Y-T-D Expense, Insurance News, Appeal Generator. */
        val gridRows: List<List<HubBentoDestination>> = listOf(
            listOf(ProviderDirectory, EobHistory, CptTracker),
            listOf(YtdExpense, InsuranceNews, AppealGenerator)
        )
    }
}
