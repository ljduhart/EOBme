package app.eob.me.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.navigation.HubBottomTab

@Composable
fun HubBottomBar(
    language: AppLanguage,
    selectedTab: HubBottomTab?,
    onTabSelected: (HubBottomTab) -> Unit,
    scanEnabled: Boolean,
    scanLimitReached: Boolean = false
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        HubBottomTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            val scanTabLimited = tab == HubBottomTab.ScanEob && scanLimitReached
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                enabled = tab != HubBottomTab.ScanEob || scanEnabled,
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = EobStrings.t(language, tab.labelKey)
                    )
                },
                label = { Text(EobStrings.t(language, tab.labelKey)) },
                colors = if (scanTabLimited) {
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        selectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                } else {
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            )
        }
    }
}
