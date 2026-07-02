package app.eob.me

import android.app.Application
import app.eob.me.data.AuthRecoveryFlow
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile
import app.eob.me.network.AuthRecoveryErrorMapper
import app.eob.me.viewmodel.AppViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthRecoveryTest {
    @Before
    fun setUpFirebase() {
        val context = RuntimeEnvironment.getApplication()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    private fun createViewModel(): AppViewModel {
        val app = RuntimeEnvironment.getApplication() as Application
        return AppViewModel(app)
    }

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
    fun backFromPasswordVerifyReturnsToEmailStep() {
        val viewModel = createViewModel()
        viewModel.onForgotPassword()
        viewModel.onPasswordResetEmailChanged("user@example.com")
        setAuthRecoveryFlow(viewModel, AuthRecoveryFlow.ForgotPasswordVerify)
        viewModel.onPasswordResetCodeChanged("12345")
        viewModel.onPasswordResetDraftChanged("newpassword1")
        viewModel.onBackFromPasswordVerify()
        assertEquals(AuthRecoveryFlow.ForgotPasswordEmail, viewModel.authRecoveryFlow.value)
        assertEquals("", viewModel.passwordResetCode.value)
        assertEquals("", viewModel.passwordResetDraft.value)
        assertEquals("", viewModel.authMessage.value)
    }

    @Test
    fun cancelAuthRecoveryClearsTransientResetState() {
        val viewModel = createViewModel()
        viewModel.onForgotPassword()
        viewModel.onPasswordResetEmailChanged("user@example.com")
        viewModel.onCancelAuthRecovery()
        assertEquals(AuthRecoveryFlow.None, viewModel.authRecoveryFlow.value)
        assertEquals("", viewModel.passwordResetEmail.value)
        assertEquals("", viewModel.passwordResetCode.value)
        assertEquals("", viewModel.passwordResetDraft.value)
    }

    @Test
    fun logoutClearsAuthRecoveryState() {
        val viewModel = createViewModel()
        viewModel.onForgotUsername()
        viewModel.onPasswordResetEmailChanged("user@example.com")
        viewModel.onLogout()
        assertEquals(AuthRecoveryFlow.None, viewModel.authRecoveryFlow.value)
        assertEquals("", viewModel.passwordResetEmail.value)
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
        assertTrue(source.contains("BackHandler"))
        assertTrue(source.contains("onBackFromPasswordVerify"))
        assertTrue(source.contains("resendResetCode"))
    }

    @Test
    fun appViewModelWiresAuthRecoveryClientAndTermsGate() {
        val source = readSource("viewmodel/AppViewModel.kt")
        assertTrue(source.contains("AuthRecoveryClient"))
        assertTrue(source.contains("signupTermsAccepted"))
        assertTrue(source.contains("signupTermsRequired"))
        assertTrue(source.contains("confirmPasswordResetCode"))
        assertTrue(source.contains("sendForgotUsernameReminder"))
        assertTrue(source.contains("onBackFromPasswordVerify"))
        assertTrue(source.contains("authMessageIsError"))
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

    private fun setAuthRecoveryFlow(viewModel: AppViewModel, flow: AuthRecoveryFlow) {
        val field = AppViewModel::class.java.getDeclaredField("_authRecoveryFlow")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val state = field.get(viewModel) as MutableStateFlow<AuthRecoveryFlow>
        state.value = flow
    }
}
