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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile

@Composable
fun ProfileScreen(
    language: AppLanguage,
    profile: UserProfile,
    credentials: RegistrationCredentials,
    saveMessage: String,
    onProfileChanged: (UserProfile) -> Unit,
    onCredentialsChanged: (RegistrationCredentials) -> Unit,
    onEditingChanged: (Boolean) -> Unit = {},
    onSave: () -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    openSupportInitially: Boolean = false
) {
    var showSupport by remember { mutableStateOf(openSupportInitially) }
    var isEditing by remember { mutableStateOf(false) }
    var draftProfile by remember { mutableStateOf(profile) }
    var draftCredentials by remember { mutableStateOf(credentials) }

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
    val canSave = mergedProfile.isComplete &&
        draftCredentials.email.isNotBlank() &&
        (draftCredentials.password.isBlank() || draftCredentials.isPasswordValid)

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
            showEmailField = false
        )
        OutlinedTextField(
            value = draftCredentials.email,
            onValueChange = { draftCredentials = draftCredentials.copy(email = it) },
            label = { Text(EobStrings.t(language, "email")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = !isEditing,
            enabled = isEditing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        if (isEditing && draftCredentials.password.isNotBlank() && !draftCredentials.isPasswordValid) {
            Text(
                EobStrings.t(language, "passwordRule"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    draftProfile = profile
                    draftCredentials = credentials
                    isEditing = true
                },
                modifier = Modifier.weight(1f),
                enabled = !isEditing
            ) {
                Text(EobStrings.t(language, "editProfile"))
            }
            Button(
                onClick = {
                    val profileToSave = mergedProfile
                    val credentialsToSave = draftCredentials.copy(email = profileToSave.email)
                    onProfileChanged(profileToSave)
                    onCredentialsChanged(credentialsToSave)
                    onSave()
                    isEditing = false
                },
                modifier = Modifier.weight(1f),
                enabled = isEditing && canSave
            ) {
                Text(EobStrings.t(language, "profileSavedButton"))
            }
        }
        if (saveMessage.isNotBlank()) {
            Text(saveMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
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
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "logout"))
        }
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
