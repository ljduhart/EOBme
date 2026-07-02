package app.eob.me.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.AuthRecoveryFlow
import app.eob.me.data.EobLegalUrls
import app.eob.me.data.EobStrings
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile

@Composable
fun LegalAcceptanceText(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    )
    val termsLabel = EobStrings.t(language, "termsOfUse")
    val privacyLabel = EobStrings.t(language, "privacyPolicy")
    val annotatedText = buildAnnotatedString {
        append(EobStrings.t(language, "legalAcceptancePrefix"))
        withLink(
            LinkAnnotation.Url(
                url = EobLegalUrls.TERMS_OF_USE,
                styles = linkStyles,
                linkInteractionListener = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(EobLegalUrls.TERMS_OF_USE))
                    )
                }
            )
        ) {
            append(termsLabel)
        }
        append(EobStrings.t(language, "legalAcceptanceMiddle"))
        withLink(
            LinkAnnotation.Url(
                url = EobLegalUrls.PRIVACY_POLICY,
                styles = linkStyles,
                linkInteractionListener = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(EobLegalUrls.PRIVACY_POLICY))
                    )
                }
            )
        ) {
            append(privacyLabel)
        }
        append(EobStrings.t(language, "legalAcceptanceSuffix"))
    }
    Text(
        text = annotatedText,
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
    credentials: RegistrationCredentials,
    isSignUp: Boolean,
    signupTermsAccepted: Boolean,
    authRecoveryFlow: AuthRecoveryFlow,
    passwordResetEmail: String,
    passwordResetCode: String,
    passwordResetDraft: String,
    awaitingEmailVerification: Boolean = false,
    authMessage: String,
    authMessageIsError: Boolean = true,
    modifier: Modifier = Modifier,
    onProfileChanged: (UserProfile) -> Unit,
    onCredentialsChanged: (RegistrationCredentials) -> Unit,
    onSignupTermsAcceptedChanged: (Boolean) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit = {},
    onForgotUsername: () -> Unit = {},
    onCancelAuthRecovery: () -> Unit = {},
    onBackFromPasswordVerify: () -> Unit = {},
    onSendForgotUsername: (String) -> Unit = {},
    onPasswordResetEmailChanged: (String) -> Unit = {},
    onRequestPasswordResetCode: () -> Unit = {},
    onPasswordResetCodeChanged: (String) -> Unit = {},
    onPasswordResetDraftChanged: (String) -> Unit = {},
    onConfirmPasswordReset: () -> Unit = {},
    onResendVerification: () -> Unit = {},
    onRefreshVerification: () -> Unit = {}
) {
    if (awaitingEmailVerification) {
        EmailVerificationScreen(
            language = language,
            authMessage = authMessage,
            authMessageIsError = authMessageIsError,
            modifier = modifier,
            onResendVerification = onResendVerification,
            onRefreshVerification = onRefreshVerification
        )
        return
    }
    BackHandler(enabled = authRecoveryFlow == AuthRecoveryFlow.ForgotPasswordVerify) {
        onBackFromPasswordVerify()
    }
    BackHandler(
        enabled = authRecoveryFlow == AuthRecoveryFlow.ForgotUsername ||
            authRecoveryFlow == AuthRecoveryFlow.ForgotPasswordEmail
    ) {
        onCancelAuthRecovery()
    }
    BackHandler(enabled = authRecoveryFlow == AuthRecoveryFlow.None) {
        onToggleMode()
    }
    when (authRecoveryFlow) {
        AuthRecoveryFlow.ForgotUsername -> ForgotUsernameScreen(
            language = language,
            email = passwordResetEmail.ifBlank { credentials.email },
            authMessage = authMessage,
            authMessageIsError = authMessageIsError,
            modifier = modifier,
            onEmailChanged = onPasswordResetEmailChanged,
            onSendUsername = onSendForgotUsername,
            onBack = onCancelAuthRecovery
        )
        AuthRecoveryFlow.ForgotPasswordEmail -> ForgotPasswordEmailScreen(
            language = language,
            email = passwordResetEmail,
            authMessage = authMessage,
            authMessageIsError = authMessageIsError,
            modifier = modifier,
            onEmailChanged = onPasswordResetEmailChanged,
            onSendResetCode = onRequestPasswordResetCode,
            onBack = onCancelAuthRecovery
        )
        AuthRecoveryFlow.ForgotPasswordVerify -> ForgotPasswordVerifyScreen(
            language = language,
            email = passwordResetEmail,
            resetCode = passwordResetCode,
            newPassword = passwordResetDraft,
            authMessage = authMessage,
            authMessageIsError = authMessageIsError,
            modifier = modifier,
            onResetCodeChanged = onPasswordResetCodeChanged,
            onNewPasswordChanged = onPasswordResetDraftChanged,
            onUpdatePassword = onConfirmPasswordReset,
            onResendResetCode = onRequestPasswordResetCode,
            onBack = onBackFromPasswordVerify,
            onCancel = onCancelAuthRecovery
        )
        AuthRecoveryFlow.None -> RegistrationScreen(
            language = language,
            profile = profile,
            credentials = credentials,
            isSignUp = isSignUp,
            signupTermsAccepted = signupTermsAccepted,
            authMessage = authMessage,
            authMessageIsError = authMessageIsError,
            modifier = modifier,
            onProfileChanged = onProfileChanged,
            onCredentialsChanged = onCredentialsChanged,
            onSignupTermsAcceptedChanged = onSignupTermsAcceptedChanged,
            onToggleMode = onToggleMode,
            onSubmit = onSubmit,
            onForgotPassword = onForgotPassword,
            onForgotUsername = onForgotUsername
        )
    }
}

