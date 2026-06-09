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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.UserProfile

private object AuthLegalUrls {
    const val PRIVACY_POLICY = "https://github.com/ljduhart/EOBme/blob/main/privacy-policy.html"
    const val TERMS_OF_USE = "https://github.com/ljduhart/EOBme/blob/main/terms-of-use.html"
}

@Composable
private fun AuthLegalNotice(language: AppLanguage, modifier: Modifier = Modifier) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    )
    val notice = buildAnnotatedString {
        append(EobStrings.t(language, "authLegalNoticePrefix"))
        withLink(LinkAnnotation.Url(AuthLegalUrls.PRIVACY_POLICY, linkStyle)) {
            append(EobStrings.t(language, "privacyPolicy"))
        }
        append(EobStrings.t(language, "authLegalNoticeMiddle"))
        withLink(LinkAnnotation.Url(AuthLegalUrls.TERMS_OF_USE, linkStyle)) {
            append(EobStrings.t(language, "termsOfUse"))
        }
        append(EobStrings.t(language, "authLegalNoticeSuffix"))
    }
    Text(
        text = notice,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AuthChoiceScreen(
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(EobStrings.t(language, "chooseAccountAction"), style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "createAccount"))
        }
        OutlinedButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "login"))
        }
    }
}

@Composable
fun AuthScreen(
    language: AppLanguage,
    profile: UserProfile,
    isSignUp: Boolean,
    awaitingEmailVerification: Boolean = false,
    authMessage: String,
    modifier: Modifier = Modifier,
    onProfileChanged: (UserProfile) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit = {},
    onForgotUsername: () -> Unit = {},
    onResendVerification: () -> Unit = {},
    onRefreshVerification: () -> Unit = {}
) {
    if (awaitingEmailVerification) {
        EmailVerificationScreen(
            language = language,
            authMessage = authMessage,
            modifier = modifier,
            onResendVerification = onResendVerification,
            onRefreshVerification = onRefreshVerification
        )
        return
    }
    RegistrationScreen(
        language = language,
        profile = profile,
        isSignUp = isSignUp,
        authMessage = authMessage,
        modifier = modifier,
        onProfileChanged = onProfileChanged,
        onToggleMode = onToggleMode,
        onSubmit = onSubmit,
        onForgotPassword = onForgotPassword,
        onForgotUsername = onForgotUsername
    )
}

@Composable
private fun EmailVerificationScreen(
    language: AppLanguage,
    authMessage: String,
    modifier: Modifier = Modifier,
    onResendVerification: () -> Unit,
    onRefreshVerification: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(EobStrings.t(language, "verifyEmailTitle"), style = MaterialTheme.typography.headlineSmall)
        Text(EobStrings.t(language, "verifyEmailHelp"))
        if (authMessage.isNotBlank()) {
            Text(authMessage, color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = onRefreshVerification, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "iVerifiedEmail"))
        }
    }
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
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit = {},
    onForgotUsername: () -> Unit = {}
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
        if (isSignUp) {
            AuthLegalNotice(language = language)
        }
        OutlinedButton(onClick = onToggleMode, modifier = Modifier.fillMaxWidth()) {
            Text(if (isSignUp) EobStrings.t(language, "login") else EobStrings.t(language, "createAccount"))
        }
        if (!isSignUp) {
            OutlinedButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth()) {
                Text(EobStrings.t(language, "forgotPassword"))
            }
            OutlinedButton(onClick = onForgotUsername, modifier = Modifier.fillMaxWidth()) {
                Text(EobStrings.t(language, "forgotUsername"))
            }
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
        value = profile.insuranceName,
        onValueChange = { onProfileChanged(profile.copy(insuranceName = it)) },
        label = { Text(EobStrings.t(language, "insuranceNameField")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.insuranceId,
        onValueChange = { onProfileChanged(profile.copy(insuranceId = it)) },
        label = { Text(EobStrings.t(language, "insuranceId")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.groupName,
        onValueChange = { onProfileChanged(profile.copy(groupName = it)) },
        label = { Text(EobStrings.t(language, "groupName")) },
        modifier = Modifier.fillMaxWidth()
    )
}
