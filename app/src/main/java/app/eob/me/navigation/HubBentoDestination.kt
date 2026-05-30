package app.eob.me.navigation

/**
 * Six main-hub bento tiles (3 columns × 2 rows). Routes map to existing feature screens.
 */
enum class HubBentoDestination(
    val route: String,
    val title: String
) {
    ProviderDirectory(EobRoute.ProviderDirectory.route, "Provider Directory"),
    EobHistory(EobRoute.History.route, "EOB History"),
    CptTracker(EobRoute.CptCount.route, "CPT Tracker"),
    YtdExpense(EobRoute.YearlyExpense.route, "Y-T-D Expense"),
    InsuranceNews(EobRoute.News.route, "Insurance News"),
    AppealGenerator(EobRoute.Appeal.route, "Appeal Generator");

    companion object {
        /** Row 1: Provider Directory, EOB History, CPT Tracker. Row 2: Y-T-D Expense, Insurance News, Appeal Generator. */
        val gridRows: List<List<HubBentoDestination>> = listOf(
            listOf(ProviderDirectory, EobHistory, CptTracker),
            listOf(YtdExpense, InsuranceNews, AppealGenerator)
        )
    }
}
