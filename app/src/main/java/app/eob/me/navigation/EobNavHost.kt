package app.eob.me.navigation

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.eob.me.billing.SubscriptionState
import app.eob.me.viewmodel.SubscriptionViewModel
import app.eob.me.ui.components.HubSettingsGearIcon
import app.eob.me.ui.screens.SettingsScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.eob.me.data.AppLanguage
import app.eob.me.data.DocumentScanPipelineState
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.UserProfile
import app.eob.me.data.repository.EobRepository
import app.eob.me.ui.components.DocumentProcessingOverlay
import app.eob.me.ui.components.EobDeleteBar
import app.eob.me.ui.components.HubBottomBar
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.navigation.HubBottomTab
import app.eob.me.ui.screens.AppealScreen
import app.eob.me.ui.screens.AuthChoiceScreen
import app.eob.me.ui.screens.AuthScreen
import app.eob.me.ui.screens.CameraCaptureScreen
import app.eob.me.ui.screens.CptTrackerScreen
import app.eob.me.ui.screens.DashboardScreen
import app.eob.me.ui.screens.EobSplashScreen
import app.eob.me.ui.screens.EobHistoryScreen
import app.eob.me.ui.screens.HomeScreen
import app.eob.me.ui.screens.IntroScreen
import app.eob.me.ui.screens.LanguageScreen
import app.eob.me.ui.screens.LoadingInvoiceScreen
import app.eob.me.ui.screens.NewsScreen
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionTier
import app.eob.me.ui.screens.PaywallDialog
import app.eob.me.ui.screens.ProfileScreen
import app.eob.me.ui.screens.ProviderDirectoryScreen
import app.eob.me.ui.screens.YtdExpenseScreen
import app.eob.me.scanner.GmsDocumentScannerLauncher
import app.eob.me.util.OcrProcessor
import app.eob.me.viewmodel.AppViewModel
import app.eob.me.viewmodel.EobViewModel
import app.eob.me.viewmodel.HubUiState
import kotlinx.coroutines.launch

