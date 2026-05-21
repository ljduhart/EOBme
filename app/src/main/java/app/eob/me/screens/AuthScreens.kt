package app.eob.me.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.UserProfile
import app.eob.me.localization.Translations

@Composable
fun LanguageScreen(modifier: Modifier = Modifier, onSelected: (AppLanguage) -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("EOBme", style = MaterialTheme.typography.displaySmall)
        Text("Select a language / Seleccione idioma / Choisissez la langue / 选择语言")
        Spacer(Modifier.height(24.dp))
        AppLanguage.entries.forEach { option ->
            Button(
                onClick = { onSelected(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(option.displayName)
            }
        }
    }
}

@Composable
fun IntroScreen(language: AppLanguage, step: Int, modifier: Modifier = Modifier, onNext: () -> Unit) {
    val slides = Translations.intro(language)
    val slide = slides[step]
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(slide.first, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(slide.second)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(if (step == 2) Translations.t(language, "createAccount") else Translations.t(language, "next"))
        }
    }
}

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isSignUp) Translations.t(language, "profileRequired") else Translations.t(language, "login"),
            style = MaterialTheme.typography.headlineSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (isSignUp) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text(Translations.t(language, "createAccount"))
                }
                OutlinedButton(onClick = onToggleMode, modifier = Modifier.weight(1f)) {
                    Text(Translations.t(language, "returningUserSignIn"))
                }
            } else {
                OutlinedButton(onClick = onToggleMode, modifier = Modifier.weight(1f)) {
                    Text(Translations.t(language, "createAccount"))
                }
                Button(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text(Translations.t(language, "returningUserSignIn"))
                }
            }
        }
        if (isSignUp) Text(Translations.t(language, "profileRequiredHelp"))
        if (isSignUp) {
            ProfileFields(language, profile, onProfileChanged)
        } else {
            OutlinedTextField(
                value = profile.email,
                onValueChange = { onProfileChanged(profile.copy(email = it)) },
                label = { Text(Translations.t(language, "email")) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = profile.password,
                onValueChange = { onProfileChanged(profile.copy(password = it)) },
                label = { Text(Translations.t(language, "password")) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (profile.password.isNotBlank() && !profile.isPasswordValid) {
            Text(Translations.t(language, "passwordRule"), color = MaterialTheme.colorScheme.error)
        }
        if (authMessage.isNotBlank()) {
            Text(authMessage, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSubmit,
            enabled = if (isSignUp) profile.isComplete else profile.email.isNotBlank() && profile.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) Translations.t(language, "createAccount") else Translations.t(language, "login"))
        }
    }
}

@Composable
fun EmailVerificationScreen(
    language: AppLanguage,
    email: String,
    verificationCode: String,
    verificationMessage: String,
    modifier: Modifier = Modifier,
    onVerificationCodeChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(Translations.t(language, "verifyEmail"), style = MaterialTheme.typography.headlineSmall)
        Text("${Translations.t(language, "email")}: $email")
        Text(Translations.t(language, "verificationCodeHelp"))
        OutlinedTextField(
            value = verificationCode,
            onValueChange = { onVerificationCodeChanged(it.filter(Char::isDigit).take(6)) },
            label = { Text(Translations.t(language, "verificationCode")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (verificationMessage.isNotBlank()) {
            Text(verificationMessage, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onVerify,
            enabled = verificationCode.length == 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Translations.t(language, "verifyCode"))
        }
        OutlinedButton(onClick = onResend, modifier = Modifier.fillMaxWidth()) {
            Text(Translations.t(language, "resendCode"))
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(Translations.t(language, "backToCreateAccount"))
        }
    }
}

@Composable
fun ProfileFields(language: AppLanguage, profile: UserProfile, onProfileChanged: (UserProfile) -> Unit) {
    OutlinedTextField(
        value = profile.firstName,
        onValueChange = { onProfileChanged(profile.copy(firstName = it)) },
        label = { Text(Translations.t(language, "firstName")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.lastName,
        onValueChange = { onProfileChanged(profile.copy(lastName = it)) },
        label = { Text(Translations.t(language, "lastName")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.email,
        onValueChange = { onProfileChanged(profile.copy(email = it)) },
        label = { Text(Translations.t(language, "email")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.password,
        onValueChange = { onProfileChanged(profile.copy(password = it)) },
        label = { Text(Translations.t(language, "password")) },
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = profile.city,
            onValueChange = { onProfileChanged(profile.copy(city = it)) },
            label = { Text(Translations.t(language, "city")) },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = profile.state,
            onValueChange = { onProfileChanged(profile.copy(state = it)) },
            label = { Text(Translations.t(language, "state")) },
            modifier = Modifier.weight(1f)
        )
    }
    OutlinedTextField(
        value = profile.insuranceName,
        onValueChange = { onProfileChanged(profile.copy(insuranceName = it)) },
        label = { Text(Translations.t(language, "insuranceName")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.insuranceGroupNumber,
        onValueChange = { onProfileChanged(profile.copy(insuranceGroupNumber = it)) },
        label = { Text(Translations.t(language, "insuranceGroupNumber")) },
        modifier = Modifier.fillMaxWidth()
    )
}
