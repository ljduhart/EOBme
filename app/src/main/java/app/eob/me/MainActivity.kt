package app.eob.me

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import app.eob.me.data.AppLanguage
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.UserProfile
import app.eob.me.navigation.EobNavHost
import app.eob.me.ui.screens.AuthScreen
import app.eob.me.ui.screens.IntroScreen
import app.eob.me.ui.screens.LanguageScreen
import app.eob.me.ui.theme.EOBmeTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EOBmeTheme {
                EobMeApp()
            }
        }
    }
}

@Composable
fun EobMeApp() {
    val appContext = LocalContext.current.applicationContext
    val firebaseRepository = remember { FirebaseEobRepository(appContext) }
    val firebaseConfigured = remember {
        runCatching {
            FirebaseApp.getApps(appContext).isNotEmpty() || FirebaseApp.initializeApp(appContext) != null
        }.getOrDefault(false)
    }
    val auth = remember(firebaseConfigured) {
        if (firebaseConfigured) runCatching { FirebaseAuth.getInstance() }.getOrNull() else null
    }
    var firebaseUser by remember { mutableStateOf<FirebaseUser?>(auth?.currentUser) }
    var language by remember { mutableStateOf<AppLanguage?>(null) }
    var introStep by remember { mutableStateOf(0) }
    var profile by remember { mutableStateOf(UserProfile(email = auth?.currentUser?.email.orEmpty())) }
    var isSignUp by remember { mutableStateOf(true) }
    var awaitingEmailVerification by remember { mutableStateOf(auth?.currentUser?.let { !it.isEmailVerified } == true) }
    var authMessage by remember {
        mutableStateOf(if (firebaseConfigured) "" else firebaseConfigMessage())
    }
    var lastActivityAt by remember { mutableStateOf(System.currentTimeMillis()) }

    DisposableEffect(auth) {
        if (auth == null) {
            onDispose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                awaitingEmailVerification = currentUser != null && !currentUser.isEmailVerified
                firebaseUser = currentUser?.takeIf { it.isEmailVerified }
                if (currentUser?.email != null && profile.email.isBlank()) {
                    profile = profile.copy(email = currentUser.email.orEmpty())
                }
            }
            auth.addAuthStateListener(listener)
            onDispose { auth.removeAuthStateListener(listener) }
        }
    }

    LaunchedEffect(firebaseUser, lastActivityAt) {
        if (firebaseUser != null) {
            delay(300_000)
            if (System.currentTimeMillis() - lastActivityAt >= 300_000) {
                firebaseRepository.signOut()
                introStep = 0
            }
        }
    }

    val selectedLanguage = language ?: AppLanguage.English
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { lastActivityAt = System.currentTimeMillis() }
            }
    ) { innerPadding ->
        when {
            language == null -> LanguageScreen(
                modifier = Modifier.padding(innerPadding),
                onSelected = {
                    language = it
                    introStep = 0
                }
            )
            firebaseUser == null && !awaitingEmailVerification && introStep < 3 -> IntroScreen(
                language = selectedLanguage,
                step = introStep,
                modifier = Modifier.padding(innerPadding),
                onNext = { introStep++ }
            )
            firebaseUser == null -> AuthScreen(
                language = selectedLanguage,
                profile = profile,
                isSignUp = isSignUp,
                awaitingEmailVerification = awaitingEmailVerification,
                authMessage = authMessage,
                modifier = Modifier.padding(innerPadding),
                onProfileChanged = {
                    profile = it
                    lastActivityAt = System.currentTimeMillis()
                },
                onToggleMode = {
                    isSignUp = !isSignUp
                    authMessage = ""
                },
                onSubmit = {
                    authMessage = ""
                    if (!firebaseConfigured) {
                        authMessage = firebaseConfigMessage()
                        return@AuthScreen
                    }
                    if (isSignUp) {
                        firebaseRepository.createAccount(profile) { status ->
                            authMessage = if (status.userId.isBlank()) status.message else "Verification email sent. Check your email before continuing."
                            if (status.userId.isNotBlank()) {
                                auth?.currentUser?.sendEmailVerification()
                                awaitingEmailVerification = true
                            }
                        }
                    } else {
                        firebaseRepository.signIn(profile.email, profile.password) { status ->
                            val currentUser = auth?.currentUser
                            if (currentUser != null && !currentUser.isEmailVerified) {
                                awaitingEmailVerification = true
                                currentUser.sendEmailVerification()
                                authMessage = "Email verification required. Check your email before continuing."
                            } else {
                                authMessage = if (status.userId.isBlank()) status.message else ""
                            }
                        }
                    }
                    lastActivityAt = System.currentTimeMillis()
                },
                onResendVerification = {
                    authMessage = "Verification email sent. Check your inbox."
                    auth?.currentUser?.sendEmailVerification()
                },
                onRefreshVerification = {
                    auth?.currentUser?.reload()?.addOnCompleteListener {
                        val currentUser = auth.currentUser
                        awaitingEmailVerification = currentUser != null && !currentUser.isEmailVerified
                        firebaseUser = currentUser?.takeIf { user -> user.isEmailVerified }
                        authMessage = if (awaitingEmailVerification) {
                            "Email is not verified yet. Please check your inbox and try again."
                        } else {
                            ""
                        }
                    }
                }
            )
            else -> EobNavHost(
                language = selectedLanguage,
                profile = profile,
                firebaseRepository = firebaseRepository,
                onProfileChanged = {
                    profile = it
                    lastActivityAt = System.currentTimeMillis()
                },
                onLanguageChanged = {
                    language = it
                    lastActivityAt = System.currentTimeMillis()
                },
                onLogout = {
                    firebaseRepository.signOut()
                    introStep = 0
                },
                onActivity = { lastActivityAt = System.currentTimeMillis() }
            )
        }
    }
}

private fun firebaseConfigMessage(): String {
    return "Firebase config was not included in this build. Confirm app/google-services.json exists, " +
        "contains package_name app.eob.me, then Sync Gradle and rebuild the signed AAB."
}
