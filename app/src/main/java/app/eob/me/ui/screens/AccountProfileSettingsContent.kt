package app.eob.me.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.R
import app.eob.me.data.AccountProfileUiState
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.SettingsTab
import app.eob.me.ui.components.HubSettingsGearIcon
import app.eob.me.ui.components.LogoutConfirmDialog
import app.eob.me.ui.components.SubscriptionTierIcon
import app.eob.me.ui.theme.EobBentoCardSurface
import app.eob.me.ui.theme.EobBrandBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountProfileSettingsScaffold(
    language: AppLanguage,
    selectedTab: SettingsTab,
    onBack: () -> Unit,
    onOpenHelpfulHints: () -> Unit,
    onTabSelected: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.account_profile_app_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.account_profile_back_cd)
                    )
                }
            },
            actions = {
                IconButton(onClick = onOpenHelpfulHints) {
                    Icon(
                        imageVector = HubSettingsGearIcon.Settings,
                        contentDescription = stringResource(R.string.account_profile_settings_cd),
                        tint = EobBrandBlue
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(EobStrings.t(language, tab.labelKey())) }
                )
            }
        }
        content()
    }
}

@Composable
fun AccountProfileSettingsContent(
    language: AppLanguage,
    accountProfileUiState: AccountProfileUiState,
    onEnableAccountEditing: () -> Unit,
    onDraftFirstNameChanged: (String) -> Unit,
    onDraftLastNameChanged: (String) -> Unit,
    onSaveAccountProfile: () -> Unit,
    onCancelAccountEditing: () -> Unit,
    onManageSubscription: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.account_profile_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        ProfileSettingsBentoCard(
            accountProfileUiState = accountProfileUiState,
            onEnableAccountEditing = onEnableAccountEditing,
            onDraftFirstNameChanged = onDraftFirstNameChanged,
            onDraftLastNameChanged = onDraftLastNameChanged,
            onSaveAccountProfile = onSaveAccountProfile,
            onCancelAccountEditing = onCancelAccountEditing
        )
        Text(
            text = stringResource(R.string.account_profile_subscription_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        SubscriptionBentoCard(
            subscriptionTier = accountProfileUiState.subscriptionTier,
            onManageSubscription = onManageSubscription
        )
        AccountActionsBentoCard(
            onLogout = { showLogoutConfirm = true },
            onDeleteAccount = onDeleteAccount
        )
        if (accountProfileUiState.notice.isNotBlank()) {
            Text(
                text = accountProfileUiState.notice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showLogoutConfirm) {
        LogoutConfirmDialog(
            language = language,
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }
}

@Composable
private fun ProfileSettingsBentoCard(
    accountProfileUiState: AccountProfileUiState,
    onEnableAccountEditing: () -> Unit,
    onDraftFirstNameChanged: (String) -> Unit,
    onDraftLastNameChanged: (String) -> Unit,
    onSaveAccountProfile: () -> Unit,
    onCancelAccountEditing: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = EobBentoCardSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = accountProfileUiState.initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (accountProfileUiState.isEditing) {
                            OutlinedTextField(
                                value = accountProfileUiState.draftFirstName,
                                onValueChange = onDraftFirstNameChanged,
                                label = { Text(stringResource(R.string.account_profile_first_name_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = accountProfileUiState.draftLastName,
                                onValueChange = onDraftLastNameChanged,
                                label = { Text(stringResource(R.string.account_profile_last_name_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = accountProfileUiState.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.account_profile_name_caption),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (!accountProfileUiState.isEditing) {
                    IconButton(onClick = onEnableAccountEditing) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.account_profile_edit_cd),
                            tint = EobBrandBlue
                        )
                    }
                }
            }
            if (!accountProfileUiState.isEditing) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = stringResource(R.string.account_profile_email_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = accountProfileUiState.email.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (accountProfileUiState.isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelAccountEditing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.account_profile_cancel))
                    }
                    Button(
                        onClick = onSaveAccountProfile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.account_profile_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionBentoCard(
    subscriptionTier: app.eob.me.data.SubscriptionTier,
    onManageSubscription: () -> Unit
) {
    val tierColor = SubscriptionTierIcon.tintFor(subscriptionTier)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = EobBentoCardSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = SubscriptionTierIcon.iconFor(subscriptionTier),
                        contentDescription = stringResource(R.string.account_profile_tier_icon_cd),
                        tint = tierColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(subscriptionTier.accountProfileLabelRes()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tierColor
                        )
                        Text(
                            text = stringResource(R.string.account_profile_subscription_caption),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalButton(onClick = onManageSubscription) {
                    Text(stringResource(R.string.account_profile_manage_subscription))
                }
            }
            Text(
                text = stringResource(R.string.account_profile_subscription_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountActionsBentoCard(
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = EobBentoCardSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.account_profile_actions_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.account_profile_logout))
            }
            HorizontalDivider()
            Text(
                text = stringResource(R.string.account_profile_danger_zone),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = onDeleteAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.account_profile_delete_account),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
