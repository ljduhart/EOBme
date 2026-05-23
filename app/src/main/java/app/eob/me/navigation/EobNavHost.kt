package app.eob.me.navigation

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.eob.me.app.EobViewModel
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.EobStrings
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.UserProfile
import app.eob.me.ui.screens.AnalysisScreen
import app.eob.me.ui.screens.AppealScreen
import app.eob.me.ui.screens.AuthChoiceScreen
import app.eob.me.ui.screens.AuthScreen
import app.eob.me.ui.screens.CameraCaptureScreen
import app.eob.me.ui.screens.CptCountScreen
import app.eob.me.ui.screens.DashboardScreen
import app.eob.me.ui.screens.EobSplashScreen
import app.eob.me.ui.screens.HomeScreen
import app.eob.me.ui.screens.IntroScreen
import app.eob.me.ui.screens.LanguageScreen
import app.eob.me.ui.screens.NewsScreen
import app.eob.me.ui.screens.ProfileScreen
import app.eob.me.util.OcrProcessor
import app.eob.me.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun EobNavHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val language by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val introStep by viewModel.introStep.collectAsStateWithLifecycle()
    val isSignUp by viewModel.isSignUp.collectAsStateWithLifecycle()
    val awaitingEmailVerification by viewModel.awaitingEmailVerification.collectAsStateWithLifecycle()
    val authMessage by viewModel.authMessage.collectAsStateWithLifecycle()

    LaunchedEffect(currentScreen) {
        val targetRoute = currentScreen.route
        if (navController.currentDestination?.route != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Splash.route) {
            EobSplashScreen(
                modifier = Modifier.fillMaxSize(),
                onSplashComplete = viewModel::onSplashComplete
            )
        }
        composable(Screen.Language.route) {
            LanguageScreen(
                modifier = Modifier.fillMaxSize(),
                onSelected = viewModel::onLanguageSelected
            )
        }
        composable(Screen.Intro.route) {
            IntroScreen(
                language = language,
                step = introStep,
                modifier = Modifier.fillMaxSize(),
                onNext = viewModel::onIntroNext
            )
        }
        composable(Screen.AuthChoice.route) {
            AuthChoiceScreen(
                language = language,
                modifier = Modifier.fillMaxSize(),
                onCreateAccount = viewModel::onCreateAccountSelected,
                onSignIn = viewModel::onSignInSelected
            )
        }
        composable(Screen.Auth.route) {
            AuthScreen(
                language = language,
                profile = profile,
                isSignUp = isSignUp == true,
                awaitingEmailVerification = awaitingEmailVerification,
                authMessage = authMessage,
                modifier = Modifier.fillMaxSize(),
                onProfileChanged = viewModel::onProfileChanged,
                onToggleMode = viewModel::onAuthToggleMode,
                onSubmit = viewModel::onAuthSubmit,
                onForgotPassword = viewModel::onForgotPassword,
                onForgotUsername = viewModel::onForgotUsername,
                onResendVerification = {},
                onRefreshVerification = viewModel::onRefreshVerification
            )
        }
        composable(Screen.MainHub.route) {
            MainHubNavHost(
                language = language,
                profile = profile,
                firebaseRepository = viewModel.firebaseRepository,
                onProfileChanged = viewModel::onProfileChanged,
                onLanguageChanged = viewModel::onLanguageChanged,
                onLogout = viewModel::onLogout,
                onActivity = viewModel::updateActivityTime
            )
        }
    }
}

