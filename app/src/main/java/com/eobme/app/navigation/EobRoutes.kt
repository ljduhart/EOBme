package app.eob.me.navigation

sealed class EobRoute(val route: String) {
    data object Home : EobRoute("home")
    data object Analysis : EobRoute("analysis")
    data object CptCount : EobRoute("cpt_count")
    data object News : EobRoute("news")
    data object Appeal : EobRoute("appeal")
    data object Profile : EobRoute("profile")
}

val primaryRoutes = listOf(
    EobRoute.Home,
    EobRoute.Analysis,
    EobRoute.CptCount,
    EobRoute.News,
    EobRoute.Appeal
)
