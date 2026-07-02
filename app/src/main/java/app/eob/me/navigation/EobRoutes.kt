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
    data object Settings : EobRoute("settings")
    data object ManageSubscription : EobRoute("manage_subscription")
    data object CameraCapture : EobRoute("camera_capture")
    data object TaxVault : EobRoute("tax_vault")
    data object ProviderDirectory : EobRoute("provider_directory")
}

/** Feature routes opened from the bento hub (not including Home, Profile, Camera). */
val hubFeatureRoutes = setOf(
    EobRoute.ProviderDirectory.route,
    EobRoute.History.route,
    EobRoute.Appeal.route,
    EobRoute.CptCount.route,
    EobRoute.YearlyExpense.route,
    EobRoute.News.route,
    EobRoute.TaxVault.route
)

/** Routes that show the back-to-home control in the hub header. */
val hubBackRoutes = hubFeatureRoutes + setOf(
    EobRoute.Dashboard.route,
    EobRoute.Profile.route,
    EobRoute.Settings.route,
    EobRoute.ManageSubscription.route,
    EobRoute.TaxVault.route
)

/** Routes where the bottom navigation bar and hub header are hidden (full-screen camera). */
val hubRoutesWithoutBottomBar = setOf(
    EobRoute.CameraCapture.route
)
