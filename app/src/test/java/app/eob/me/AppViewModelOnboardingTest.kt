package app.eob.me

import android.app.Application
import app.eob.me.data.AppLanguage
import app.eob.me.data.CptCategory
import app.eob.me.data.UserProfile
import app.eob.me.navigation.Screen
import app.eob.me.viewmodel.AppViewModel
import app.eob.me.viewmodel.EobViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppViewModelOnboardingTest {
    private fun createViewModel(): AppViewModel {
        val app = RuntimeEnvironment.getApplication() as Application
        return AppViewModel(app)
    }

    private fun completeOnboardingThroughIntro(viewModel: AppViewModel) {
        viewModel.onSplashComplete()
        viewModel.onLanguageSelected(AppLanguage.English)
        repeat(AppViewModel.INTRO_SLIDE_COUNT) { viewModel.onIntroNext() }
    }

    private fun currentScreen(viewModel: AppViewModel): Screen = runBlocking {
        viewModel.currentScreen.first()
    }

    @Test
    fun createAccountSelectedOpensAuthScreen() {
        val viewModel = createViewModel()
        completeOnboardingThroughIntro(viewModel)
        viewModel.onCreateAccountSelected()
        assertEquals(Screen.Auth, currentScreen(viewModel))
        assertEquals(true, viewModel.isSignUp.value)
    }

    @Test
    fun signInSelectedOpensAuthScreenInLoginMode() {
        val viewModel = createViewModel()
        completeOnboardingThroughIntro(viewModel)
        viewModel.onSignInSelected()
        assertEquals(Screen.Auth, currentScreen(viewModel))
        assertEquals(false, viewModel.isSignUp.value)
    }

    @Test
    fun authToggleReturnsToAuthChoice() {
        val viewModel = createViewModel()
        completeOnboardingThroughIntro(viewModel)
        viewModel.onCreateAccountSelected()
        viewModel.onAuthToggleMode()
        assertEquals(Screen.AuthChoice, currentScreen(viewModel))
        assertNull(viewModel.isSignUp.value)
    }

    @Test
    fun logoutResetsSignUpMode() {
        val viewModel = createViewModel()
        completeOnboardingThroughIntro(viewModel)
        viewModel.onCreateAccountSelected()
        viewModel.onLogout()
        assertNull(viewModel.isSignUp.value)
    }

    @Test
    fun authSubmitSanitizesPlanLimitsBeforePersistence() {
        val viewModel = createViewModel()
        viewModel.onProfileChanged(
            UserProfile(
                firstName = "Jane",
                lastName = "Doe",
                email = "jane@example.com",
                city = "Austin",
                state = "TX",
                annualDeductibleLimit = 999_999.0,
                annualOutOfPocketMax = -50.0
            )
        )
        viewModel.onAuthSubmit()
        val safe = viewModel.profile.value
        assertEquals(100_000.0, safe.annualDeductibleLimit, 0.01)
        assertEquals(0.0, safe.annualOutOfPocketMax, 0.01)
    }

    @Test
    fun eobViewModelRemainsHubSourceOfTruthForCptCategory() {
        val viewModel = EobViewModel()
        viewModel.setSelectedCptCategory(CptCategory.Hospital)
        assertEquals(CptCategory.Hospital, viewModel.uiState.value.selectedCptCategory)
        assertTrue(viewModel.cptBentoSnapshot(AppLanguage.English).translatorLine.isNotBlank())
    }
}
