package app.eob.me.app

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
import app.eob.me.screens.AuthScreen
import app.eob.me.screens.EmailVerificationScreen
import app.eob.me.screens.IntroScreen
import app.eob.me.screens.LanguageScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay

@Composable
fun EobMeAppRoot() {
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
    var isAwaitingAccountVerification by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var verificationMessage by remember { mutableStateOf("") }
    var authMessage by remember {
        mutableStateOf(if (firebaseConfigured) "" else firebaseConfigMessage())
    }
    var lastActivityAt by remember { mutableStateOf(System.currentTimeMillis()) }

    DisposableEffect(auth) {
        if (auth == null) {
            onDispose { }
        } else {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            firebaseUser = firebaseAuth.currentUser
            if (firebaseAuth.currentUser?.email != null && profile.email.isBlank()) {
                profile = profile.copy(email = firebaseAuth.currentUser?.email.orEmpty())
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

    LaunchedEffect(firebaseUser?.uid) {
        val user = firebaseUser ?: return@LaunchedEffect
        firebaseRepository.loadProfile(user.uid, profile.password) { loadedProfile ->
            profile = loadedProfile
            if (!loadedProfile.accountSetupVerified) {
                isAwaitingAccountVerification = true
                verificationMessage = "Enter the latest code sent to ${loadedProfile.email}."
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
            firebaseUser == null && introStep < 3 -> IntroScreen(
                language = selectedLanguage,
                step = introStep,
                modifier = Modifier.padding(innerPadding),
                onNext = { introStep++ }
            )
            firebaseUser != null && isAwaitingAccountVerification -> EmailVerificationScreen(
                language = selectedLanguage,
                email = profile.email.ifBlank { firebaseUser?.email.orEmpty() },
                verificationCode = verificationCode,
                verificationMessage = verificationMessage,
                modifier = Modifier.padding(innerPadding),
                onVerificationCodeChanged = {
                    verificationCode = it
                    lastActivityAt = System.currentTimeMillis()
                },
                onVerify = {
                    verificationMessage = ""
                    firebaseRepository.verifyAccountCreationCode(verificationCode) { status ->
                        if (status.userId.isNotBlank()) {
                            val verifiedProfile = profile.copy(accountSetupVerified = true)
                            profile = verifiedProfile
                            firebaseRepository.saveProfile(status.userId, verifiedProfile) {}
                            isAwaitingAccountVerification = false
                            verificationCode = ""
                        } else {
                            verificationMessage = status.message
                        }
                    }
                    lastActivityAt = System.currentTimeMillis()
                },
                onResend = {
                    verificationMessage = ""
                    firebaseRepository.requestAccountVerificationCode { status ->
                        verificationMessage = status.message
                    }
                    lastActivityAt = System.currentTimeMillis()
                },
                onCancel = {
                    firebaseRepository.signOut()
                    isAwaitingAccountVerification = false
                    verificationCode = ""
                    verificationMessage = ""
                    introStep = 3
                    lastActivityAt = System.currentTimeMillis()
                }
            )
            firebaseUser == null -> AuthScreen(
                language = selectedLanguage,
                profile = profile,
                isSignUp = isSignUp,
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
                            if (status.userId.isBlank()) {
                                isAwaitingAccountVerification = false
                                authMessage = status.message
                            } else {
                                profile = profile.copy(accountSetupVerified = false)
                                isAwaitingAccountVerification = status.requiresEmailCodeVerification
                                verificationMessage = status.message
                                authMessage = ""
                            }
                        }
                    } else {
                        firebaseRepository.signIn(profile.email, profile.password) { status ->
                            authMessage = if (status.userId.isBlank()) status.message else ""
                            if (status.userId.isNotBlank()) {
                                firebaseRepository.loadProfile(status.userId, profile.password) { loadedProfile ->
                                    profile = loadedProfile
                                    isAwaitingAccountVerification = !loadedProfile.accountSetupVerified
                                    if (!loadedProfile.accountSetupVerified) {
                                        verificationMessage = "Enter the latest code sent to ${loadedProfile.email}."
                                    }
                                }
                            }
                        }
                    }
                    lastActivityAt = System.currentTimeMillis()
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
