package app.eob.me.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.navigation.HubBottomTab

@Composable
fun HubBottomBar(
    language: AppLanguage,
    selectedTab: HubBottomTab?,
    onTabSelected: (HubBottomTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        HubBottomTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = EobStrings.t(language, tab.labelKey)
                    )
                },
                label = { Text(EobStrings.t(language, tab.labelKey)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            )
        }
    }
}
