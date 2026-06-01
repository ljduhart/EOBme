package app.eob.me.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import app.eob.me.ui.components.hubBottomIcons

enum class HubBottomTab(
    val route: String,
    val labelKey: String,
    val icon: ImageVector
) {
    Dashboard(EobRoute.Dashboard.route, "bottomDashboard", hubBottomIcons.Dashboard),
    Profile(EobRoute.Profile.route, "bottomProfile", hubBottomIcons.Profile);

    companion object {
        fun fromRoute(route: String?): HubBottomTab? = when (route) {
            EobRoute.Dashboard.route -> Dashboard
            EobRoute.Profile.route -> Profile
            else -> null
        }
    }
}
