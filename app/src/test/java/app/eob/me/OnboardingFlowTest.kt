package app.eob.me

import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile
import app.eob.me.data.AppLanguage
import app.eob.me.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Onboarding gate mirrors [app.eob.me.viewmodel.AppViewModel] `currentScreen` combine logic.
 * If onboarding routing changes in AppViewModel, update this resolver and tests together.
 */
class OnboardingFlowTest {
    @Test
    fun splashIsFirstWhenSplashIncomplete() {
        assertEquals(
            Screen.Splash,
            resolveOnboardingScreen(
                splashComplete = false,
                language = AppLanguage.English,
                introStep = 3,
                hasVerifiedUser = true,
                awaitingEmailVerification = false,
                isSignUp = false
            )
        )
    }

    @Test
    fun languageFollowsSplash() {
        assertEquals(
            Screen.Language,
            resolveOnboardingScreen(
                splashComplete = true,
                language = null,
                introStep = 0,
                hasVerifiedUser = false,
                awaitingEmailVerification = false,
                isSignUp = null
            )
        )
    }

    @Test
    fun introShowsForThreeStepsBeforeAuthChoice() {
        listOf(0, 1, 2).forEach { step ->
            assertEquals(
                Screen.Intro,
                resolveOnboardingScreen(
                    splashComplete = true,
                    language = AppLanguage.English,
                    introStep = step,
                    hasVerifiedUser = false,
                    awaitingEmailVerification = false,
                    isSignUp = null
                )
            )
        }
    }

    @Test
    fun authChoiceAfterIntroComplete() {
        assertEquals(
            Screen.AuthChoice,
            resolveOnboardingScreen(
                splashComplete = true,
                language = AppLanguage.English,
                introStep = 3,
                hasVerifiedUser = false,
                awaitingEmailVerification = false,
                isSignUp = null
            )
        )
    }

    @Test
    fun authScreenWhenModeSelected() {
        assertEquals(
            Screen.Auth,
            resolveOnboardingScreen(
                splashComplete = true,
                language = AppLanguage.English,
                introStep = 3,
                hasVerifiedUser = false,
                awaitingEmailVerification = false,
                isSignUp = true
            )
        )
    }

    @Test
    fun mainHubWhenUserVerified() {
        assertEquals(
            Screen.MainHub,
            resolveOnboardingScreen(
                splashComplete = true,
                language = AppLanguage.English,
                introStep = 0,
                hasVerifiedUser = true,
                awaitingEmailVerification = false,
                isSignUp = null
            )
        )
    }

    @Test
    fun registrationCredentialsValidateSignUpAndSignIn() {
        val profile = UserProfile(
            firstName = "Jane",
            lastName = "Doe",
            email = "jane@example.com",
            city = "Austin",
            state = "TX"
        )
        val creds = RegistrationCredentials(email = "jane@example.com", password = "password1")
        assertTrue(creds.isReadyForSignIn())
        assertTrue(creds.isReadyForSignUp(profile))
        assertFalse(RegistrationCredentials(email = "", password = "password1").isReadyForSignIn())
        assertFalse(RegistrationCredentials(email = "a@b.com", password = "short").isPasswordValid)
    }

    @Test
    fun outerScreenRoutesAreUnique() {
        val routes = listOf(
            Screen.Splash.route,
            Screen.Language.route,
            Screen.Intro.route,
            Screen.AuthChoice.route,
            Screen.Auth.route,
            Screen.MainHub.route
        )
        assertEquals(routes.size, routes.toSet().size)
    }

    private fun resolveOnboardingScreen(
        splashComplete: Boolean,
        language: AppLanguage?,
        introStep: Int,
        hasVerifiedUser: Boolean,
        awaitingEmailVerification: Boolean,
        isSignUp: Boolean?
    ): Screen = when {
        !splashComplete -> Screen.Splash
        language == null -> Screen.Language
        !hasVerifiedUser && !awaitingEmailVerification && introStep < 3 -> Screen.Intro
        !hasVerifiedUser && isSignUp == null -> Screen.AuthChoice
        !hasVerifiedUser -> Screen.Auth
        else -> Screen.MainHub
    }
}
