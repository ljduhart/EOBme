package app.eob.me.navigation

sealed class EobRoute(val route: String) {
    data object Home : EobRoute("home")
    data object History : EobRoute("history")
    data object Dashboard : EobRoute("dashboard")
    data object YearlyExpense : EobRoute("yearly_expense")
    data object CptCount : EobRoute("cpt_count")
    data object News : EobRoute("news")
    data object Appeal : EobRoute("appeal")
    data object Profile : EobRoute("profile")
    data object CameraCapture : EobRoute("camera_capture")
    data object ProviderDirectory : EobRoute("provider_directory")
}

/** Feature routes opened from the 6-bubble main hub (not including Home, Profile, Camera). */
val hubFeatureRoutes = setOf(
    EobRoute.ProviderDirectory.route,
    EobRoute.History.route,
    EobRoute.Appeal.route,
    EobRoute.CptCount.route,
    EobRoute.YearlyExpense.route,
    EobRoute.News.route
)

/** Routes that show the back-to-home control in the hub header. */
val hubBackRoutes = hubFeatureRoutes + setOf(
    EobRoute.Profile.route
)

/** Routes where the scan FAB is hidden (camera has its own capture UI). */
val hubRoutesWithoutScanFab = setOf(
    EobRoute.CameraCapture.route,
    EobRoute.Profile.route
)
