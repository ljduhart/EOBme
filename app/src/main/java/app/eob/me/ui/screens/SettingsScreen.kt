package app.eob.me.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.eob.me.data.AccountProfileUiState
import app.eob.me.data.AppLanguage
import app.eob.me.data.AppLockTimeout
import app.eob.me.data.EobLegalUrls
import app.eob.me.data.EobStrings
import app.eob.me.data.HubSettingsState
import app.eob.me.data.ImageCompressionLevel
import app.eob.me.data.SettingsTab
import app.eob.me.util.CacheSizeCalculator

@Composable
fun SettingsScreen(
    language: AppLanguage,
    accountProfileUiState: AccountProfileUiState,
    hubSettings: HubSettingsState,
    appVersionLabel: String,
    onBack: () -> Unit,
    onEnableAccountEditing: () -> Unit,
    onDraftFirstNameChanged: (String) -> Unit,
    onDraftLastNameChanged: (String) -> Unit,
    onSaveAccountProfile: () -> Unit,
    onCancelAccountEditing: () -> Unit,
    onManageSubscription: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    onPinLockToggle: (Boolean) -> Unit,
    onSavePin: (String, String) -> Boolean,
    onAppLockTimeoutSelected: (AppLockTimeout) -> Unit,
    onCrashlyticsToggle: (Boolean) -> Unit,
    onWifiOnlyToggle: (Boolean) -> Unit,
    onCompressionSelected: (ImageCompressionLevel) -> Unit,
    onAutoCropToggle: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onTabSelected: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpfulHintsDialog by remember { mutableStateOf(false) }

    AccountProfileSettingsScaffold(
        language = language,
        selectedTab = hubSettings.selectedTab,
        onBack = onBack,
        onOpenHelpfulHints = { showHelpfulHintsDialog = true },
        onTabSelected = onTabSelected,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (hubSettings.selectedTab) {
                SettingsTab.Account -> AccountProfileSettingsContent(
                    language = language,
                    accountProfileUiState = accountProfileUiState,
                    onEnableAccountEditing = onEnableAccountEditing,
                    onDraftFirstNameChanged = onDraftFirstNameChanged,
                    onDraftLastNameChanged = onDraftLastNameChanged,
                    onSaveAccountProfile = onSaveAccountProfile,
                    onCancelAccountEditing = onCancelAccountEditing,
                    onManageSubscription = onManageSubscription,
                    onLogout = onLogout,
                    onDeleteAccount = { showDeleteDialog = true }
                )
                SettingsTab.Security -> SecuritySettingsTab(
                    language = language,
                    hubSettings = hubSettings,
                    onPinLockToggle = onPinLockToggle,
                    onSavePin = onSavePin,
                    onAppLockTimeoutSelected = onAppLockTimeoutSelected,
                    onCrashlyticsToggle = onCrashlyticsToggle,
                    modifier = Modifier.padding(16.dp)
                )
                SettingsTab.DocumentScan -> DocumentScanSettingsTab(
                    language = language,
                    hubSettings = hubSettings,
                    onWifiOnlyToggle = onWifiOnlyToggle,
                    onCompressionSelected = onCompressionSelected,
                    onAutoCropToggle = onAutoCropToggle,
                    modifier = Modifier.padding(16.dp)
                )
                SettingsTab.Storage -> StorageSettingsTab(
                    language = language,
                    cacheSizeBytes = hubSettings.cacheSizeBytes,
                    onClearCache = onClearCache,
                    modifier = Modifier.padding(16.dp)
                )
                SettingsTab.Legal -> LegalSettingsTab(
                    language = language,
                    appVersionLabel = appVersionLabel,
                    onOpenPrivacy = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(EobLegalUrls.PRIVACY_POLICY))
                        )
                    },
                    onOpenTerms = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(EobLegalUrls.TERMS_OF_USE))
                        )
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
            if (hubSettings.selectedTab != SettingsTab.Account && hubSettings.settingsNotice.isNotBlank()) {
                Text(
                    text = hubSettings.settingsNotice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (showHelpfulHintsDialog) {
        AlertDialog(
            onDismissRequest = { showHelpfulHintsDialog = false },
            title = { Text(EobStrings.t(language, "settingsHelpfulHintsTitle")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        "settingsHelpfulHint1",
                        "settingsHelpfulHint2",
                        "settingsHelpfulHint3",
                        "settingsHelpfulHint4",
                        "settingsHelpfulHint5",
                        "settingsHelpfulHint6",
                        "settingsHelpfulHint7",
                        "settingsHelpfulHint8",
                        "settingsHelpfulHint9",
                        "settingsHelpfulHint10"
                    ).forEachIndexed { index, key ->
                        Text(
                            text = "${index + 1}. ${EobStrings.t(language, key)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpfulHintsDialog = false }) {
                    Text(EobStrings.t(language, "settingsHelpfulHintsClose"))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(EobStrings.t(language, "settingsDeleteAccountTitle")) },
            text = { Text(EobStrings.t(language, "settingsDeleteAccountMessage")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccountConfirmed()
                    }
                ) {
                    Text(EobStrings.t(language, "settingsDeleteAccountConfirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(EobStrings.t(language, "settingsDeleteAccountCancel"))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecuritySettingsTab(
    language: AppLanguage,
    hubSettings: HubSettingsState,
    onPinLockToggle: (Boolean) -> Unit,
    onSavePin: (String, String) -> Boolean,
    onAppLockTimeoutSelected: (AppLockTimeout) -> Unit,
    onCrashlyticsToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDraft by remember { mutableStateOf("") }
    var confirmPinDraft by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(EobStrings.t(language, "settingsSecurityTitle"), style = MaterialTheme.typography.titleLarge)
        if (!hubSettings.pinConfigured) {
            Button(
                onClick = {
                    pinDraft = ""
                    confirmPinDraft = ""
                    showPinDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(EobStrings.t(language, "settingsCreatePin"))
            }
        } else {
            OutlinedButton(
                onClick = {
                    pinDraft = ""
                    confirmPinDraft = ""
                    showPinDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(EobStrings.t(language, "settingsChangePin"))
            }
            SettingsToggleRow(
                label = EobStrings.t(language, "settingsPinLock"),
                checked = hubSettings.pinLockEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !hubSettings.pinConfigured) return@SettingsToggleRow
                    onPinLockToggle(enabled)
                }
            )
        }
        AppLockTimeoutDropdown(
            language = language,
            selectedTimeout = hubSettings.appLockTimeout,
            onTimeoutSelected = onAppLockTimeoutSelected
        )
        SettingsToggleRow(
            label = EobStrings.t(language, "settingsCrashlyticsOptIn"),
            checked = hubSettings.crashlyticsOptIn,
            onCheckedChange = onCrashlyticsToggle
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(
                    EobStrings.t(
                        language,
                        if (hubSettings.pinConfigured) "settingsPinDialogChangeTitle" else "settingsPinDialogTitle"
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinDraft,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() }) {
                                pinDraft = value
                            }
                        },
                        label = { Text(EobStrings.t(language, "settingsPinEntry")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPinDraft,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() }) {
                                confirmPinDraft = value
                            }
                        },
                        label = { Text(EobStrings.t(language, "settingsPinConfirm")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onSavePin(pinDraft, confirmPinDraft)) {
                            pinDraft = ""
                            confirmPinDraft = ""
                            showPinDialog = false
                        }
                    }
                ) {
                    Text(EobStrings.t(language, "settingsPinSave"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(EobStrings.t(language, "cancel"))
                }
            }
        )
    }
}

@Composable
private fun DocumentScanSettingsTab(
    language: AppLanguage,
    hubSettings: HubSettingsState,
    onWifiOnlyToggle: (Boolean) -> Unit,
    onCompressionSelected: (ImageCompressionLevel) -> Unit,
    onAutoCropToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(EobStrings.t(language, "settingsDocumentScanTitle"), style = MaterialTheme.typography.titleLarge)
        SettingsToggleRow(
            label = EobStrings.t(language, "settingsUploadWifiOnly"),
            checked = hubSettings.uploadOverWifiOnly,
            onCheckedChange = onWifiOnlyToggle
        )
        Text(EobStrings.t(language, "settingsImageCompression"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImageCompressionLevel.entries.forEach { level ->
                FilterChip(
                    selected = hubSettings.imageCompressionLevel == level,
                    onClick = { onCompressionSelected(level) },
                    label = { Text(EobStrings.t(language, level.labelKey())) }
                )
            }
        }
        SettingsToggleRow(
            label = EobStrings.t(language, "settingsAutoCrop"),
            checked = hubSettings.autoCropEnabled,
            onCheckedChange = onAutoCropToggle
        )
    }
}

@Composable
private fun StorageSettingsTab(
    language: AppLanguage,
    cacheSizeBytes: Long,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(EobStrings.t(language, "settingsStorageTitle"), style = MaterialTheme.typography.titleLarge)
        SettingsReadOnlyRow(
            language = language,
            label = EobStrings.t(language, "settingsCacheSize"),
            value = CacheSizeCalculator.formatBytes(cacheSizeBytes),
            editable = false,
            onValueChange = {}
        )
        Button(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "settingsClearCache"))
        }
    }
}

@Composable
private fun LegalSettingsTab(
    language: AppLanguage,
    appVersionLabel: String,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(EobStrings.t(language, "settingsLegalTitle"), style = MaterialTheme.typography.titleLarge)
        SettingsLinkRow(
            label = EobStrings.t(language, "privacyPolicy"),
            onClick = onOpenPrivacy
        )
        SettingsLinkRow(
            label = EobStrings.t(language, "termsOfUse"),
            onClick = onOpenTerms
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = appVersionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppLockTimeoutDropdown(
    language: AppLanguage,
    selectedTimeout: AppLockTimeout,
    onTimeoutSelected: (AppLockTimeout) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = EobStrings.t(language, selectedTimeout.labelKey()),
            onValueChange = {},
            readOnly = true,
            label = { Text(EobStrings.t(language, "settingsAppLockTimeout")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLockTimeout.entries.forEach { timeout ->
                DropdownMenuItem(
                    text = { Text(EobStrings.t(language, timeout.labelKey())) },
                    onClick = {
                        onTimeoutSelected(timeout)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsReadOnlyRow(
    language: AppLanguage,
    label: String,
    value: String,
    editable: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        if (editable) {
            androidx.compose.material3.OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            Text(
                text = value.ifBlank { EobStrings.t(language, "valueNotSet") },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsLinkRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}
