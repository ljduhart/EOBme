package com.eobme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.eobme.app.ui.navigation.AppNavigation
import com.eobme.app.ui.navigation.Routes
import com.eobme.app.ui.theme.EOBmeTheme
import com.eobme.app.util.InactivityTracker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var inactivityTracker: InactivityTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as EOBmeApp

        inactivityTracker = InactivityTracker(
            scope = lifecycleScope,
            timeoutMillis = 3 * 60 * 1000L,
            onTimeout = {
                lifecycleScope.launch {
                    app.userPreferences.logout()
                }
            }
        )

        setContent {
            EOBmeTheme {
                val isLoggedIn by app.userPreferences.isLoggedIn.collectAsState(initial = false)
                val onboardingComplete by app.userPreferences.onboardingComplete.collectAsState(initial = false)

                val startDestination = when {
                    isLoggedIn -> Routes.HOME
                    onboardingComplete -> Routes.LOGIN
                    else -> Routes.LANGUAGE
                }

                val navController = rememberNavController()

                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination,
                        app = app
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inactivityTracker.start()
    }

    override fun onPause() {
        super.onPause()
        inactivityTracker.stop()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        inactivityTracker.onUserActivity()
    }
}
