package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.UserProfile

@Composable
fun AuthScreen(
    language: AppLanguage,
    profile: UserProfile,
    isSignUp: Boolean,
    authMessage: String,
    modifier: Modifier = Modifier,
    onProfileChanged: (UserProfile) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit
) {
    RegistrationScreen(
        language = language,
        profile = profile,
        isSignUp = isSignUp,
        authMessage = authMessage,
        modifier = modifier,
        onProfileChanged = onProfileChanged,
        onToggleMode = onToggleMode,
        onSubmit = onSubmit
    )
}

@Composable
fun RegistrationScreen(
    language: AppLanguage,
    profile: UserProfile,
    isSignUp: Boolean,
    authMessage: String,
    modifier: Modifier = Modifier,
    onProfileChanged: (UserProfile) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isSignUp) EobStrings.t(language, "profileRequired") else EobStrings.t(language, "login"),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(EobStrings.t(language, "profileRequiredHelp"))
        if (isSignUp) {
            ProfileFields(language, profile, onProfileChanged)
        } else {
            OutlinedTextField(
                value = profile.email,
                onValueChange = { onProfileChanged(profile.copy(email = it)) },
                label = { Text(EobStrings.t(language, "email")) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = profile.password,
                onValueChange = { onProfileChanged(profile.copy(password = it)) },
                label = { Text(EobStrings.t(language, "password")) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (profile.password.isNotBlank() && !profile.isPasswordValid) {
            Text(EobStrings.t(language, "passwordRule"), color = MaterialTheme.colorScheme.error)
        }
        if (authMessage.isNotBlank()) {
            Text(authMessage, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSubmit,
            enabled = if (isSignUp) profile.isComplete else profile.email.isNotBlank() && profile.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) EobStrings.t(language, "createAccount") else EobStrings.t(language, "login"))
        }
        OutlinedButton(onClick = onToggleMode, modifier = Modifier.fillMaxWidth()) {
            Text(if (isSignUp) EobStrings.t(language, "login") else EobStrings.t(language, "createAccount"))
        }
    }
}

@Composable
fun ProfileFields(language: AppLanguage, profile: UserProfile, onProfileChanged: (UserProfile) -> Unit) {
    OutlinedTextField(
        value = profile.firstName,
        onValueChange = { onProfileChanged(profile.copy(firstName = it)) },
        label = { Text(EobStrings.t(language, "firstName")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.lastName,
        onValueChange = { onProfileChanged(profile.copy(lastName = it)) },
        label = { Text(EobStrings.t(language, "lastName")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.email,
        onValueChange = { onProfileChanged(profile.copy(email = it)) },
        label = { Text(EobStrings.t(language, "email")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.password,
        onValueChange = { onProfileChanged(profile.copy(password = it)) },
        label = { Text(EobStrings.t(language, "password")) },
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = profile.city,
            onValueChange = { onProfileChanged(profile.copy(city = it)) },
            label = { Text(EobStrings.t(language, "city")) },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = profile.state,
            onValueChange = { onProfileChanged(profile.copy(state = it)) },
            label = { Text(EobStrings.t(language, "state")) },
            modifier = Modifier.weight(1f)
        )
    }
    OutlinedTextField(
        value = profile.subscriberId,
        onValueChange = { onProfileChanged(profile.copy(subscriberId = it)) },
        label = { Text(EobStrings.t(language, "subscriberId")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.insuranceCardSummary,
        onValueChange = { onProfileChanged(profile.copy(insuranceCardSummary = it)) },
        label = { Text(EobStrings.t(language, "insuranceCardDetails")) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
}
