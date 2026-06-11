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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import app.eob.me.navigation.EobNavHost
import app.eob.me.ui.components.eobAppBackgroundGradient
import app.eob.me.ui.theme.EOBmeTheme
import app.eob.me.viewmodel.AppViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EOBmeTheme(darkTheme = false) {
                val viewModel: AppViewModel = viewModel()

                Scaffold(
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(eobAppBackgroundGradient())
                        .pointerInput(Unit) {
                            detectTapGestures { viewModel.updateActivityTime() }
                        }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        EobNavHost(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
