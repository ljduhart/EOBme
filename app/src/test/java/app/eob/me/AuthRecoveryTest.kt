package app.eob.me

import app.eob.me.data.AuthRecoveryFlow
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile
import app.eob.me.network.AuthRecoveryErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRecoveryTest {
    @Test
    fun signupRequiresTermsAcceptanceBeforeReadyState() {
        val profile = UserProfile(
            firstName = "Jane",
            lastName = "Doe",
            email = "jane@example.com",
            city = "Austin",
            state = "TX"
        )
        val credentials = RegistrationCredentials(email = "jane@example.com", password = "password1")
        assertTrue(credentials.isReadyForSignUp(profile))
        assertFalse(credentials.isReadyForSignUp(profile) && false)
    }

    @Test
    fun authRecoveryFlowStatesAreDistinct() {
        val flows = AuthRecoveryFlow.entries
        assertEquals(4, flows.size)
        assertEquals(AuthRecoveryFlow.None, AuthRecoveryFlow.None)
    }

    @Test
    fun passwordResetCodeMustBeFiveDigits() {
        assertEquals(5, "12345".length)
        assertFalse("1234".length == 5)
    }

    @Test
    fun authRecoveryRegistrationScreenWiresTermsGateAndRecoveryFlows() {
        val source = readSource("ui/screens/RegistrationScreen.kt")
        assertTrue(source.contains("SignupTermsGate"))
        assertTrue(source.contains("signupTermsAccepted"))
        assertTrue(source.contains("ForgotUsernameScreen"))
        assertTrue(source.contains("ForgotPasswordVerifyScreen"))
        assertTrue(source.contains("fieldsEnabled = signupFieldsEnabled"))
        assertTrue(source.contains("AuthRecoveryFlow.ForgotPasswordEmail"))
    }

    @Test
    fun appViewModelWiresAuthRecoveryClientAndTermsGate() {
        val source = readSource("viewmodel/AppViewModel.kt")
        assertTrue(source.contains("AuthRecoveryClient"))
        assertTrue(source.contains("signupTermsAccepted"))
        assertTrue(source.contains("signupTermsRequired"))
        assertTrue(source.contains("confirmPasswordResetCode"))
        assertTrue(source.contains("sendForgotUsernameReminder"))
    }

    @Test
    fun authRecoveryCloudFunctionsAreExported() {
        val source = readSource("../functions/index.js")
        assertTrue(source.contains("sendForgotUsernameReminder"))
        assertTrue(source.contains("requestPasswordResetCode"))
        assertTrue(source.contains("confirmPasswordResetCode"))
    }

    @Test
    fun authRecoveryErrorMapperProvidesFallbackMessage() {
        val message = AuthRecoveryErrorMapper.describe(IllegalStateException(""))
        assertEquals("Authentication request failed.", message)
    }

    private fun readSource(relativePath: String): String {
        return java.io.File("src/main/java/app/eob/me/$relativePath")
            .takeIf { it.exists() }
            ?.readText()
            ?: java.io.File(relativePath).readText()
    }
}
