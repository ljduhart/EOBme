package app.eob.me

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.eob.me.navigation.EobNavHost
import app.eob.me.navigation.Screen
import app.eob.me.ui.theme.EOBmeTheme
import app.eob.me.ui.theme.eobCyberAppBackgroundGradient
import app.eob.me.ui.theme.eobLightAppBackgroundGradient
import app.eob.me.viewmodel.AppViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            var hubDarkModeEnabled by remember { mutableStateOf(false) }
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val useDarkTheme = currentScreen == Screen.MainHub && hubDarkModeEnabled
            val appBackground: Brush = if (useDarkTheme) {
                eobCyberAppBackgroundGradient()
            } else {
                eobLightAppBackgroundGradient()
            }

            EOBmeTheme(darkTheme = useDarkTheme) {
                Scaffold(
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appBackground)
                        .pointerInput(Unit) {
                            detectTapGestures { viewModel.updateActivityTime() }
                        }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        EobNavHost(
                            viewModel = viewModel,
                            onHubDarkModeChanged = { enabled ->
                                hubDarkModeEnabled = enabled
                            }
                        )
                    }
                }
            }
        }
    }
}
