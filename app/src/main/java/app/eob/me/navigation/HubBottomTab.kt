package app.eob.me.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import app.eob.me.ui.components.hubBottomIcons

enum class HubBottomTab(
    val route: String?,
    val label: String,
    val icon: ImageVector
) {
    Dashboard(EobRoute.Dashboard.route, "Dashboard", hubBottomIcons.Dashboard),
    ScanEob(null, "Scan EOB", hubBottomIcons.ScanEob),
    Profile(EobRoute.Profile.route, "Profile", hubBottomIcons.Profile);

    companion object {
        fun fromRoute(route: String?): HubBottomTab? = when (route) {
            EobRoute.Dashboard.route -> Dashboard
            EobRoute.Profile.route -> Profile
            else -> null
        }
    }
}
