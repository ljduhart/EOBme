package com.eobme.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eobme.app.EOBmeApp
import com.eobme.app.ui.screens.analysis.AnalysisResultsScreen
import com.eobme.app.ui.screens.appeal.AppealLetterScreen
import com.eobme.app.ui.screens.auth.LoginScreen
import com.eobme.app.ui.screens.auth.RegisterScreen
import com.eobme.app.ui.screens.cpt.CptTrackingScreen
import com.eobme.app.ui.screens.eob.EobUploadScreen
import com.eobme.app.ui.screens.home.HomeScreen
import com.eobme.app.ui.screens.language.LanguageSelectionScreen
import com.eobme.app.ui.screens.onboarding.OnboardingScreen
import com.eobme.app.ui.screens.profile.ProfileScreen
import com.eobme.app.ui.screens.support.SupportScreen

object Routes {
    const val LANGUAGE = "language"
    const val ONBOARDING = "onboarding"
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val HOME = "home"
    const val EOB_UPLOAD = "eob_upload"
    const val ANALYSIS = "analysis/{eobId}"
    const val APPEAL = "appeal/{eobId}"
    const val PROFILE = "profile"
    const val CPT_TRACKING = "cpt_tracking"
    const val SUPPORT = "support"

    fun analysis(eobId: Long) = "analysis/$eobId"
    fun appeal(eobId: Long) = "appeal/$eobId"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    app: EOBmeApp
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LANGUAGE) {
            LanguageSelectionScreen(
                preferences = app.userPreferences,
                onLanguageSelected = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.LANGUAGE) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                preferences = app.userPreferences,
                onComplete = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                userRepository = app.userRepository,
                preferences = app.userPreferences,
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN)
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                userRepository = app.userRepository,
                preferences = app.userPreferences,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                app = app,
                onUploadEob = { navController.navigate(Routes.EOB_UPLOAD) },
                onEobClick = { eobId -> navController.navigate(Routes.analysis(eobId)) },
                onCptTracking = { navController.navigate(Routes.CPT_TRACKING) },
                onProfile = { navController.navigate(Routes.PROFILE) },
                onSupport = { navController.navigate(Routes.SUPPORT) },
                onLogout = {
                    navController.navigate(Routes.LANGUAGE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.EOB_UPLOAD) {
            EobUploadScreen(
                app = app,
                onEobSaved = { eobId ->
                    navController.navigate(Routes.analysis(eobId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ANALYSIS,
            arguments = listOf(navArgument("eobId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eobId = backStackEntry.arguments?.getLong("eobId") ?: return@composable
            AnalysisResultsScreen(
                eobId = eobId,
                app = app,
                onAppeal = { navController.navigate(Routes.appeal(eobId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.APPEAL,
            arguments = listOf(navArgument("eobId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eobId = backStackEntry.arguments?.getLong("eobId") ?: return@composable
            AppealLetterScreen(
                eobId = eobId,
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CPT_TRACKING) {
            CptTrackingScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SUPPORT) {
            SupportScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
