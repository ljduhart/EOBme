package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr193Test {
    @Test
    fun authRecoveryCallableFunctionsAreDeployed() {
        val functionsSource = readFunctionsSource("index.js")
        val authRecoverySource = readFunctionsSource("lib/authRecovery.js")
        val authRecoveryCoreSource = readFunctionsSource("lib/authRecoveryCore.js")
        assertTrue(functionsSource.contains("exports.sendForgotUsernameReminder"))
        assertTrue(functionsSource.contains("exports.requestPasswordResetCode"))
        assertTrue(functionsSource.contains("exports.confirmPasswordResetCode"))
        assertTrue(authRecoverySource.contains("sendForgotUsernameReminder"))
        assertTrue(authRecoverySource.contains("requestPasswordResetCode"))
        assertTrue(authRecoverySource.contains("confirmPasswordResetCode"))
        assertTrue(authRecoveryCoreSource.contains("auth_password_reset_codes"))
    }

    @Test
    fun signupAndRecoverySendEmailToAccountAddress() {
        val appViewModelSource = readSource("viewmodel/AppViewModel.kt")
        val authRecoverySource = readSource("network/AuthRecoveryClient.kt")
        assertTrue(appViewModelSource.contains("sendEmailVerification()"))
        assertTrue(appViewModelSource.contains("onSendForgotUsername"))
        assertTrue(appViewModelSource.contains("authRecoveryClient.sendForgotUsernameReminder"))
        assertTrue(appViewModelSource.contains("authRecoveryClient.requestPasswordResetCode"))
        assertTrue(authRecoverySource.contains("sendForgotUsernameReminder"))
        assertTrue(authRecoverySource.contains("requestPasswordResetCode"))
    }

    @Test
    fun billingAndRevenueCatStaySyncedWithForcedPlayBillingRuntime() {
        val versionsSource = readProjectFile("gradle/libs.versions.toml")
        val appGradleSource = readProjectFile("app/build.gradle.kts")
        val applicationSource = readSource("EobApplication.kt")
        assertTrue(versionsSource.contains("revenuecatPurchases = \"10.15.1\""))
        assertTrue(versionsSource.contains("billing = \"9.1.0\""))
        assertTrue(appGradleSource.contains("resolutionStrategy.force(\"com.android.billingclient:billing:"))
        assertTrue(applicationSource.contains("Purchases.configure"))
        assertTrue(applicationSource.contains("RevenueCatConfig.PUBLIC_API_KEY"))
    }

    @Test
    fun duplicateDiscardCleansPendingHybridScanBeforeHistorySync() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val firebaseSource = readSource("data/FirebaseEobRepository.kt")
        val repositorySource = readSource("data/repository/EobRepository.kt")
        assertTrue(viewModelSource.contains("repo.discardPendingHybridScan"))
        assertTrue(viewModelSource.contains("HybridDocumentRef.stableDocumentId"))
        assertFalse(pipelineSource.contains("discardPendingHybridDocument"))
        assertTrue(firebaseSource.contains("discardPendingHybridScan"))
        assertTrue(firebaseSource.contains("discardedPendingDuplicateScan"))
        assertTrue(repositorySource.contains("suspend fun discardPendingHybridScan"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr193() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("discardedPendingDuplicateScan"))
        assertFalse(veryfiSource.contains("auth_password_reset_codes"))
        assertFalse(splashSource.contains("discardPendingHybridScan"))
        assertFalse(introSource.contains("requestPasswordResetCode"))
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

    private fun readFunctionsSource(relativePath: String): String {
        val candidates = listOf(
            File("functions/$relativePath"),
            File("../functions/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