@Composable
private fun MainHubNavHost(
    language: AppLanguage,
    profile: UserProfile,
    firebaseRepository: FirebaseEobRepository,
    onProfileChanged: (UserProfile) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onActivity: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val eobViewModel: EobViewModel = viewModel()

    val historyRecords by remember {
        derivedStateOf { eobViewModel.records.sortedBy { it.serviceDateSortKey } }
    }

    fun prepareAndUpload(uri: Uri, sourceName: String) {
        scope.launch {
            runCatching { OcrProcessor.prepareUriForUpload(context, uri) }
                .onSuccess { preparedUri ->
                    eobViewModel.uploadEobFile(
                        repository = firebaseRepository,
                        userId = eobViewModel.firebaseStatus.userId,
                        uri = preparedUri,
                        sourceName = sourceName,
                        language = language
                    )
                    navController.navigate(EobRoute.History.route) { launchSingleTop = true }
                    onActivity()
                }
                .onFailure {
                    Toast.makeText(context, EobStrings.t(language, "ocrFailed"), Toast.LENGTH_SHORT).show()
                }
        }
    }

    val libraryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            prepareAndUpload(uri, EobStrings.t(language, "libraryUpload"))
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) navController.navigate(EobRoute.CameraCapture.route)
        else Toast.makeText(context, EobStrings.t(language, "cameraPermissionRequired"), Toast.LENGTH_SHORT).show()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        eobViewModel.firebaseStatus = firebaseRepository.status()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(profile.email, profile.password, profile.isComplete) {
        if (profile.isComplete && eobViewModel.firebaseStatus.userId.isNotBlank()) {
            firebaseRepository.saveProfile(eobViewModel.firebaseStatus.userId, profile) {}
            firebaseRepository.saveInsuranceCardMetadata(eobViewModel.firebaseStatus.userId, profile) {}
        }
    }

    DisposableEffect(eobViewModel.firebaseStatus.userId) {
        val userId = eobViewModel.firebaseStatus.userId
        val profileListener = firebaseRepository.observeProfile(
            userId = userId,
            currentPassword = profile.password,
            onProfile = {
                onProfileChanged(it)
                onActivity()
            },
            onError = { eobViewModel.firebaseStatus = eobViewModel.firebaseStatus.copy(message = it) }
        )
        val eobListener = firebaseRepository.observeEobs(
            userId = userId,
            onRecords = {
                eobViewModel.replaceRecords(it, profile)
                onActivity()
            },
            onError = { eobViewModel.firebaseStatus = eobViewModel.firebaseStatus.copy(message = it) }
        )
        val newsListener = firebaseRepository.observeInsuranceNews(
            onNews = {
                eobViewModel.firebaseNews = it
                onActivity()
            },
            onError = { eobViewModel.firebaseStatus = eobViewModel.firebaseStatus.copy(message = it) }
        )
        onDispose {
            profileListener?.remove()
            eobListener?.remove()
            newsListener?.remove()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: EobRoute.Home.route
    val selectedTabIndex = primaryRoutes.indexOfFirst { it.route == currentRoute }
        .takeIf { it >= 0 }
        ?: primaryRoutes.indexOf(EobRoute.History).coerceAtLeast(0)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(EobStrings.t(language, "scanBill"), modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Header(language, onProfile = { navController.navigate(EobRoute.Profile.route) }, onLogout = onLogout)
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 8.dp
            ) {
                primaryRoutes.forEach { route ->
                    Tab(
                        selected = currentRoute == route.route,
                        onClick = {
                            navController.navigate(route.route) {
                                launchSingleTop = true
                                popUpTo(EobRoute.Home.route)
                            }
                            onActivity()
                        },
                        text = { Text(route.title(language)) }
                    )
                }
            }
            NavHost(navController = navController, startDestination = EobRoute.Home.route) {
                composable(EobRoute.Home.route) {
                    HomeScreen(
                        language = language,
                        profile = profile,
                        records = historyRecords,
                        appointments = eobViewModel.appointments.sortedBy { it.date },
                        uploadNotice = eobViewModel.uploadNotice,
                        onAddAppointment = { date, provider, time, notes ->
                            eobViewModel.addAppointment(date, provider, time, notes)
                            onActivity()
                        },
                        onRemoveAppointment = {
                            eobViewModel.removeAppointment(it)
                            onActivity()
                        }
                    )
                }
                composable(EobRoute.History.route) {
                    AnalysisScreen(
                        language = language,
                        records = historyRecords,
                        selectedRecord = eobViewModel.selectedRecord,
                        uploadText = eobViewModel.uploadText,
                        uploadNotice = eobViewModel.uploadNotice,
                        onUploadTextChanged = {
                            eobViewModel.uploadText = it
                            onActivity()
                        },
                        onLibraryUpload = { libraryLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        onCameraScan = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        onDeleteEob = {
                            eobViewModel.deleteRecord(it, profile)
                            if (eobViewModel.firebaseStatus.userId.isNotBlank()) {
                                firebaseRepository.deleteEob(eobViewModel.firebaseStatus.userId, it) { message ->
                                    eobViewModel.updateUploadNotice(message)
                                }
                            }
                            onActivity()
                        }
                    ) {
                        eobViewModel.selectRecord(it, profile)
                        onActivity()
                    }
                }
                composable(EobRoute.Dashboard.route) {
                    DashboardScreen(language = language, records = historyRecords)
                }
                composable(EobRoute.CameraCapture.route) {
                    CameraCaptureScreen(
                        language = language,
                        onImageCaptured = { uri ->
                            prepareAndUpload(uri, EobStrings.t(language, "cameraScan"))
                            navController.popBackStack(EobRoute.History.route, inclusive = false)
                        },
                        onClose = { navController.popBackStack() }
                    )
                }
                composable(EobRoute.CptCount.route) {
                    CptCountScreen(language, historyRecords, eobViewModel.selectedCptCategory) {
                        eobViewModel.selectedCptCategory = it
                        onActivity()
                    }
                }
                composable(EobRoute.News.route) {
                    NewsScreen(
                        language = language,
                        newsItems = EobKnowledgeBase.currentNewsReleases(
                            eobViewModel.visibleNews(EobKnowledgeBase.newsReleases)
                        ),
                        onDeleteNews = { eobViewModel.deleteNews(it) }
                    )
                }
                composable(EobRoute.Appeal.route) {
                    AppealScreen(
                        language,
                        profile,
                        eobViewModel.selectedRecord,
                        eobViewModel.appealLetter,
                        {
                            eobViewModel.regenerateAppeal(profile)
                            onActivity()
                        },
                        {
                            eobViewModel.updateAppeal(it)
                            onActivity()
                        }
                    )
                }
                composable(EobRoute.Profile.route) {
                    ProfileScreen(
                        language = language,
                        profile = profile,
                        onProfileChanged = {
                            onProfileChanged(it)
                            if (eobViewModel.firebaseStatus.userId.isNotBlank()) {
                                firebaseRepository.saveProfile(eobViewModel.firebaseStatus.userId, it) {}
                                firebaseRepository.saveInsuranceCardMetadata(eobViewModel.firebaseStatus.userId, it) {}
                            }
                            onActivity()
                        },
                        onLanguageChanged = onLanguageChanged,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(language: AppLanguage, onProfile: () -> Unit, onLogout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("EOBme", style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(onClick = { expanded = true }) { Text("👤") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(EobStrings.t(language, "profile")) },
                onClick = {
                    expanded = false
                    onProfile()
                }
            )
            DropdownMenuItem(
                text = { Text(EobStrings.t(language, "support")) },
                onClick = {
                    expanded = false
                    onProfile()
                }
            )
            DropdownMenuItem(
                text = { Text(EobStrings.t(language, "logout")) },
                onClick = {
                    expanded = false
                    onLogout()
                }
            )
        }
    }
}

private fun EobRoute.title(language: AppLanguage): String {
    return when (this) {
        EobRoute.Home -> EobStrings.t(language, "home")
        EobRoute.History -> EobStrings.t(language, "history")
        EobRoute.Dashboard -> EobStrings.t(language, "dashboard")
        EobRoute.CptCount -> EobStrings.t(language, "cptCount")
        EobRoute.News -> EobStrings.t(language, "news")
        EobRoute.Appeal -> EobStrings.t(language, "appeal")
        EobRoute.Profile -> EobStrings.t(language, "profile")
        EobRoute.CameraCapture -> EobStrings.t(language, "scanBill")
    }
}