@Composable
fun EobNavHost(
    viewModel: AppViewModel,
    onHubDarkModeChanged: (Boolean) -> Unit = {}
) {
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
        if (currentScreen != Screen.MainHub) {
            onHubDarkModeChanged(false)
        }
    }

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
                onActivity = viewModel::updateActivityTime,
                onHubDarkModeChanged = onHubDarkModeChanged
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
    onActivity: () -> Unit,
    onHubDarkModeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val eobViewModel: EobViewModel = viewModel()
    val eobRepository: EobRepository = appViewModel.eobRepository

    val uiState by eobViewModel.uiState.collectAsStateWithLifecycle()
    val documentScanState by eobViewModel.documentScanState.collectAsStateWithLifecycle()
    val sortedEobRecords by eobViewModel.sortedEobRecords.collectAsStateWithLifecycle()
    val personalizedNewsFeed by eobViewModel.personalizedNewsFeed.collectAsStateWithLifecycle()
    val firebaseUser by appViewModel.firebaseUser.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.hubSettings.darkModeEnabled) {
        onHubDarkModeChanged(uiState.hubSettings.darkModeEnabled)
    }

    fun prepareAndUpload(uri: Uri, sourceName: String) {
        val uid = firebaseUser?.uid.orEmpty()
        if (uid.isBlank()) {
            Toast.makeText(context, EobStrings.t(language, "signInBeforeUpload"), Toast.LENGTH_SHORT).show()
            return
        }
        if (!eobViewModel.canUploadOnCurrentNetwork()) {
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

    val activity = context as? FragmentActivity

    val documentScannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val scannedUri = GmsDocumentScannerLauncher.parseScanResult(result.resultCode, result.data)
        if (scannedUri != null) {
            eobViewModel.processScannedDocument(
                userId = firebaseUser?.uid.orEmpty(),
                uri = scannedUri,
                sourceName = EobStrings.t(language, "documentScannerSource"),
                language = language
            )
            navController.navigate(EobRoute.History.route) { launchSingleTop = true }
            onActivity()
        } else if (result.resultCode != android.app.Activity.RESULT_CANCELED) {
            eobViewModel.onDocumentScanLaunchFailed(
                language = language,
                message = EobStrings.t(language, "documentScanNoResult")
            )
        } else {
            eobViewModel.onDocumentScanCancelled()
        }
    }

    fun launchDocumentScanner() {
        val host = activity ?: return
        GmsDocumentScannerLauncher.buildScanRequest(
            activity = host,
            onReady = { request ->
                eobViewModel.onDocumentScanStarted()
                documentScannerLauncher.launch(request)
            },
            onFailure = { error ->
                eobViewModel.onDocumentScanLaunchFailed(
                    language = language,
                    message = error.localizedMessage.orEmpty()
                )
            }
        )
    }

    val customCameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            navController.navigate(EobRoute.CameraCapture.route) {
                launchSingleTop = true
            }
            onActivity()
        } else {
            Toast.makeText(context, EobStrings.t(language, "cameraPermissionRequired"), Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val lifecycleOwner = LocalLifecycleOwner.current
    val subscriptionViewModel: SubscriptionViewModel = viewModel()
    val subscriptionState by subscriptionViewModel.subscriptionState.collectAsStateWithLifecycle()
    val billingNoticeKey by subscriptionViewModel.billingNoticeKey.collectAsStateWithLifecycle()
    val paywallPricing by subscriptionViewModel.paywallPricing.collectAsStateWithLifecycle()

    fun launchManageSubscriptionFlow() {
        eobViewModel.showPaywall(eobViewModel.billingNoticeForPaywall(language))
        onActivity()
    }

    fun launchTierPurchaseFlow(tier: SubscriptionTier, interval: BillingInterval) {
        val host = activity
        if (host == null) {
            eobViewModel.handleBillingNoticeForPaywall(language, "billing_flow_failed")
            onActivity()
            return
        }
        eobViewModel.beginPaywallPurchase()
        subscriptionViewModel.launchPurchaseFlow(host, tier, interval)
        onActivity()
    }

    LaunchedEffect(Unit) {
        subscriptionViewModel.startBilling()
    }

    LaunchedEffect(firebaseUser?.uid) {
        subscriptionViewModel.bindUser(firebaseUser?.uid.orEmpty())
    }

    LaunchedEffect(subscriptionState) {
        eobViewModel.applySubscriptionState(subscriptionState)
    }

    LaunchedEffect(billingNoticeKey, language) {
        billingNoticeKey?.let { key ->
            eobViewModel.handleBillingNoticeForPaywall(language, key)
            subscriptionViewModel.clearBillingNotice()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            subscriptionViewModel.stopBilling()
        }
    }

    DisposableEffect(lifecycleOwner, uiState.hubSettings.pinLockEnabled) {
        if (!uiState.hubSettings.pinLockEnabled) {
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

    LaunchedEffect(uiState.hubSettings.pinLockEnabled) {
        if (!uiState.hubSettings.pinLockEnabled && uiState.hubSettings.appLocked) {
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
    val showHubHeader = currentRoute !in hubRoutesWithoutBottomBar
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
        EobStrings.tf(language, "settingsAppVersion", versionName)
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

    BackHandler(
        enabled = currentRoute == EobRoute.Home.route &&
            !uiState.paywallVisible &&
            !uiState.hubSettings.appLocked
    ) {
        eobViewModel.resetHubState()
        appViewModel.exitHubToSignIn()
    }

    BackHandler(
        enabled = currentRoute == EobRoute.News.route && uiState.selectedInsuranceArticle != null
    ) {
        eobViewModel.dismissInsuranceArticle()
    }

    BackHandler(enabled = uiState.paywallVisible) {
        eobViewModel.dismissPaywall()
        onActivity()
    }

    BackHandler(enabled = uiState.hubSettings.appLocked) {
        // Consume back while the PIN lock overlay is active.
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
                                    customCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            if (showHubHeader) {
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
            }
            NavHost(
                navController = navController,
                startDestination = EobRoute.Home.route,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                composable(EobRoute.Home.route) {
                    val hubTimeKey = eobViewModel.hubTimeKey()
                    val taxVaultFilterState by eobViewModel.taxVaultFilterState.collectAsStateWithLifecycle()
                    val taxVaultVisibilityMode by eobViewModel.taxVaultVisibilityMode.collectAsStateWithLifecycle()
                    val historySnapshot = remember(
                        sortedEobRecords,
                        hubTimeKey,
                        taxVaultFilterState,
                        taxVaultVisibilityMode
                    ) {
                        eobViewModel.historyBentoSnapshot()
                    }
                    val taxVaultBudgetSummary = remember(
                        sortedEobRecords,
                        profile.hsaAllocation,
                        profile.fsaAllocation,
                        taxVaultFilterState
                    ) {
                        eobViewModel.taxVaultBudgetSummary(profile)
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
                    val insuranceNewsRotationSlot = eobViewModel.insuranceNewsRotationSlot()
                    val insuranceNewsBentoSnapshot = remember(
                        sortedEobRecords,
                        language,
                        hubTimeKey,
                        uiState.newsFeedRevision,
                        insuranceNewsRotationSlot,
                        personalizedNewsFeed,
                        uiState.selectedCptCategory,
                        profile.city,
                        profile.state
                    ) {
                        eobViewModel.insuranceNewsBentoSnapshot(language)
                    }
                    val insuranceCardDisplay = remember(profile, language) {
                        eobViewModel.insuranceCardDisplay(profile, language)
                    }
                    HomeScreen(
                        language = language,
                        darkModeEnabled = uiState.hubSettings.darkModeEnabled,
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
                        taxVaultFilterState = taxVaultFilterState,
                        taxVaultVisibilityMode = taxVaultVisibilityMode,
                        taxVaultBudgetSummary = taxVaultBudgetSummary,
                        subscriptionTier = uiState.hubSettings.subscriptionTier,
                        onPremiumFeatureLocked = {
                            eobViewModel.showPaywall(eobViewModel.billingNoticeForPaywall(language))
                            onActivity()
                        },
                        onTaxVaultFilterSelected = { filter ->
                            eobViewModel.setTaxVaultFilterState(filter)
                            onActivity()
                        },
                        onTaxVaultVisibilityModeSelected = { mode ->
                            eobViewModel.setTaxVaultVisibilityMode(mode)
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
                    val ytdExpenseData = remember(sortedEobRecords, profile, eobViewModel.hubTimeKey()) {
                        eobViewModel.ytdExpenseData(profile)
                    }
                    YtdExpenseScreen(
                        language = language,
                        data = ytdExpenseData,
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
                        onViewProviderRecords = { providerName ->
                            eobViewModel.openProviderRecordHistory(providerName)
                            navController.navigate(EobRoute.History.route) {
                                launchSingleTop = true
                            }
                            onActivity()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                composable(EobRoute.CameraCapture.route) {
                    CameraCaptureScreen(
                        language = language,
                        autoCropEnabled = uiState.hubSettings.autoCropEnabled,
                        imageCompression = eobViewModel.imageCompressionLevel(),
                        onImageCaptured = { uri ->
                            eobViewModel.processScannedDocument(
                                userId = firebaseUser?.uid.orEmpty(),
                                uri = uri,
                                sourceName = EobStrings.t(language, "cameraScan"),
                                language = language
                            )
                            navController.navigate(EobRoute.History.route) {
                                launchSingleTop = true
                            }
                            onActivity()
                        },
                        onClose = { navController.popBackStack() }
                    )
                }
                composable(EobRoute.CptCount.route) {
                    val cptFlashcardEntries = remember(
                        sortedEobRecords,
                        uiState.selectedCptCategory,
                        language
                    ) {
                        eobViewModel.cptFlashcardEntries(
                            records = sortedEobRecords,
                            category = uiState.selectedCptCategory,
                            language = language
                        )
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        CptTrackerScreen(
                            language = language,
                            entries = cptFlashcardEntries,
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
                    val newsFeedRevision = uiState.newsFeedRevision
                    val selectedNewsCarrier = uiState.selectedNewsCarrier
                    val hubTimeKey = eobViewModel.hubTimeKey()
                    val insuranceNewsRotationSlot = eobViewModel.insuranceNewsRotationSlot()
                    val carrierHubItems = remember(hubTimeKey) {
                        eobViewModel.insuranceCarrierHubItems()
                    }
                    val newsItems = remember(
                        newsFeedRevision,
                        insuranceNewsRotationSlot,
                        personalizedNewsFeed,
                        selectedNewsCarrier
                    ) {
                        eobViewModel.filteredNewsReleases(EobKnowledgeBase.newsReleases)
                    }
                    NewsScreen(
                        language = language,
                        carrierHubItems = carrierHubItems,
                        selectedCarrier = selectedNewsCarrier,
                        onCarrierSelected = { carrier ->
                            eobViewModel.setSelectedNewsCarrier(carrier)
                            carrierHubItems.firstOrNull { it.carrier == carrier }
                                ?.featuredArticle
                                ?.let(eobViewModel::openInsuranceArticle)
                            onActivity()
                        },
                        selectedInsuranceArticle = uiState.selectedInsuranceArticle,
                        onDismissInsuranceArticle = eobViewModel::dismissInsuranceArticle,
                        newsItems = newsItems,
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
                        onPinLockToggle = { enabled ->
                            if (enabled && !eobViewModel.isAppPinConfigured()) {
                                eobViewModel.updateSettingsNotice(
                                    EobStrings.t(language, "settingsPinRequired")
                                )
                            } else {
                                eobViewModel.setPinLockEnabled(enabled)
                            }
                            onActivity()
                        },
                        onSavePin = { pin, confirmPin ->
                            val message = eobViewModel.saveAppPin(pin, confirmPin, language)
                            eobViewModel.updateSettingsNotice(message)
                            onActivity()
                            message == EobStrings.t(language, "settingsPinSaved")
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
                        darkModeEnabled = uiState.hubSettings.darkModeEnabled,
                        onDarkModeChanged = {
                            eobViewModel.setDarkModeEnabled(it)
                            onActivity()
                        },
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
            DocumentProcessingOverlay(
                language = language,
                state = documentScanState,
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(documentScanState) {
                when (val state = documentScanState) {
                    is DocumentScanPipelineState.Success -> {
                        Toast.makeText(
                            context,
                            EobStrings.t(language, "documentScanSuccess"),
                            Toast.LENGTH_SHORT
                        ).show()
                        eobViewModel.dismissDocumentScanState()
                    }
                    is DocumentScanPipelineState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        eobViewModel.dismissDocumentScanState()
                    }
                    else -> Unit
                }
            }
            if (uiState.hubSettings.appLocked) {
                var unlockPin by remember { mutableStateOf("") }
                var unlockPinError by remember { mutableStateOf("") }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(app.eob.me.ui.theme.EobCyberOverlay),
                    contentAlignment = Alignment.Center
                ) {
                    AppLockOverlay(
                        language = language,
                        pinInput = unlockPin,
                        pinError = unlockPinError,
                        onPinInputChanged = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() }) {
                                unlockPin = value
                                unlockPinError = ""
                            }
                        },
                        onUnlock = {
                            if (eobViewModel.verifyAppPinAndUnlock(unlockPin)) {
                                unlockPin = ""
                                unlockPinError = ""
                                eobViewModel.updateSettingsNotice("")
                            } else {
                                unlockPinError = EobStrings.t(language, "settingsPinIncorrect")
                            }
                        }
                    )
                }
            }
            if (uiState.paywallVisible) {
                PaywallDialog(
                    message = uiState.paywallMessage,
                    paywallPricing = paywallPricing,
                    onPurchaseClicked = ::launchTierPurchaseFlow,
                    onDismiss = {
                        eobViewModel.dismissPaywall()
                        onActivity()
                    }
                )
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

    LaunchedEffect(uiState.historyProviderSearch) {
        if (uiState.historyProviderSearch.isNotBlank()) {
            searchQuery = uiState.historyProviderSearch
            eobViewModel.clearHistoryProviderSearch()
        }
    }

    val historyBentoFilter = uiState.historyBentoFilter
    val historyPaymentFilter = uiState.historyPaymentFilter
    val taxVaultFilterState by eobViewModel.taxVaultFilterState.collectAsStateWithLifecycle()
    val taxVaultVisibilityMode by eobViewModel.taxVaultVisibilityMode.collectAsStateWithLifecycle()
    val filteredRecords by remember(
        sortedEobRecords,
        searchQuery,
        historyBentoFilter,
        taxVaultFilterState,
        taxVaultVisibilityMode
    ) {
        derivedStateOf {
            eobViewModel.historyRecordsForDisplay(historyBentoFilter, searchQuery)
        }
    }

    val timelineSections by remember(
        searchQuery,
        historyBentoFilter,
        historyPaymentFilter,
        taxVaultFilterState,
        taxVaultVisibilityMode,
        sortedEobRecords
    ) {
        derivedStateOf {
            eobViewModel.historyTimelineSections(
                bentoFilter = historyBentoFilter,
                searchQuery = searchQuery,
                paymentFilter = historyPaymentFilter,
                language = language
            )
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
                modifier = Modifier.fillMaxWidth(),
                label = { Text(EobStrings.t(language, "history")) },
                placeholder = { Text(EobStrings.t(language, "provider")) },
                singleLine = true
            )
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
                EobHistoryScreen(
                    language = language,
                    timelineSections = timelineSections,
                    paymentFilter = historyPaymentFilter,
                    onPaymentFilterSelected = { filter ->
                        eobViewModel.setHistoryPaymentFilter(filter)
                        onActivity()
                    },
                    onDeleteEob = onDeleteEob,
                    onUploadEob = onLibraryUpload,
                    onRecordSelected = { record ->
                        eobViewModel.selectRecord(record, profile)
                        onActivity()
                    },
                    showVaultFilterBanner = eobViewModel.isTaxVaultHistoryGated(),
                    taxVaultFilterState = taxVaultFilterState,
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
    pinInput: String,
    pinError: String,
    onPinInputChanged: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = EobStrings.t(language, "settingsAppLocked"),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = EobStrings.t(language, "settingsEnterPin"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = pinInput,
                onValueChange = onPinInputChanged,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            if (pinError.isNotBlank()) {
                Text(
                    text = pinError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = onUnlock,
                enabled = pinInput.length == 5
            ) {
                Text(EobStrings.t(language, "settingsUnlock"))
            }
        }
    }
}