@Composable
private fun EmailVerificationScreen(
    language: AppLanguage,
    authMessage: String,
    authMessageIsError: Boolean,
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
            Text(
                authMessage,
                color = if (authMessageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
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
    signupTermsAccepted: Boolean,
    authMessage: String,
    authMessageIsError: Boolean,
    modifier: Modifier = Modifier,
    onProfileChanged: (UserProfile) -> Unit,
    onCredentialsChanged: (RegistrationCredentials) -> Unit,
    onSignupTermsAcceptedChanged: (Boolean) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit = {},
    onForgotUsername: () -> Unit = {}
) {
    val signupFieldsEnabled = !isSignUp || signupTermsAccepted
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
        Text(
            if (isSignUp) {
                EobStrings.t(language, "signupTermsGateHelp")
            } else {
                EobStrings.t(language, "profileRequiredHelp")
            }
        )
        if (isSignUp) {
            SignupTermsGate(
                language = language,
                accepted = signupTermsAccepted,
                onAcceptedChanged = onSignupTermsAcceptedChanged
            )
        }
        if (isSignUp) {
            ProfileFields(
                language = language,
                profile = profile,
                onProfileChanged = onProfileChanged,
                fieldsEnabled = signupFieldsEnabled
            )
            PasswordField(
                language = language,
                password = credentials.password,
                enabled = signupFieldsEnabled,
                onPasswordChanged = {
                    onCredentialsChanged(credentials.copy(email = profile.email, password = it))
                }
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
            Text(
                authMessage,
                color = if (authMessageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        Button(
            onClick = onSubmit,
            enabled = if (isSignUp) {
                signupTermsAccepted && credentials.isReadyForSignUp(profile)
            } else {
                credentials.isReadyForSignIn()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) EobStrings.t(language, "createAccount") else EobStrings.t(language, "login"))
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
private fun SignupTermsGate(
    language: AppLanguage,
    accepted: Boolean,
    onAcceptedChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = EobStrings.t(language, "signupTermsGateTitle"),
            style = MaterialTheme.typography.titleMedium
        )
        LegalAcceptanceText(language = language)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = onAcceptedChanged
            )
            Text(
                text = EobStrings.t(language, "signupTermsAcceptLabel"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ForgotUsernameScreen(
    language: AppLanguage,
    email: String,
    authMessage: String,
    authMessageIsError: Boolean,
    modifier: Modifier = Modifier,
    onEmailChanged: (String) -> Unit,
    onSendUsername: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(EobStrings.t(language, "forgotUsernameTitle"), style = MaterialTheme.typography.headlineSmall)
        Text(EobStrings.t(language, "forgotUsernameHelp"))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text(EobStrings.t(language, "email")) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        if (authMessage.isNotBlank()) {
            Text(
                authMessage,
                color = if (authMessageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        Button(
            onClick = { onSendUsername(email) },
            enabled = email.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(EobStrings.t(language, "sendUsername"))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "backToSignIn"))
        }
    }
}

@Composable
private fun ForgotPasswordEmailScreen(
    language: AppLanguage,
    email: String,
    authMessage: String,
    authMessageIsError: Boolean,
    modifier: Modifier = Modifier,
    onEmailChanged: (String) -> Unit,
    onSendResetCode: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(EobStrings.t(language, "forgotPasswordTitle"), style = MaterialTheme.typography.headlineSmall)
        Text(EobStrings.t(language, "forgotPasswordHelp"))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text(EobStrings.t(language, "email")) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        if (authMessage.isNotBlank()) {
            Text(
                authMessage,
                color = if (authMessageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        Button(
            onClick = onSendResetCode,
            enabled = email.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(EobStrings.t(language, "sendResetCode"))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "backToSignIn"))
        }
    }
}

@Composable
private fun ForgotPasswordVerifyScreen(
    language: AppLanguage,
    email: String,
    resetCode: String,
    newPassword: String,
    authMessage: String,
    authMessageIsError: Boolean,
    modifier: Modifier = Modifier,
    onResetCodeChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onUpdatePassword: () -> Unit,
    onResendResetCode: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val passwordValid = RegistrationCredentials(email = email, password = newPassword).isPasswordValid
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(EobStrings.t(language, "forgotPasswordTitle"), style = MaterialTheme.typography.headlineSmall)
        Text(EobStrings.t(language, "forgotPasswordHelp"))
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = resetCode,
            onValueChange = onResetCodeChanged,
            label = { Text(EobStrings.t(language, "passwordResetCodeLabel")) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true
        )
        PasswordField(
            language = language,
            password = newPassword,
            labelKey = "password",
            onPasswordChanged = onNewPasswordChanged
        )
        if (newPassword.isNotBlank() && !passwordValid) {
            Text(EobStrings.t(language, "passwordRule"), color = MaterialTheme.colorScheme.error)
        }
        if (authMessage.isNotBlank()) {
            Text(
                authMessage,
                color = if (authMessageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        Button(
            onClick = onUpdatePassword,
            enabled = resetCode.length == 5 && passwordValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(EobStrings.t(language, "updatePassword"))
        }
        TextButton(
            onClick = onResendResetCode,
            enabled = email.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(EobStrings.t(language, "resendResetCode"))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "backToResetEmail"))
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "backToSignIn"))
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
    OutlinedTextField(
        value = profile.hsaAllocation.takeIf { it > 0 }?.let { formatPlanAmount(it) }.orEmpty(),
        onValueChange = { value ->
            onProfileChanged(profile.copy(hsaAllocation = value.toDoubleOrNull() ?: 0.0))
        },
        label = { Text(EobStrings.t(language, "hsaAllocation")) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !fieldsEnabled,
        enabled = fieldsEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
    OutlinedTextField(
        value = profile.fsaAllocation.takeIf { it > 0 }?.let { formatPlanAmount(it) }.orEmpty(),
        onValueChange = { value ->
            onProfileChanged(profile.copy(fsaAllocation = value.toDoubleOrNull() ?: 0.0))
        },
        label = { Text(EobStrings.t(language, "fsaAllocation")) },
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
    labelKey: String = "password",
    enabled: Boolean = true,
    onPasswordChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChanged,
        label = { Text(EobStrings.t(language, labelKey)) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = !enabled,
        enabled = enabled
    )
}
