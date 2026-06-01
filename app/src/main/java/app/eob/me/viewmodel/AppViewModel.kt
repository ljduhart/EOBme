package app.eob.me.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.remote.FirebaseEobRemoteDataSource
import app.eob.me.data.repository.EobRepository
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile
import app.eob.me.navigation.Screen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseRepository = FirebaseEobRepository(application.applicationContext)
    val eobRepository: EobRepository = FirebaseEobRemoteDataSource(firebaseRepository)

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

    private val _profileEditing = MutableStateFlow(false)

    private val _registrationCredentials = MutableStateFlow(RegistrationCredentials())
    val registrationCredentials: StateFlow<RegistrationCredentials> = _registrationCredentials.asStateFlow()

    private val _isSignUp = MutableStateFlow<Boolean?>(null)
    val isSignUp: StateFlow<Boolean?> = _isSignUp.asStateFlow()

    private val _awaitingEmailVerification = MutableStateFlow(
        auth?.currentUser?.let { !it.isEmailVerified } == true
    )
    val awaitingEmailVerification: StateFlow<Boolean> = _awaitingEmailVerification.asStateFlow()

    private val _authMessage = MutableStateFlow(
        if (firebaseConfigured) "" else EobStrings.firebaseConfigMessage(AppLanguage.English)
    )
    val authMessage: StateFlow<String> = _authMessage.asStateFlow()

    private val _splashComplete = MutableStateFlow(false)
    val splashComplete: StateFlow<Boolean> = _splashComplete.asStateFlow()

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser?.takeIf { it.isEmailVerified })
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _lastActivityAt = MutableStateFlow(System.currentTimeMillis())
    val lastActivityAt: StateFlow<Long> = _lastActivityAt.asStateFlow()

    val selectedLanguage: StateFlow<AppLanguage> = _language
        .map { language -> language ?: AppLanguage.English }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.English)

    val currentScreen: StateFlow<Screen> = listOf(
        _splashComplete,
        _language,
        _introStep,
        _firebaseUser,
        _awaitingEmailVerification,
        _isSignUp
    ).combineAll { values ->
        val splashComplete = values[0] as Boolean
        val language = values[1] as AppLanguage?
        val introStep = values[2] as Int
        val firebaseUser = values[3] as FirebaseUser?
        val awaitingEmailVerification = values[4] as Boolean
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
        _registrationCredentials.value = RegistrationCredentials(email = _profile.value.email)
    }

    fun onSignInSelected() {
        _isSignUp.value = false
        _authMessage.value = ""
        _registrationCredentials.value = RegistrationCredentials(email = _profile.value.email)
    }

    fun onAuthToggleMode() {
        _isSignUp.value = null
        _authMessage.value = ""
        _registrationCredentials.value = RegistrationCredentials()
    }

    fun setProfileEditing(editing: Boolean) {
        _profileEditing.value = editing
    }

    fun onProfileChanged(updated: UserProfile) {
        _profile.value = updated
        updateActivityTime()
    }

    fun applyRemoteProfile(updated: UserProfile) {
        if (!_profileEditing.value) {
            _profile.value = updated
        }
    }

    fun onCredentialsChanged(updated: RegistrationCredentials) {
        _registrationCredentials.value = updated
        if (updated.email.isNotBlank() && updated.email != _profile.value.email) {
            _profile.update { it.copy(email = updated.email) }
        }
        updateActivityTime()
    }

    fun onLanguageChanged(updated: AppLanguage) {
        _language.value = updated
        updateActivityTime()
    }

    fun onAuthSubmit() {
        val profile = _profile.value
        val credentials = _registrationCredentials.value
        val isSignUp = _isSignUp.value == true
        val language = _language.value ?: AppLanguage.English

        viewModelScope.launch(Dispatchers.IO) {
            if (!firebaseConfigured) {
                withContext(Dispatchers.Main) {
                    _authMessage.value = EobStrings.firebaseConfigMessage(language)
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _authMessage.value = ""
            }

            try {
                val status = suspendCancellableCoroutine { continuation ->
                    if (isSignUp) {
                        val signUpCredentials = credentials.copy(email = profile.email)
                        firebaseRepository.createAccount(profile, signUpCredentials) { result ->
                            continuation.resume(result)
                        }
                    } else {
                        firebaseRepository.signIn(credentials.email, credentials.password) { result ->
                            continuation.resume(result)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (isSignUp) {
                        _authMessage.value = if (status.userId.isBlank()) {
                            status.message
                        } else {
                            EobStrings.t(language, "verificationEmailSentSignup")
                        }
                        if (status.userId.isNotBlank()) {
                            auth?.currentUser?.sendEmailVerification()
                            _awaitingEmailVerification.value = true
                            _registrationCredentials.value = RegistrationCredentials()
                        }
                    } else {
                        val currentUser = auth?.currentUser
                        if (currentUser != null && !currentUser.isEmailVerified) {
                            _awaitingEmailVerification.value = true
                            _authMessage.value = EobStrings.t(language, "verificationEmailRequired")
                        } else {
                            _authMessage.value = if (status.userId.isBlank()) {
                                status.message.ifBlank { EobStrings.t(language, "invalidCredentials") }
                            } else {
                                ""
                            }
                            if (status.userId.isNotBlank()) {
                                _registrationCredentials.value = RegistrationCredentials()
                            }
                        }
                    }
                    updateActivityTime()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authMessage.value = e.localizedMessage
                        ?: EobStrings.t(language, "authErrorGeneric")
                }
            }
        }
    }

    fun onForgotPassword() {
        val language = _language.value ?: AppLanguage.English
        val email = _registrationCredentials.value.email.ifBlank { _profile.value.email }
        firebaseRepository.sendPasswordReset(email) { message ->
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    _authMessage.value = message.ifBlank {
                        EobStrings.t(language, "passwordResetSent")
                    }
                }
            }
        }
    }

    fun onForgotUsername() {
        val language = _language.value ?: AppLanguage.English
        _authMessage.value = EobStrings.t(language, "forgotUsernameHelp")
    }

    fun onResendVerification() {
        val language = _language.value ?: AppLanguage.English
        val user = auth?.currentUser
        if (user == null) {
            _authMessage.value = EobStrings.t(language, "verifyEmailHelp")
            return
        }
        user.sendEmailVerification()
            .addOnSuccessListener {
                viewModelScope.launch {
                    withContext(Dispatchers.Main) {
                        _authMessage.value = EobStrings.t(language, "verificationEmailResent")
                    }
                }
            }
            .addOnFailureListener { error ->
                viewModelScope.launch {
                    withContext(Dispatchers.Main) {
                        _authMessage.value = error.localizedMessage
                            ?: EobStrings.t(language, "resendVerificationFailed")
                    }
                }
            }
    }

    fun onRefreshVerification() {
        auth?.currentUser?.reload()?.addOnCompleteListener {
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    val currentUser = auth.currentUser
                    _awaitingEmailVerification.value = currentUser != null && !currentUser.isEmailVerified
                    _firebaseUser.value = currentUser?.takeIf { user -> user.isEmailVerified }
                    val language = _language.value ?: AppLanguage.English
                    _authMessage.value = if (_awaitingEmailVerification.value) {
                        EobStrings.t(language, "emailNotVerifiedYet")
                    } else {
                        ""
                    }
                }
            }
        }
    }

    fun saveProfileAndCredentials(
        profile: UserProfile,
        credentials: RegistrationCredentials,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val language = _language.value ?: AppLanguage.English
            if (!firebaseConfigured) {
                withContext(Dispatchers.Main) {
                    onComplete(EobStrings.firebaseConfigMessage(language))
                }
                return@launch
            }
            val userId = auth?.currentUser?.uid.orEmpty()
            if (userId.isBlank()) {
                withContext(Dispatchers.Main) {
                    onComplete(EobStrings.t(language, "signInToSaveProfile"))
                }
                return@launch
            }

            try {
                val profileMessage = suspendCancellableCoroutine { continuation ->
                    firebaseRepository.saveProfile(userId, profile) { message ->
                        continuation.resume(EobStrings.localizeRepositoryMessage(language, message))
                    }
                }
                firebaseRepository.saveInsuranceCardMetadata(userId, profile) {}

                val passwordMessage = if (credentials.password.isNotBlank()) {
                    if (!credentials.isPasswordValid) {
                        EobStrings.t(language, "profileSavedPasswordRule")
                    } else {
                        suspendCancellableCoroutine { continuation ->
                            auth?.currentUser?.updatePassword(credentials.password)
                                ?.addOnSuccessListener {
                                    continuation.resume(EobStrings.t(language, "profileAndPasswordSaved"))
                                }
                                ?.addOnFailureListener { error ->
                                    continuation.resume(
                                        EobStrings.tf(
                                            language,
                                            "profileSavedPasswordFailed",
                                            error.localizedMessage.orEmpty()
                                        )
                                    )
                                }
                                ?: continuation.resume(profileMessage)
                        }
                    }
                } else {
                    profileMessage.ifBlank { EobStrings.t(language, "profileSaved") }
                }

                withContext(Dispatchers.Main) {
                    _profile.value = profile
                    _registrationCredentials.value = credentials.copy(
                        email = credentials.email.ifBlank { profile.email }
                    )
                    onComplete(passwordMessage)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(e.localizedMessage ?: EobStrings.t(language, "unableToSaveProfile"))
                }
            }
        }
    }

    fun onLogout() {
        firebaseRepository.signOut()
        _introStep.value = 0
        _registrationCredentials.value = RegistrationCredentials()
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
    }
}
