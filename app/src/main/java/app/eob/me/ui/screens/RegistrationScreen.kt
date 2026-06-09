package app.eob.me.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile

private object LegalAcceptanceUrls {
    const val TERMS_OF_USE = "https://ljduhart.github.io/EOBme/terms-of-use.html"
    const val PRIVACY_POLICY = "https://ljduhart.github.io/EOBme/privacy-policy.html"
}

private const val LEGAL_TERMS_TAG = "terms_of_use"
private const val LEGAL_PRIVACY_TAG = "privacy_policy"

@Composable
fun LegalAcceptanceText(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val termsLabel = EobStrings.t(language, "termsOfUse")
    val privacyLabel = EobStrings.t(language, "privacyPolicy")
    val annotatedText = buildAnnotatedString {
        append(EobStrings.t(language, "legalAcceptancePrefix"))
        pushStringAnnotation(tag = LEGAL_TERMS_TAG, annotation = LegalAcceptanceUrls.TERMS_OF_USE)
        withStyle(SpanStyle(color = linkColor)) {
            append(termsLabel)
        }
        pop()
        append(EobStrings.t(language, "legalAcceptanceMiddle"))
        pushStringAnnotation(tag = LEGAL_PRIVACY_TAG, annotation = LegalAcceptanceUrls.PRIVACY_POLICY)
        withStyle(SpanStyle(color = linkColor)) {
            append(privacyLabel)
        }
        pop()
        append(EobStrings.t(language, "legalAcceptanceSuffix"))
    }
    ClickableText(
        text = annotatedText,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        ),
        modifier = modifier.fillMaxWidth(),
        onClick = { offset ->
            annotatedText.getStringAnnotations(start = offset, end = offset)
                .firstOrNull()
                ?.let { annotation ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
                }
        }
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
    credentials: RegistrationCredentials,
    isSignUp: Boolean,
    awaitingEmailVerification: Boolean = false,
    authMessage: String,
    modifier: Modifier = Modifier,
    onProfileChanged: (UserProfile) -> Unit,
    onCredentialsChanged: (RegistrationCredentials) -> Unit,
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
        credentials = credentials,
        isSignUp = isSignUp,
        authMessage = authMessage,
        modifier = modifier,
        onProfileChanged = onProfileChanged,
        onCredentialsChanged = onCredentialsChanged,
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
        Button(onClick = onResendVerification, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "resendVerification"))
        }
        OutlinedButton(onClick = onRefreshVerification, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "iVerifiedEmail"))
        }
    }
}

