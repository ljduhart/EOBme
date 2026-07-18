package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.ProfileFieldErrors
import app.eob.me.data.ProfileFormValidator
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile
import app.eob.me.ui.components.LogoutConfirmDialog

@Composable
fun ProfileScreen(
    language: AppLanguage,
    profile: UserProfile,
    credentials: RegistrationCredentials,
    saveMessage: String,
    darkModeEnabled: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    onProfileChanged: (UserProfile) -> Unit,
    onCredentialsChanged: (RegistrationCredentials) -> Unit,
    onEditingChanged: (Boolean) -> Unit = {},
    onSave: (UserProfile, RegistrationCredentials) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    openSupportInitially: Boolean = false
) {
    var showSupport by remember { mutableStateOf(openSupportInitially) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var draftProfile by remember { mutableStateOf(profile) }
    var draftCredentials by remember { mutableStateOf(credentials) }

    var showValidationErrors by remember { mutableStateOf(false) }
    var validationErrors by remember { mutableStateOf(ProfileFieldErrors()) }

    LaunchedEffect(profile, credentials) {
        if (!isEditing) {
            draftProfile = profile
            draftCredentials = credentials
        }
    }

    LaunchedEffect(isEditing) {
        onEditingChanged(isEditing)
    }

    val mergedProfile = draftProfile.copy(
        email = draftCredentials.email.ifBlank { draftProfile.email }
    )

    LaunchedEffect(openSupportInitially) {
        if (openSupportInitially) showSupport = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(EobStrings.t(language, "userProfile"), style = MaterialTheme.typography.titleLarge)
        Text(EobStrings.t(language, "editSavedDetails"))
        ProfileFields(
            language = language,
            profile = mergedProfile,
            onProfileChanged = { draftProfile = it },
            fieldsEnabled = isEditing,
            showEmailField = false,
            fieldErrors = validationErrors,
            showFieldErrors = showValidationErrors
        )
        OutlinedTextField(
            value = draftCredentials.email,
            onValueChange = { draftCredentials = draftCredentials.copy(email = it) },
            label = { Text(EobStrings.t(language, "email")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = !isEditing,
            enabled = isEditing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = showValidationErrors && validationErrors.email != null,
            supportingText = if (showValidationErrors && validationErrors.email != null) {
                { Text(validationErrors.email!!) }
            } else {
                null
            }
        )
        OutlinedTextField(
            value = draftCredentials.password,
            onValueChange = { draftCredentials = draftCredentials.copy(password = it) },
            label = { Text(EobStrings.t(language, "password")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = !isEditing,
            enabled = isEditing,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = showValidationErrors && validationErrors.password != null,
            supportingText = if (showValidationErrors && validationErrors.password != null) {
                { Text(validationErrors.password!!) }
            } else {
                null
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    draftProfile = profile
                    draftCredentials = credentials
                    showValidationErrors = false
                    validationErrors = ProfileFieldErrors()
                    isEditing = true
                },
                modifier = Modifier.weight(1f),
                enabled = !isEditing
            ) {
                Text(EobStrings.t(language, "editProfile"))
            }
            Button(
                onClick = {
                    val errors = ProfileFormValidator.validate(
                        language = language,
                        profile = mergedProfile,
                        credentials = draftCredentials
                    )
                    if (errors.hasErrors) {
                        showValidationErrors = true
                        validationErrors = errors
                        return@Button
                    }
                    val profileToSave = mergedProfile.copy(
                        firstName = mergedProfile.firstName.trim(),
                        lastName = mergedProfile.lastName.trim(),
                        email = mergedProfile.email.trim(),
                        city = mergedProfile.city.trim(),
                        state = mergedProfile.state.trim(),
                        insuranceName = mergedProfile.insuranceName.trim(),
                        insuranceId = mergedProfile.insuranceId.trim(),
                        groupName = mergedProfile.groupName.trim(),
                        pcpCopay = mergedProfile.pcpCopay.trim(),
                        specialistCopay = mergedProfile.specialistCopay.trim()
                    )
                    val credentialsToSave = draftCredentials.copy(
                        email = profileToSave.email,
                        password = draftCredentials.password
                    )
                    onProfileChanged(profileToSave)
                    onCredentialsChanged(credentialsToSave)
                    onSave(profileToSave, credentialsToSave)
                    showValidationErrors = false
                    validationErrors = ProfileFieldErrors()
                    isEditing = false
                },
                modifier = Modifier.weight(1f),
                enabled = isEditing
            ) {
                Text(EobStrings.t(language, "profileSavedButton"))
            }
        }
        if (saveMessage.isNotBlank()) {
            Text(saveMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    EobStrings.t(language, "appearanceSettings"),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    EobStrings.t(language, "darkModeDescription"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = darkModeEnabled,
                onCheckedChange = onDarkModeChanged
            )
        }
        Text(EobStrings.t(language, "languageSettings"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { option ->
                AssistChip(
                    onClick = { onLanguageChanged(option) },
                    label = { Text(option.displayName) },
                    enabled = isEditing && option != language
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showSupport = !showSupport },
            modifier = Modifier.fillMaxWidth(),
            enabled = isEditing
        ) {
            Text(EobStrings.t(language, "support"))
        }
        if (showSupport) {
            SupportContent(language)
        }
        Button(onClick = { showLogoutConfirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "logout"))
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
private fun SupportContent(language: AppLanguage) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(EobStrings.t(language, "support"), style = MaterialTheme.typography.titleLarge)
        Text(EobStrings.t(language, "howToUse"), style = MaterialTheme.typography.titleMedium)
        Text(EobStrings.t(language, "supportStep1"))
        Text(EobStrings.t(language, "supportStep2"))
        Text(EobStrings.t(language, "supportStep3"))
        Text(EobStrings.t(language, "supportStep4"))
        Text(EobStrings.t(language, "supportStep5"))
        Text(EobStrings.t(language, "features"), style = MaterialTheme.typography.titleMedium)
        Text(EobStrings.t(language, "featuresText"))
    }
}
