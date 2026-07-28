package app.eob.me

import android.app.Application
import app.eob.me.data.AuthRecoveryFlow
import app.eob.me.data.FirebasePasswordResetState
import app.eob.me.data.RegistrationCredentials
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MobileUiPr194Test {
    @Before
    fun setUpFirebase() {
        val context = RuntimeEnvironment.getApplication()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    @Test
    fun firebaseAuthDependencyIsPresentInGradle() {
        val appGradle = readProjectFile("app/build.gradle.kts")
        val versions = readProjectFile("gradle/libs.versions.toml")
        assertTrue(appGradle.contains("implementation(libs.firebase.auth)"))
        assertTrue(appGradle.contains("implementation(platform(libs.firebase.bom))"))
        assertTrue(versions.contains("firebase-auth"))
    }

    @Test
    fun appViewModelUsesFirebaseSendPasswordResetEmailWithStateFlow() {
        val source = readSource("viewmodel/AppViewModel.kt")
        assertTrue(source.contains("FirebasePasswordResetState"))
        assertTrue(source.contains("firebasePasswordResetState: StateFlow<FirebasePasswordResetState>"))
        assertTrue(source.contains("fun sendPasswordResetEmail()"))
        assertTrue(source.contains("sendPasswordResetEmail(email)"))
        assertTrue(source.contains("FirebasePasswordResetState.Loading"))
        assertTrue(source.contains("FirebasePasswordResetState.Success"))
        assertTrue(source.contains("FirebasePasswordResetState.Error"))
    }

    @Test
    fun loginScreenShowsForgotPasswordTextButtonAndResetDialog() {
        val source = readSource("ui/screens/RegistrationScreen.kt")
        assertTrue(source.contains("TextButton("))
        assertTrue(source.contains("ForgotPasswordResetDialog"))
        assertTrue(source.contains("AlertDialog"))
        assertTrue(source.contains("sendResetLink"))
        assertTrue(source.contains("Toast.makeText"))
        assertTrue(source.contains("onSendPasswordResetEmail"))
        assertFalse(source.contains("OutlinedButton(onClick = onForgotPassword"))
    }

    @Test
    fun forgotPasswordOpensDialogWithoutChangingRecoveryFlow() {
        val viewModel = createViewModel()
        viewModel.onSignInSelected()
        viewModel.onCredentialsChanged(RegistrationCredentials(email = "user@example.com", password = "secret1"))
        viewModel.onForgotPassword()
        assertEquals(AuthRecoveryFlow.None, viewModel.authRecoveryFlow.value)
        assertTrue(viewModel.forgotPasswordDialogVisible.value)
        assertEquals("user@example.com", viewModel.forgotPasswordDialogEmail.value)
    }

    @Test
    fun sendPasswordResetEmailRequiresAddressBeforeCallingFirebase() {
        val viewModel = createViewModel()
        viewModel.onForgotPasswordDialogEmailChanged("")
        viewModel.sendPasswordResetEmail()
        assertTrue(viewModel.firebasePasswordResetState.value is FirebasePasswordResetState.Error)
    }

    @Test
    fun forgotUsernameOpensRecoveryFlowAndSyncsSharedEmail() {
        val viewModel = createViewModel()
        viewModel.onSignInSelected()
        viewModel.onCredentialsChanged(RegistrationCredentials(email = "user@example.com", password = "secret1"))
        viewModel.onForgotUsername()
        assertEquals(AuthRecoveryFlow.ForgotUsername, viewModel.authRecoveryFlow.value)
        assertFalse(viewModel.forgotPasswordDialogVisible.value)
        assertEquals("user@example.com", viewModel.passwordResetEmail.value)
        assertEquals("user@example.com", viewModel.forgotPasswordDialogEmail.value)
    }

    @Test
    fun forgotPasswordAndForgotUsernameShareEmailState() {
        val viewModel = createViewModel()
        viewModel.onSignInSelected()
        viewModel.onForgotPassword()
        viewModel.onForgotPasswordDialogEmailChanged("shared@example.com")
        viewModel.onDismissForgotPasswordDialog()
        viewModel.onForgotUsername()
        assertEquals("shared@example.com", viewModel.passwordResetEmail.value)
        assertEquals("shared@example.com", viewModel.registrationCredentials.value.email)
    }

    @Test
    fun cancelAuthRecoveryDismissesForgotPasswordDialog() {
        val viewModel = createViewModel()
        viewModel.onForgotPassword()
        assertTrue(viewModel.forgotPasswordDialogVisible.value)
        viewModel.onCancelAuthRecovery()
        assertFalse(viewModel.forgotPasswordDialogVisible.value)
        assertEquals(AuthRecoveryFlow.None, viewModel.authRecoveryFlow.value)
    }

    @Test
    fun forgotUsernameScreenWiringRemainsIntact() {
        val source = readSource("ui/screens/RegistrationScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(source.contains("ForgotUsernameScreen"))
        assertTrue(source.contains("onSendForgotUsername"))
        assertTrue(navSource.contains("onForgotUsername = viewModel::onForgotUsername"))
        assertTrue(navSource.contains("onSendForgotUsername = viewModel::onSendForgotUsername"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr194() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        val eobViewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertFalse(pipelineSource.contains("ForgotPasswordResetDialog"))
        assertFalse(splashSource.contains("sendPasswordResetEmail"))
        assertFalse(introSource.contains("FirebasePasswordResetState"))
        assertFalse(eobViewModelSource.contains("sendPasswordResetEmail"))
    }

    private fun createViewModel(): AppViewModel {
        val app = RuntimeEnvironment.getApplication() as Application
        return AppViewModel(app)
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readProjectFile(relativePath: String): String {
        val candidates = listOf(
            File(relativePath),
            File("../$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