@Composable
fun RegistrationScreen(
    language: AppLanguage,
    profile: UserProfile,
    credentials: RegistrationCredentials,
    isSignUp: Boolean,
    authMessage: String,
    modifier: Modifier = Modifier,
    onProfileChanged: (UserProfile) -> Unit,
    onCredentialsChanged: (RegistrationCredentials) -> Unit,
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
            PasswordField(
                language = language,
                password = credentials.password,
                onPasswordChanged = { onCredentialsChanged(credentials.copy(email = profile.email, password = it)) }
            )
        } else {
            CredentialFields(
                language = language,
                credentials = credentials,
                onCredentialsChanged = { updated ->
                    onCredentialsChanged(updated)
                    if (updated.email != profile.email) {
                        onProfileChanged(profile.copy(email = updated.email))
                    }
                }
            )
        }
        if (credentials.password.isNotBlank() && !credentials.isPasswordValid) {
            Text(EobStrings.t(language, "passwordRule"), color = MaterialTheme.colorScheme.error)
        }
        if (authMessage.isNotBlank()) {
            Text(authMessage, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSubmit,
            enabled = if (isSignUp) {
                credentials.isReadyForSignUp(profile)
            } else {
                credentials.isReadyForSignIn()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) EobStrings.t(language, "createAccount") else EobStrings.t(language, "login"))
        }
        if (isSignUp) {
            LegalAcceptanceText(
                language = language,
                modifier = Modifier.padding(top = 16.dp)
            )
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
fun ProfileFields(
    language: AppLanguage,
    profile: UserProfile,
    onProfileChanged: (UserProfile) -> Unit,
    fieldsEnabled: Boolean = true,
    showEmailField: Boolean = true
) {
    OutlinedTextField(
        value = profile.firstName,
        onValueChange = { onProfileChanged(profile.copy(firstName = it)) },
        label = { Text(EobStrings.t(language, "firstName")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled
    )
    OutlinedTextField(
        value = profile.lastName,
        onValueChange = { onProfileChanged(profile.copy(lastName = it)) },
        label = { Text(EobStrings.t(language, "lastName")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled
    )
    if (showEmailField) {
        OutlinedTextField(
            value = profile.email,
            onValueChange = { onProfileChanged(profile.copy(email = it)) },
            label = { Text(EobStrings.t(language, "email")) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !fieldsEnabled,
            enabled = fieldsEnabled
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = profile.city,
            onValueChange = { onProfileChanged(profile.copy(city = it)) },
            label = { Text(EobStrings.t(language, "city")) },
            modifier = Modifier.weight(1f),
            readOnly = !fieldsEnabled,
            enabled = fieldsEnabled
        )
        OutlinedTextField(
            value = profile.state,
            onValueChange = { onProfileChanged(profile.copy(state = it)) },
            label = { Text(EobStrings.t(language, "state")) },
            modifier = Modifier.weight(1f),
            readOnly = !fieldsEnabled,
            enabled = fieldsEnabled
        )
    }
    OutlinedTextField(
        value = profile.insuranceName,
        onValueChange = { onProfileChanged(profile.copy(insuranceName = it)) },
        label = { Text(EobStrings.t(language, "insuranceNameField")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled
    )
    OutlinedTextField(
        value = profile.insuranceId,
        onValueChange = { onProfileChanged(profile.copy(insuranceId = it)) },
        label = { Text(EobStrings.t(language, "insuranceId")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled
    )
    OutlinedTextField(
        value = profile.groupName,
        onValueChange = { onProfileChanged(profile.copy(groupName = it)) },
        label = { Text(EobStrings.t(language, "groupName")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled
    )
    OutlinedTextField(
        value = profile.pcpCopay,
        onValueChange = { onProfileChanged(profile.copy(pcpCopay = it)) },
        label = { Text(EobStrings.t(language, "pcpCopayField")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
    OutlinedTextField(
        value = profile.specialistCopay,
        onValueChange = { onProfileChanged(profile.copy(specialistCopay = it)) },
        label = { Text(EobStrings.t(language, "specialistCopayField")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
    OutlinedTextField(
        value = profile.annualDeductibleLimit.takeIf { it > 0 }?.let { formatPlanAmount(it) }.orEmpty(),
        onValueChange = { value ->
            onProfileChanged(profile.copy(annualDeductibleLimit = value.toDoubleOrNull() ?: 0.0))
        },
        label = { Text(EobStrings.t(language, "annualDeductibleLimit")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
    OutlinedTextField(
        value = profile.annualOutOfPocketMax.takeIf { it > 0 }?.let { formatPlanAmount(it) }.orEmpty(),
        onValueChange = { value ->
            onProfileChanged(profile.copy(annualOutOfPocketMax = value.toDoubleOrNull() ?: 0.0))
        },
        label = { Text(EobStrings.t(language, "annualOutOfPocketMax")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

private fun formatPlanAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
}

@Composable
private fun CredentialFields(
    language: AppLanguage,
    credentials: RegistrationCredentials,
    onCredentialsChanged: (RegistrationCredentials) -> Unit
) {
    OutlinedTextField(
        value = credentials.email,
        onValueChange = { onCredentialsChanged(credentials.copy(email = it)) },
        label = { Text(EobStrings.t(language, "email")) },
        modifier = Modifier.fillMaxWidth()
    )
    PasswordField(
        language = language,
        password = credentials.password,
        onPasswordChanged = { onCredentialsChanged(credentials.copy(password = it)) }
    )
}

@Composable
private fun PasswordField(
    language: AppLanguage,
    password: String,
    onPasswordChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChanged,
        label = { Text(EobStrings.t(language, "password")) },
        modifier = Modifier.fillMaxWidth()
    )
}
