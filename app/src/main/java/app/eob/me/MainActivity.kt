package app.eob.me

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.eob.me.app.EobMeAppRoot
import app.eob.me.ui.theme.EOBmeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EOBmeTheme {
                EobMeAppRoot()
            }
        }
    }
}
