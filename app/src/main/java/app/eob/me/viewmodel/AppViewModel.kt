package app.eob.me.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.UserProfile
import app.eob.me.navigation.Screen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val firebaseRepository = FirebaseEobRepository(application.applicationContext)

    private val firebaseConfigured: Boolean = runCatching {
        val context = application.applicationContext
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    }.getOrDefault(false)

    private val auth: FirebaseAuth? = if (firebaseConfigured) {
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
    } else {
        null
    }

    private val _language = MutableStateFlow<AppLanguage?>(null)
    val language: StateFlow<AppLanguage?> = _language.asStateFlow()

    private val _introStep = MutableStateFlow(0)
    val introStep: StateFlow<Int> = _introStep.asStateFlow()

    private val _profile = MutableStateFlow(UserProfile(email = auth?.currentUser?.email.orEmpty()))
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _isSignUp = MutableStateFlow<Boolean?>(null)
    val isSignUp: StateFlow<Boolean?> = _isSignUp.asStateFlow()

    private val _awaitingEmailVerification = MutableStateFlow(
        auth?.currentUser?.let { !it.isEmailVerified } == true
    )
    val awaitingEmailVerification: StateFlow<Boolean> = _awaitingEmailVerification.asStateFlow()

    private val _authMessage = MutableStateFlow(
        if (firebaseConfigured) "" else firebaseConfigMessage()
    )
    val authMessage: StateFlow<String> = _authMessage.asStateFlow()

    private val _splashComplete = MutableStateFlow(false)
    val splashComplete: StateFlow<Boolean> = _splashComplete.asStateFlow()

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser?.takeIf { it.isEmailVerified })
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _lastActivityAt = MutableStateFlow(System.currentTimeMillis())
    val lastActivityAt: StateFlow<Long> = _lastActivityAt.asStateFlow()

    val selectedLanguage: StateFlow<AppLanguage> = combine(_language) { languages ->
        languages[0] ?: AppLanguage.English
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.English)

    val currentScreen: StateFlow<Screen> = combine(
        _splashComplete,
        _language,
        _introStep,
        _firebaseUser,
        _awaitingEmailVerification,
        _isSignUp
    ) { values ->
        val splashComplete = values[0] as Boolean
        val language = values[1] as AppLanguage?
        val introStep = values[2] as Int
        val firebaseUser = values[3] as FirebaseUser?
        val awaitingEmailVerification = values[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val isSignUp = values[5] as Boolean?

        when {
            !splashComplete -> Screen.Splash
            language == null -> Screen.Language
            firebaseUser == null && !awaitingEmailVerification && introStep < 3 -> Screen.Intro
            firebaseUser == null && isSignUp == null -> Screen.AuthChoice
            firebaseUser == null -> Screen.Auth
            else -> Screen.MainHub
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Screen.Splash)

    private var inactivityJob: Job? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            _awaitingEmailVerification.value = currentUser != null && !currentUser.isEmailVerified
            _firebaseUser.value = currentUser?.takeIf { it.isEmailVerified }
            if (currentUser?.email != null && _profile.value.email.isBlank()) {
                _profile.update { it.copy(email = currentUser.email.orEmpty()) }
            }
            resetInactivityTimer()
        }
        auth?.addAuthStateListener(authStateListener!!)

        viewModelScope.launch {
            combine(_firebaseUser, _lastActivityAt) { user, _ -> user }
                .collect { user ->
                    if (user != null) resetInactivityTimer() else inactivityJob?.cancel()
                }
        }
    }

    fun updateActivityTime() {
        _lastActivityAt.value = System.currentTimeMillis()
        resetInactivityTimer()
    }

    fun onSplashComplete() {
        _splashComplete.value = true
    }

    fun onLanguageSelected(selected: AppLanguage) {
        _language.value = selected
        _introStep.value = 0
        updateActivityTime()
    }

    fun onIntroNext() {
        _introStep.update { it + 1 }
        updateActivityTime()
    }

    fun onCreateAccountSelected() {
        _isSignUp.value = true
        _authMessage.value = ""
    }

    fun onSignInSelected() {
        _isSignUp.value = false
        _authMessage.value = ""
    }

    fun onAuthToggleMode() {
        _isSignUp.value = null
        _authMessage.value = ""
    }

    fun onProfileChanged(updated: UserProfile) {
        _profile.value = updated
        updateActivityTime()
    }

    fun onLanguageChanged(updated: AppLanguage) {
        _language.value = updated
        updateActivityTime()
    }

    fun onAuthSubmit() {
        _authMessage.value = ""
        if (!firebaseConfigured) {
            _authMessage.value = firebaseConfigMessage()
            return
        }
        val profile = _profile.value
        if (_isSignUp.value == true) {
            firebaseRepository.createAccount(profile) { status ->
                _authMessage.value = if (status.userId.isBlank()) {
                    status.message
                } else {
                    "Verification email sent. Check your email before continuing."
                }
                if (status.userId.isNotBlank()) {
                    auth?.currentUser?.sendEmailVerification()
                    _awaitingEmailVerification.value = true
                }
            }
        } else {
            firebaseRepository.signIn(profile.email, profile.password) { status ->
                val currentUser = auth?.currentUser
                if (currentUser != null && !currentUser.isEmailVerified) {
                    _awaitingEmailVerification.value = true
                    _authMessage.value =
                        "Email verification required. Check your original verification email before continuing."
                } else {
                    _authMessage.value = if (status.userId.isBlank()) status.message else ""
                }
            }
        }
        updateActivityTime()
    }

    fun onForgotPassword() {
        firebaseRepository.sendPasswordReset(_profile.value.email) { message ->
            _authMessage.value = message.ifBlank {
                EobStrings.t(selectedLanguage.value, "passwordResetSent")
            }
        }
    }

    fun onForgotUsername() {
        _authMessage.value = EobStrings.t(selectedLanguage.value, "forgotUsernameHelp")
    }

    fun onRefreshVerification() {
        auth?.currentUser?.reload()?.addOnCompleteListener {
            val currentUser = auth.currentUser
            _awaitingEmailVerification.value = currentUser != null && !currentUser.isEmailVerified
            _firebaseUser.value = currentUser?.takeIf { user -> user.isEmailVerified }
            _authMessage.value = if (_awaitingEmailVerification.value) {
                "Email is not verified yet. Please check your inbox and try again."
            } else {
                ""
            }
        }
    }

    fun onLogout() {
        firebaseRepository.signOut()
        _introStep.value = 0
        updateActivityTime()
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        if (_firebaseUser.value == null) return
        val scheduledAt = _lastActivityAt.value
        inactivityJob = viewModelScope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            if (_firebaseUser.value != null && System.currentTimeMillis() - scheduledAt >= INACTIVITY_TIMEOUT_MS) {
                firebaseRepository.signOut()
                _introStep.value = 0
            }
        }
    }

    override fun onCleared() {
        authStateListener?.let { listener -> auth?.removeAuthStateListener(listener) }
        inactivityJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val INACTIVITY_TIMEOUT_MS = 300_000L

        fun firebaseConfigMessage(): String {
            return "Firebase config was not included in this build. Confirm app/google-services.json exists, " +
                "contains package_name app.eob.me, then Sync Gradle and rebuild the signed AAB."
        }
    }
}
