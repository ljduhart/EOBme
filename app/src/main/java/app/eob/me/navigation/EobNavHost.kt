package app.eob.me.navigation

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.eob.me.billing.PlayBillingManager
import app.eob.me.security.BiometricAuthManager
import app.eob.me.ui.components.HubSettingsGearIcon
import app.eob.me.ui.screens.SettingsScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.eob.me.data.AppLanguage
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile
import app.eob.me.data.repository.EobRepository
import app.eob.me.ui.components.EobDeleteBar
import app.eob.me.ui.components.HubBottomBar
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.navigation.HubBottomTab
import app.eob.me.ui.screens.AppealScreen
import app.eob.me.ui.screens.AuthChoiceScreen
import app.eob.me.ui.screens.AuthScreen
import app.eob.me.ui.screens.CameraCaptureScreen
import app.eob.me.ui.screens.CptCountScreen
import app.eob.me.ui.screens.DashboardScreen
import app.eob.me.ui.screens.EobSplashScreen
import app.eob.me.ui.screens.HistoryGridScreen
import app.eob.me.ui.screens.HomeScreen
import app.eob.me.ui.screens.IntroScreen
import app.eob.me.ui.screens.LanguageScreen
import app.eob.me.ui.screens.LoadingInvoiceScreen
import app.eob.me.ui.screens.NewsScreen
import app.eob.me.ui.screens.ProfileScreen
import app.eob.me.ui.screens.ProviderDirectoryScreen
import app.eob.me.ui.screens.YearlyExpenseScreen
import app.eob.me.ui.history.HistoryPagination
import app.eob.me.util.OcrProcessor
import app.eob.me.viewmodel.AppViewModel
import app.eob.me.viewmodel.EobViewModel
import app.eob.me.viewmodel.HubUiState
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
    val registrationCredentials by viewModel.registrationCredentials.collectAsStateWithLifecycle()

    LaunchedEffect(currentScreen) {
        val targetRoute = currentScreen.route
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(Screen.Splash.route) { saveState = true }
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
                step = introStep.coerceIn(0, AppViewModel.INTRO_SLIDE_COUNT - 1),
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
                credentials = registrationCredentials,
                isSignUp = isSignUp == true,
                awaitingEmailVerification = awaitingEmailVerification,
                authMessage = authMessage,
                modifier = Modifier.fillMaxSize(),
                onProfileChanged = viewModel::onProfileChanged,
                onCredentialsChanged = viewModel::onCredentialsChanged,
                onToggleMode = viewModel::onAuthToggleMode,
                onSubmit = viewModel::onAuthSubmit,
                onForgotPassword = viewModel::onForgotPassword,
                onForgotUsername = viewModel::onForgotUsername,
                onResendVerification = viewModel::onResendVerification,
                onRefreshVerification = viewModel::onRefreshVerification
            )
        }
        composable(Screen.MainHub.route) {
            MainHubNavHost(
                appViewModel = viewModel,
                language = language,
                profile = profile,
                credentials = registrationCredentials,
                onProfileChanged = viewModel::onProfileChanged,
                onCredentialsChanged = viewModel::onCredentialsChanged,
                onLanguageChanged = viewModel::onLanguageChanged,
                onLogout = viewModel::onLogout,
                onActivity = viewModel::updateActivityTime
            )
        }
    }
}

