package app.eob.me.navigation

sealed class EobRoute(val route: String) {
    data object Home : EobRoute("home")
    data object History : EobRoute("history")
    data object Dashboard : EobRoute("dashboard")
    data object CptCount : EobRoute("cpt_count")
    data object News : EobRoute("news")
    data object Appeal : EobRoute("appeal")
    data object Profile : EobRoute("profile")
    data object CameraCapture : EobRoute("camera_capture")
    data object ProviderDirectory : EobRoute("provider_directory")
}

val primaryRoutes = listOf(
    EobRoute.Home,
    EobRoute.History,
    EobRoute.Dashboard,
    EobRoute.CptCount,
    EobRoute.News,
    EobRoute.Appeal
)
