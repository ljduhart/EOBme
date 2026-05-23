package app.eob.me

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.UserProfile
import app.eob.me.navigation.EobNavHost
import app.eob.me.ui.screens.AuthChoiceScreen
import app.eob.me.ui.screens.AuthScreen
import app.eob.me.ui.screens.IntroScreen
import app.eob.me.ui.screens.LanguageScreen
import app.eob.me.ui.theme.EOBmeTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
    var isSignUp by remember { mutableStateOf<Boolean?>(null) }
    var awaitingEmailVerification by remember { mutableStateOf(auth?.currentUser?.let { !it.isEmailVerified } == true) }
    var authMessage by remember {
        mutableStateOf(if (firebaseConfigured) "" else firebaseConfigMessage())
    }
    var lastActivityAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var splashComplete by remember { mutableStateOf(false) }

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
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(eobAppBackgroundGradient())
            .pointerInput(Unit) {
                detectTapGestures { lastActivityAt = System.currentTimeMillis() }
            }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!splashComplete) {
                EobSplashScreen(
                    modifier = Modifier.fillMaxSize(),
                    onSplashComplete = { splashComplete = true }
                )
            } else {
                when {
                    language == null -> LanguageScreen(
                        modifier = Modifier.fillMaxSize(),
                        onSelected = {
                            language = it
                            introStep = 0
                        }
                    )
                    firebaseUser == null && !awaitingEmailVerification && introStep < 3 -> IntroScreen(
                        language = selectedLanguage,
                        step = introStep,
                        modifier = Modifier.fillMaxSize(),
                        onNext = { introStep++ }
                    )
                    firebaseUser == null && isSignUp == null -> AuthChoiceScreen(
                        language = selectedLanguage,
                        modifier = Modifier.fillMaxSize(),
                        onCreateAccount = {
                            isSignUp = true
                            authMessage = ""
                        },
                        onSignIn = {
                            isSignUp = false
                            authMessage = ""
                        }
                    )
                    firebaseUser == null -> AuthScreen(
                        language = selectedLanguage,
                        profile = profile,
                        isSignUp = isSignUp == true,
                        awaitingEmailVerification = awaitingEmailVerification,
                        authMessage = authMessage,
                        modifier = Modifier.fillMaxSize(),
                        onProfileChanged = {
                            profile = it
                            lastActivityAt = System.currentTimeMillis()
                        },
                        onToggleMode = {
                            isSignUp = null
                            authMessage = ""
                        },
                        onSubmit = {
                            authMessage = ""
                            if (!firebaseConfigured) {
                                authMessage = firebaseConfigMessage()
                                return@AuthScreen
                            }
                            if (isSignUp == true) {
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
                                        authMessage = "Email verification required. Check your original verification email before continuing."
                                    } else {
                                        authMessage = if (status.userId.isBlank()) status.message else ""
                                    }
                                }
                            }
                            lastActivityAt = System.currentTimeMillis()
                        },
                        onForgotPassword = {
                            firebaseRepository.sendPasswordReset(profile.email) { authMessage = it.ifBlank { EobStrings.t(selectedLanguage, "passwordResetSent") } }
                        },
                        onForgotUsername = {
                            authMessage = EobStrings.t(selectedLanguage, "forgotUsernameHelp")
                        },
                        onResendVerification = {},
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
    }
}

@Composable
private fun EobSplashScreen(modifier: Modifier = Modifier, onSplashComplete: () -> Unit) {
    val splashAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Main) {
            delay(4_000)
            splashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
            onSplashComplete()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        EobSplashLogo(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .aspectRatio(1f)
                .graphicsLayer { alpha = splashAlpha.value }
        )
    }
}

@Composable
private fun EobSplashLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerRadius = size.width * 0.21f
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2498EA), Color(0xFF0E45BE)),
                    start = Offset(size.width * 0.2f, 0f),
                    end = Offset(size.width * 0.75f, size.height)
                ),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            val shadowWave = Path().apply {
                moveTo(0f, size.height * 0.70f)
                cubicTo(size.width * 0.20f, size.height * 0.58f, size.width * 0.38f, size.height * 0.62f, size.width * 0.55f, size.height * 0.66f)
                cubicTo(size.width * 0.72f, size.height * 0.70f, size.width * 0.89f, size.height * 0.66f, size.width, size.height * 0.45f)
                lineTo(size.width, size.height * 0.58f)
                cubicTo(size.width * 0.86f, size.height * 0.82f, size.width * 0.68f, size.height * 0.80f, size.width * 0.50f, size.height * 0.75f)
                cubicTo(size.width * 0.31f, size.height * 0.70f, size.width * 0.17f, size.height * 0.70f, 0f, size.height * 0.82f)
                close()
            }
            drawPath(
                path = shadowWave,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2A91F1), Color(0xFF58BDF6)),
                    start = Offset(0f, size.height * 0.82f),
                    end = Offset(size.width, size.height * 0.54f)
                )
            )

            val lightWave = Path().apply {
                moveTo(0f, size.height * 0.60f)
                cubicTo(size.width * 0.20f, size.height * 0.49f, size.width * 0.35f, size.height * 0.55f, size.width * 0.53f, size.height * 0.57f)
                cubicTo(size.width * 0.73f, size.height * 0.60f, size.width * 0.88f, size.height * 0.56f, size.width, size.height * 0.40f)
                lineTo(size.width, size.height * 0.47f)
                cubicTo(size.width * 0.88f, size.height * 0.70f, size.width * 0.68f, size.height * 0.71f, size.width * 0.50f, size.height * 0.67f)
                cubicTo(size.width * 0.30f, size.height * 0.62f, size.width * 0.16f, size.height * 0.60f, 0f, size.height * 0.70f)
                close()
            }
            drawPath(
                path = lightWave,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF69B8F7), Color(0xFFD7F2FF)),
                    start = Offset(0f, size.height * 0.63f),
                    end = Offset(size.width, size.height * 0.52f)
                )
            )
        }

        Row(
            modifier = Modifier.offset(y = (-14).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EOB",
                color = Color.White,
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )
            Text(
                modifier = Modifier.offset(x = (-2).dp, y = 4.dp),
                text = "me",
                color = Color(0xFF7DD4FF),
                fontSize = 52.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-3).sp
            )
        }
    }
}

private fun eobAppBackgroundGradient(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFEAF6FF),
            Color(0xFFD6ECFF)
        )
    )
}

private fun firebaseConfigMessage(): String {
    return "Firebase config was not included in this build. Confirm app/google-services.json exists, " +
        "contains package_name app.eob.me, then Sync Gradle and rebuild the signed AAB."
}