@Composable
private fun MainHubNavHost(
    appViewModel: AppViewModel,
    language: AppLanguage,
    profile: UserProfile,
    credentials: RegistrationCredentials,
    onProfileChanged: (UserProfile) -> Unit,
    onCredentialsChanged: (RegistrationCredentials) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onActivity: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val eobViewModel: EobViewModel = viewModel()
    val eobRepository: EobRepository = appViewModel.eobRepository

    val uiState by eobViewModel.uiState.collectAsStateWithLifecycle()
    val insuranceArticles by eobViewModel.insuranceArticles.collectAsStateWithLifecycle()
    val sortedEobRecords by eobViewModel.sortedEobRecords.collectAsStateWithLifecycle()
    val firebaseUser by appViewModel.firebaseUser.collectAsStateWithLifecycle()

    fun prepareAndUpload(uri: Uri, sourceName: String) {
        val uid = firebaseUser?.uid.orEmpty()
        if (uid.isBlank()) {
            Toast.makeText(context, EobStrings.t(language, "signInBeforeUpload"), Toast.LENGTH_SHORT).show()
            return
        }
        if (!eobViewModel.canUploadOnCurrentNetwork(context)) {
            Toast.makeText(context, EobStrings.t(language, "settingsUploadWifiBlocked"), Toast.LENGTH_LONG).show()
            return
        }
        val compression = eobViewModel.imageCompressionLevel()
        scope.launch {
            runCatching { OcrProcessor.prepareUriForUpload(context, uri, compression) }
                .onSuccess { preparedUri ->
                    eobViewModel.uploadEobFile(
                        userId = uid,
                        uri = preparedUri,
                        sourceName = sourceName,
                        language = language
                    )
                    navController.navigate(EobRoute.History.route) { launchSingleTop = true }
                    onActivity()
                }
                .onFailure {
                    eobViewModel.setLoadingInvoice(false)
                    Toast.makeText(
                        context,
                        EobStrings.t(language, "imagePrepFailed"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    var profileSaveMessage by remember { mutableStateOf("") }
    var openProfileSupport by remember { mutableStateOf(false) }

    val libraryUploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            prepareAndUpload(it, EobStrings.t(language, "libraryUpload"))
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) navController.navigate(EobRoute.CameraCapture.route)
        else Toast.makeText(context, EobStrings.t(language, "cameraPermissionRequired"), Toast.LENGTH_SHORT).show()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? FragmentActivity
    var playBillingManager by remember { mutableStateOf<PlayBillingManager?>(null) }

    fun billingNoticeMessage(key: String): String {
        return when (key) {
            "billing_not_ready" -> EobStrings.t(language, "billingNotReady")
            "billing_product_unavailable" -> EobStrings.t(language, "billingProductUnavailable")
            else -> EobStrings.t(language, "billingFlowFailed")
        }
    }

    fun launchManageSubscriptionFlow() {
        val host = activity ?: return
        val manager = playBillingManager ?: PlayBillingManager(
            activity = host,
            onTierChanged = eobViewModel::setSubscriptionTier,
            onBillingMessage = { key -> eobViewModel.updateSettingsNotice(billingNoticeMessage(key)) }
        ).also { playBillingManager = it }
        manager.start()
        manager.launchManageSubscription()
        onActivity()
    }

    DisposableEffect(Unit) {
        onDispose {
            playBillingManager?.endConnection()
            playBillingManager = null
        }
    }

    DisposableEffect(lifecycleOwner, uiState.hubSettings.biometricLoginEnabled) {
        if (!uiState.hubSettings.biometricLoginEnabled) {
            return@DisposableEffect onDispose { }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> eobViewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> eobViewModel.onAppForegrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.hubSettings.biometricLoginEnabled) {
        if (!uiState.hubSettings.biometricLoginEnabled && uiState.hubSettings.appLocked) {
            eobViewModel.unlockApp()
        }
    }

    LaunchedEffect(Unit) {
        eobViewModel.attachRepository(eobRepository, context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(firebaseUser?.uid) {
        eobViewModel.refreshFirebaseStatus()
    }

    val userId = firebaseUser?.uid.orEmpty()

    LaunchedEffect(userId) {
        if (userId.isBlank()) {
            eobViewModel.resetHubState()
            return@LaunchedEffect
        }
        eobViewModel.startFirestoreSync(
            userId = userId,
            profile = profile,
            onProfileChanged = { updated ->
                appViewModel.applyRemoteProfile(updated)
                onActivity()
            }
        )
    }

    LaunchedEffect(profile, userId) {
        if (userId.isNotBlank()) {
            eobViewModel.updateSyncProfile(profile)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: EobRoute.Home.route
    val showBack = currentRoute in hubBackRoutes
    val showSettingsGear = currentRoute == EobRoute.Home.route
    val showBottomBar = currentRoute !in hubRoutesWithoutBottomBar
    val selectedBottomTab = HubBottomTab.fromRoute(currentRoute)
    var settingsDraftFirstName by remember(profile.firstName) { mutableStateOf(profile.firstName) }
    var settingsDraftLastName by remember(profile.lastName) { mutableStateOf(profile.lastName) }

    LaunchedEffect(profile.firstName, profile.lastName, uiState.hubSettings.settingsAccountEditing) {
        if (!uiState.hubSettings.settingsAccountEditing) {
            settingsDraftFirstName = profile.firstName
            settingsDraftLastName = profile.lastName
        }
    }

    val appVersionLabel = remember(language) {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val versionName = packageInfo?.versionName.orEmpty().ifBlank { "1.0.0" }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toInt() ?: 0
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode ?: 0
        }
        EobStrings.tf(language, "settingsAppVersion", versionName, versionCode)
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == EobRoute.Settings.route) {
            eobViewModel.refreshCacheSize()
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != EobRoute.News.route && uiState.selectedInsuranceArticle != null) {
            eobViewModel.dismissInsuranceArticle()
        }
    }

    fun deleteEob(record: EobRecord) {
        eobViewModel.deleteRecordRemote(userId, record, profile, language) { message ->
            if (message.isNotBlank()) eobViewModel.updateUploadNotice(message)
        }
        onActivity()
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HubBottomBar(
                    language = language,
                    selectedTab = selectedBottomTab,
                    scanEnabled = userId.isNotBlank(),
                    onTabSelected = { tab ->
                        when (tab) {
                            HubBottomTab.Dashboard -> {
                                navController.navigate(EobRoute.Dashboard.route) {
                                    launchSingleTop = true
                                }
                                onActivity()
                            }
                            HubBottomTab.ScanEob -> {
                                if (userId.isNotBlank()) {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    Toast.makeText(
                                        context,
                                        EobStrings.t(language, "signInBeforeUpload"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            HubBottomTab.Profile -> {
                                openProfileSupport = false
                                navController.navigate(EobRoute.Profile.route) {
                                    launchSingleTop = true
                                }
                                onActivity()
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            HubHeader(
                language = language,
                showBack = showBack,
                showSettingsGear = showSettingsGear,
                onBack = {
                    navController.navigate(EobRoute.Home.route) {
                        popUpTo(EobRoute.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    onActivity()
                },
                onOpenSettings = {
                    navController.navigate(EobRoute.Settings.route) { launchSingleTop = true }
                    onActivity()
                }
            )
            NavHost(navController = navController, startDestination = EobRoute.Home.route) {
                composable(EobRoute.Home.route) {
                    val hubTimeKey = eobViewModel.hubTimeKey()
                    val historySnapshot = remember(sortedEobRecords, hubTimeKey) {
                        eobViewModel.historyBentoSnapshot()
                    }
                    val providerAvatars = remember(sortedEobRecords, language) {
                        eobViewModel.providerAvatarPreviews(language)
                    }
                    val careTeamCards = remember(
                        uiState.preferredDoctors,
                        uiState.appointments,
                        sortedEobRecords,
                        uiState.isLoadingInvoice,
                        uiState.invoiceProcessingPhase,
                        language
                    ) {
                        eobViewModel.careTeamCardStates(language)
                    }
                    val providerDirectoryAssurance = remember(
                        uiState.preferredDoctors,
                        sortedEobRecords,
                        uiState.isLoadingInvoice,
                        uiState.invoiceProcessingPhase,
                        language
                    ) {
                        eobViewModel.providerDirectoryAssurance(language)
                    }
                    val cptBentoSnapshot = remember(
                        sortedEobRecords,
                        language,
                        uiState.selectedCptCategory,
                        hubTimeKey
                    ) {
                        eobViewModel.cptBentoSnapshot(language)
                    }
                    val ytdBentoSnapshot = remember(
                        sortedEobRecords,
                        profile.annualDeductibleLimit,
                        profile.annualOutOfPocketMax,
                        hubTimeKey
                    ) {
                        eobViewModel.ytdDeductibleBentoSnapshot(profile)
                    }
                    val insuranceNewsBentoSnapshot = remember(
                        sortedEobRecords,
                        language,
                        hubTimeKey,
                        uiState.newsFeedRevision
                    ) {
                        eobViewModel.insuranceNewsBentoSnapshot(language)
                    }
                    val insuranceCardDisplay = remember(profile, language) {
                        eobViewModel.insuranceCardDisplay(profile, language)
                    }
                    HomeScreen(
                        language = language,
                        profile = profile,
                        insuranceCardDisplay = insuranceCardDisplay,
                        canEditInsuranceCard = userId.isNotBlank(),
                        onSaveInsuranceCard = { insuranceName, memberId, groupNumber, pcpCopay, specialistCopay ->
                            val updatedProfile = eobViewModel.applyInsuranceCardEdits(
                                profile = profile,
                                insuranceName = insuranceName,
                                memberId = memberId,
                                groupNumber = groupNumber,
                                pcpCopay = pcpCopay,
                                specialistCopay = specialistCopay
                            )
                            onProfileChanged(updatedProfile)
                            eobViewModel.updateSyncProfile(updatedProfile)
                            eobViewModel.saveProfileToRemote(userId, updatedProfile, language) { message ->
                                if (message.isNotBlank()) {
                                    eobViewModel.updateUploadNotice(message)
                                }
                            }
                            onActivity()
                        },
                        recordCount = sortedEobRecords.size,
                        firebaseStatusLine = EobStrings.firebaseStatusText(
                            language,
                            uiState.firebaseSyncStatus
                        ),
                        uploadNotice = uiState.uploadNotice,
                        appointments = uiState.appointments,
                        preferredDoctors = uiState.preferredDoctors,
                        careTeamCards = careTeamCards,
                        providerDirectoryAssurance = providerDirectoryAssurance,
                        cptBentoSnapshot = cptBentoSnapshot,
                        insuranceNewsBentoSnapshot = insuranceNewsBentoSnapshot,
                        ytdBentoSnapshot = ytdBentoSnapshot,
                        ytdBentoViewMode = uiState.ytdBentoViewMode,
                        onYtdBentoViewModeSelected = eobViewModel::setYtdBentoViewMode,
                        calendarExpanded = uiState.calendarExpanded,
                        onCalendarExpandedChange = eobViewModel::setCalendarExpanded,
                        onSavePreferredDoctor = { doctor ->
                            eobViewModel.updatePreferredDoctor(doctor)
                            onActivity()
                        },
                        onAddAppointment = { date, provider, time, notes, providerType ->
                            eobViewModel.addAppointment(date, provider, time, notes, providerType)
                            onActivity()
                        },
                        onRemoveAppointment = { appointment ->
                            eobViewModel.removeAppointment(appointment)
                            onActivity()
                        },
                        onUpdateAppointment = { id, date, provider, time, notes, providerType ->
                            eobViewModel.updateAppointment(id, date, provider, time, notes, providerType)
                            onActivity()
                        },
                        historySnapshot = historySnapshot,
                        processingPhase = uiState.invoiceProcessingPhase,
                        isLoadingInvoice = uiState.isLoadingInvoice,
                        historyFilter = uiState.historyBentoFilter,
                        providerAvatars = providerAvatars,
                        onHistoryFilterSelected = { filter ->
                            eobViewModel.setHistoryBentoFilter(filter)
                            navController.navigate(EobRoute.History.route) { launchSingleTop = true }
                            onActivity()
                        },
                        onInvoiceFileDropFinished = {
                            eobViewModel.acknowledgeInvoiceFileDropAnimation()
                        },
                        appealGeneratorBentoProcessing = uiState.appealGeneratorBentoProcessing,
                        onAppealGeneratorProcessingFinished = {
                            eobViewModel.acknowledgeAppealGeneratorBentoActivation()
                        },
                        onBentoSelected = { destination ->
                            if (destination == HubBentoDestination.AppealGenerator) {
                                eobViewModel.activateAppealGeneratorBento(profile)
                            }
                            navController.navigate(destination.route) { launchSingleTop = true }
                            onActivity()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composable(EobRoute.Dashboard.route) {
                    DashboardScreen(
                        language = language,
                        records = sortedEobRecords,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composable(EobRoute.History.route) {
                    HistoryRoute(
                        language = language,
                        profile = profile,
                        uiState = uiState,
                        sortedEobRecords = sortedEobRecords,
                        eobViewModel = eobViewModel,
                        onDeleteEob = { deleteEob(it) },
                        onLibraryUpload = {
                            libraryUploadLauncher.launch(arrayOf("image/*", "application/pdf"))
                        },
                        onActivity = onActivity
                    )
                }
                composable(EobRoute.YearlyExpense.route) {
                    val yearlySummary = remember(sortedEobRecords, eobViewModel.hubTimeKey()) {
                        eobViewModel.yearlyHealthCostSummary()
                    }
                    YearlyExpenseScreen(
                        language = language,
                        summary = yearlySummary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composable(EobRoute.ProviderDirectory.route) {
                    val providers = remember(sortedEobRecords) {
                        eobViewModel.providerDirectory()
                    }
                    ProviderDirectoryScreen(
                        language = language,
                        providers = providers,
                        records = sortedEobRecords,
                        onDeleteEob = { deleteEob(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composable(EobRoute.CameraCapture.route) {
                    CameraCaptureScreen(
                        language = language,
                        autoCropEnabled = uiState.hubSettings.autoCropEnabled,
                        onImageCaptured = { uri ->
                            prepareAndUpload(uri, EobStrings.t(language, "cameraScan"))
                            navController.popBackStack(EobRoute.History.route, inclusive = false)
                        },
                        onClose = { navController.popBackStack() }
                    )
                }
                composable(EobRoute.CptCount.route) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CptCountScreen(
                            language = language,
                            records = sortedEobRecords,
                            selectedCategory = uiState.selectedCptCategory,
                            onCategorySelected = {
                                eobViewModel.setSelectedCptCategory(it)
                                onActivity()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        EobDeleteBar(
                            language = language,
                            selectedRecord = uiState.selectedRecord,
                            onDeleteEob = { deleteEob(it) }
                        )
                    }
                }
                composable(EobRoute.News.route) {
                    NewsScreen(
                        language = language,
                        insuranceArticles = insuranceArticles,
                        selectedInsuranceArticle = uiState.selectedInsuranceArticle,
                        onInsuranceArticleSelected = eobViewModel::openInsuranceArticle,
                        onDismissInsuranceArticle = eobViewModel::dismissInsuranceArticle,
                        newsItems = eobViewModel.currentNewsReleases(EobKnowledgeBase.newsReleases),
                        onDeleteNews = { eobViewModel.deleteNews(it) }
                    )
                }
                composable(EobRoute.Appeal.route) {
                    LaunchedEffect(Unit) {
                        eobViewModel.acknowledgeAppealGeneratorBentoActivation()
                    }
                    AppealScreen(
                        language = language,
                        profile = profile,
                        selectedRecord = uiState.selectedRecord,
                        appealLetter = uiState.appealLetter,
                        appealLetterEditingEnabled = uiState.appealLetterEditingEnabled,
                        onRegenerate = {
                            eobViewModel.regenerateAppeal(profile)
                            onActivity()
                        },
                        onEditLetter = {
                            eobViewModel.updateAppeal(it)
                            onActivity()
                        },
                        onEnableEditing = {
                            eobViewModel.enableAppealLetterEditing()
                            onActivity()
                        },
                        onSaveLetter = {
                            eobViewModel.saveAppealLetter()
                            onActivity()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composable(EobRoute.Settings.route) {
                    SettingsScreen(
                        language = language,
                        profile = profile,
                        hubSettings = uiState.hubSettings,
                        appVersionLabel = appVersionLabel,
                        accountEditing = uiState.hubSettings.settingsAccountEditing,
                        draftFirstName = settingsDraftFirstName,
                        draftLastName = settingsDraftLastName,
                        onDraftFirstNameChanged = { settingsDraftFirstName = it },
                        onDraftLastNameChanged = { settingsDraftLastName = it },
                        onEnableAccountEditing = eobViewModel::enableSettingsAccountEditing,
                        onSaveAccountProfile = {
                            val updatedProfile = profile.copy(
                                firstName = settingsDraftFirstName.trim(),
                                lastName = settingsDraftLastName.trim()
                            )
                            onProfileChanged(updatedProfile)
                            eobViewModel.updateSyncProfile(updatedProfile)
                            eobViewModel.saveProfileToRemote(userId, updatedProfile, language) { message ->
                                eobViewModel.updateSettingsNotice(
                                    message.ifBlank { EobStrings.t(language, "settingsProfileSaved") }
                                )
                            }
                            eobViewModel.disableSettingsAccountEditing()
                            onActivity()
                        },
                        onCancelAccountEditing = {
                            settingsDraftFirstName = profile.firstName
                            settingsDraftLastName = profile.lastName
                            eobViewModel.disableSettingsAccountEditing()
                        },
                        onManageSubscription = ::launchManageSubscriptionFlow,
                        onLogout = {
                            eobViewModel.resetHubState()
                            profileSaveMessage = ""
                            onLogout()
                        },
                        onDeleteAccountConfirmed = {
                            eobViewModel.deleteAccount(userId, language) { message ->
                                eobViewModel.updateSettingsNotice(message)
                                if (message == EobStrings.t(language, "settingsAccountDeleted")) {
                                    eobViewModel.resetHubState()
                                    onLogout()
                                }
                            }
                            onActivity()
                        },
                        onBiometricToggle = { enabled ->
                            if (enabled) {
                                val hostActivity = activity
                                if (hostActivity == null || !BiometricAuthManager.canAuthenticate(hostActivity)) {
                                    eobViewModel.updateSettingsNotice(
                                        EobStrings.t(language, "settingsBiometricUnavailable")
                                    )
                                } else {
                                    eobViewModel.setBiometricLoginEnabled(true)
                                }
                            } else {
                                eobViewModel.setBiometricLoginEnabled(false)
                            }
                            onActivity()
                        },
                        onAppLockTimeoutSelected = {
                            eobViewModel.setAppLockTimeout(it)
                            onActivity()
                        },
                        onCrashlyticsToggle = {
                            eobViewModel.setCrashlyticsOptIn(it)
                            onActivity()
                        },
                        onWifiOnlyToggle = {
                            eobViewModel.setUploadOverWifiOnly(it)
                            onActivity()
                        },
                        onCompressionSelected = {
                            eobViewModel.setImageCompressionLevel(it)
                            onActivity()
                        },
                        onAutoCropToggle = {
                            eobViewModel.setAutoCropEnabled(it)
                            onActivity()
                        },
                        onClearCache = {
                            eobViewModel.clearLocalCache { cleared ->
                                eobViewModel.updateSettingsNotice(
                                    EobStrings.t(
                                        language,
                                        if (cleared) "settingsCacheCleared" else "settingsCacheClearFailed"
                                    )
                                )
                            }
                            onActivity()
                        },
                        onTabSelected = {
                            eobViewModel.setSettingsTab(it)
                            onActivity()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composable(EobRoute.Profile.route) {
                    ProfileScreen(
                        language = language,
                        profile = profile,
                        credentials = credentials,
                        saveMessage = profileSaveMessage,
                        onProfileChanged = onProfileChanged,
                        onCredentialsChanged = onCredentialsChanged,
                        onEditingChanged = appViewModel::setProfileEditing,
                        onSave = { savedProfile, savedCredentials ->
                            appViewModel.saveProfileAndCredentials(savedProfile, savedCredentials) { message ->
                                profileSaveMessage = message
                            }
                        },
                        onLanguageChanged = onLanguageChanged,
                        onLogout = {
                            eobViewModel.resetHubState()
                            profileSaveMessage = ""
                            onLogout()
                        },
                        openSupportInitially = openProfileSupport
                    )
                }
            }
            }
            if (uiState.hubSettings.appLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    AppLockOverlay(
                        language = language,
                        onUnlock = {
                            val hostActivity = activity
                            if (hostActivity == null || !BiometricAuthManager.canAuthenticate(hostActivity)) {
                                eobViewModel.updateSettingsNotice(
                                    EobStrings.t(language, "settingsBiometricUnavailable")
                                )
                            } else {
                                BiometricAuthManager.showPrompt(
                                    activity = hostActivity,
                                    language = language,
                                    onSuccess = {
                                        eobViewModel.unlockApp()
                                        eobViewModel.updateSettingsNotice("")
                                    },
                                    onError = { message ->
                                        eobViewModel.updateSettingsNotice(message)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRoute(
    language: AppLanguage,
    profile: UserProfile,
    uiState: HubUiState,
    sortedEobRecords: List<EobRecord>,
    eobViewModel: EobViewModel,
    onDeleteEob: (EobRecord) -> Unit,
    onLibraryUpload: () -> Unit,
    onActivity: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val historyBentoFilter = uiState.historyBentoFilter
    val filteredRecords by remember(sortedEobRecords, searchQuery, historyBentoFilter) {
        derivedStateOf {
            eobViewModel.historyRecordsForDisplay(historyBentoFilter, searchQuery)
        }
    }

    val totalBillingErrors by remember(filteredRecords) {
        derivedStateOf { eobViewModel.totalBillingErrors(filteredRecords) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                label = { Text(EobStrings.t(language, "history")) },
                placeholder = { Text(EobStrings.t(language, "provider")) },
                singleLine = true
            )
            Button(onClick = onLibraryUpload) {
                Text(EobStrings.t(language, "uploadFromLibrary"))
            }
        }
        if (totalBillingErrors > 0) {
            Text(
                text = "$totalBillingErrors ${EobStrings.t(language, "analysis")}",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoadingInvoice) {
                LoadingInvoiceScreen(modifier = Modifier.fillMaxSize())
            } else {
                HistoryGridScreen(
                    language = language,
                    records = filteredRecords.take(HistoryPagination.MAX_EOBS),
                    selectedRecord = uiState.selectedRecord,
                    currentPage = uiState.historyPage,
                    onPageChange = {
                        eobViewModel.setHistoryPage(it)
                        onActivity()
                    },
                    onSelected = {
                        eobViewModel.selectRecord(it, profile)
                        onActivity()
                    },
                    onDeleteEob = onDeleteEob,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun HubHeader(
    language: AppLanguage,
    showBack: Boolean,
    showSettingsGear: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            TextButton(onClick = onBack) {
                Text("← ${EobStrings.t(language, "home")}")
            }
        }
        Text(
            text = EobStrings.t(language, "appBrand"),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(top = 8.dp)
                .weight(1f)
        )
        if (showSettingsGear) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = HubSettingsGearIcon.Settings,
                    contentDescription = EobStrings.t(language, "settings")
                )
            }
        }
    }
}

@Composable
private fun AppLockOverlay(
    language: AppLanguage,
    onUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = EobStrings.t(language, "settingsAppLocked"),
                style = MaterialTheme.typography.headlineSmall
            )
            Button(onClick = onUnlock) {
                Text(EobStrings.t(language, "settingsUnlock"))
            }
        }
    }
}
