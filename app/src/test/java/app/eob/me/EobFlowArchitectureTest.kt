package app.eob.me

import app.eob.me.navigation.EobRoute
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.navigation.HubBottomTab
import app.eob.me.navigation.hubBackRoutes
import app.eob.me.navigation.hubFeatureRoutes
import app.eob.me.navigation.hubRoutesWithoutBottomBar
import app.eob.me.ui.history.HistoryPagination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Static wiring checks for every major user flow: onboarding, hub nav, bento, uploads, feature screens.
 */
class EobFlowArchitectureTest {
    private val appModuleRoot = resolveAppModuleRoot()
    private val navHostSource by lazy { readSource("navigation/EobNavHost.kt") }
    private val mainActivitySource by lazy { readSource("MainActivity.kt") }
    private val manifestSource by lazy { resolveManifestFile().readText() }

    @Test
    fun mainActivityHostsEobNavHost() {
        assertTrue(mainActivitySource.contains("EobNavHost"))
        assertTrue(mainActivitySource.contains("AppViewModel"))
    }

    @Test
    fun hubUsesCyberDarkThemeWithoutTouchingOpeningScreens() {
        assertTrue(mainActivitySource.contains("EOBmeTheme(darkTheme = useDarkTheme)"))
        assertTrue(mainActivitySource.contains("eobCyberAppBackgroundGradient()"))
        assertTrue(mainActivitySource.contains("eobLightAppBackgroundGradient()"))
        assertFalse(
            "MainActivity must not reuse splash gradient on hub screens",
            mainActivitySource.contains("eobAppBackgroundGradient()")
        )
        val colorSource = readSource("ui/theme/Color.kt")
        listOf(
            "EobCyberBackground",
            "EobCyberAccent",
            "EobCyberAccentBright",
            "EobCyberSuccess",
            "EobCyberError",
            "EobCyberTextSecondary",
            "eobCyberAppBackgroundGradient",
            "eobLightAppBackgroundGradient",
            "EobLightBackground"
        ).forEach { token ->
            assertTrue("Color.kt missing cyber token: $token", colorSource.contains(token))
        }
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "ui/components/EobSplashLogo.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse(
                "$path must remain unchanged for opening flow branding",
                source.contains("EobCyber")
            )
        }
    }

    @Test
    fun profileDarkModeToggleWiresThroughEobViewModel() {
        val profileSource = readSource("ui/screens/ProfileScreen.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val storeSource = readSource("data/HubSettingsStore.kt")
        listOf(
            "darkModeEnabled",
            "onDarkModeChanged",
            "Switch(",
            "appearanceSettings",
            "setDarkModeEnabled"
        ).forEach { snippet ->
            assertTrue("Profile dark mode wiring missing: $snippet", profileSource.contains(snippet) || navHostSource.contains(snippet) || viewModelSource.contains(snippet))
        }
        assertTrue(navHostSource.contains("onHubDarkModeChanged"))
        assertTrue(navHostSource.contains("eobViewModel.setDarkModeEnabled"))
        assertTrue(storeSource.contains("KEY_DARK_MODE"))
        assertTrue(viewModelSource.contains("fun setDarkModeEnabled"))
    }

    @Test
    fun manifestDeclaresLauncherActivityAndMessagingService() {
        listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CAMERA",
            "android.permission.POST_NOTIFICATIONS",
            "com.android.vending.BILLING"
        ).forEach { permission ->
            assertTrue("Manifest missing $permission", manifestSource.contains(permission))
        }
        assertTrue(manifestSource.contains("MainActivity"))
        assertTrue(manifestSource.contains("EobApplication"))
        assertTrue(manifestSource.contains("EobFirebaseMessagingService"))
        assertFalse(
            "Manifest should not declare CALL_PHONE",
            manifestSource.contains("android.permission.CALL_PHONE")
        )
        assertFalse(
            "Manifest should not declare legacy storage permission",
            manifestSource.contains("android.permission.READ_EXTERNAL_STORAGE")
        )
    }

    @Test
    fun authChoiceWiresCreateAccountThroughAppViewModel() {
        listOf(
            "onCreateAccountSelected",
            "onSignInSelected",
            "onAuthSubmit",
            "onAuthToggleMode",
            "AuthChoiceScreen",
            "AuthScreen"
        ).forEach { snippet ->
            assertTrue("Auth onboarding missing: $snippet", navHostSource.contains(snippet))
        }
        assertTrue(
            "Intro step must be clamped while route transitions",
            navHostSource.contains("introStep.coerceIn(0, AppViewModel.INTRO_SLIDE_COUNT - 1)")
        )
    }

    @Test
    fun outerOnboardingScreensAreRegistered() {
        listOf("Splash", "Language", "Intro", "AuthChoice", "Auth", "MainHub").forEach { screen ->
            assertTrue("Missing Screen.$screen", navHostSource.contains("composable(Screen.$screen.route)"))
        }
    }

    @Test
    fun introAndSplashScreenFilesExist() {
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "ui/components/EobSplashLogo.kt"
        ).forEach { path ->
            assertTrue("Missing $path", File(appModuleRoot, path).isFile)
        }
    }

    @Test
    fun hubInnerRoutesAndFeatureScreensExist() {
        val routeToScreen = listOf(
            "EobRoute.Home.route" to "HomeScreen.kt",
            "EobRoute.History.route" to "HistoryGridScreen.kt",
            "EobRoute.Dashboard.route" to "DashboardScreen.kt",
            "EobRoute.YearlyExpense.route" to "YearlyExpenseScreen.kt",
            "EobRoute.CptCount.route" to "CptCountScreen.kt",
            "EobRoute.News.route" to "NewsScreen.kt",
            "EobRoute.Appeal.route" to "AppealScreen.kt",
            "EobRoute.Profile.route" to "ProfileScreen.kt",
            "EobRoute.Settings.route" to "SettingsScreen.kt",
            "EobRoute.CameraCapture.route" to "CameraCaptureScreen.kt",
            "EobRoute.ProviderDirectory.route" to "ProviderDirectoryScreen.kt"
        )
        routeToScreen.forEach { (routeToken, screenFile) ->
            assertTrue(
                "EobNavHost missing composable($routeToken)",
                navHostSource.contains("composable($routeToken)")
            )
            assertTrue("Missing screen $screenFile", File(appModuleRoot, "ui/screens/$screenFile").isFile)
        }
    }

    @Test
    fun bentoTilesMapToRoutesAndSmartCellsExist() {
        HubBentoDestination.entries.forEach { destination ->
            val routeToken = when (destination) {
                HubBentoDestination.ProviderDirectory -> "EobRoute.ProviderDirectory.route"
                HubBentoDestination.EobHistory -> "EobRoute.History.route"
                HubBentoDestination.CptTracker -> "EobRoute.CptCount.route"
                HubBentoDestination.YtdExpense -> "EobRoute.YearlyExpense.route"
                HubBentoDestination.InsuranceNews -> "EobRoute.News.route"
                HubBentoDestination.AppealGenerator -> "EobRoute.Appeal.route"
            }
            assertTrue("Bento ${destination.name} missing nav wiring", navHostSource.contains(routeToken))
            assertTrue(
                "Bento ${destination.name} route must be a hub feature route",
                destination.route in hubFeatureRoutes
            )
            assertTrue(
                "Bento ${destination.name} route must show hub back navigation",
                destination.route in hubBackRoutes
            )
        }
        listOf(
            "ui/components/bento/BentoCellLayout.kt",
            "ui/components/bento/HistoryBentoCell.kt",
            "ui/components/bento/ProviderDirectoryBentoCell.kt",
            "ui/components/bento/CptTrackerBentoCell.kt",
            "ui/components/bento/YtdExpenseBentoCell.kt",
            "ui/components/bento/AppealGeneratorBentoCell.kt",
            "ui/components/bento/InsuranceNewsBentoCell.kt",
            "ui/components/bento/BentoGridCell.kt"
        ).forEach { path ->
            assertTrue("Missing bento component $path", File(appModuleRoot, path).isFile)
        }
        val gridDestinations = HubBentoDestination.gridRows.flatten()
        assertEquals(HubBentoDestination.entries.size, gridDestinations.size)
        assertEquals(HubBentoDestination.entries.toSet(), gridDestinations.toSet())
        assertEquals(6, HubBentoDestination.entries.size)
        assertEquals(2, HubBentoDestination.gridRows.size)
    }

    @Test
    fun homeTaxVaultFilterWiresThroughViewModel() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val vmSource = readSource("viewmodel/EobViewModel.kt")
        val taxVaultSource = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        listOf(
            "TaxVaultVerticalFilterCard",
            "taxVaultFilterState",
            "taxVaultVisibilityMode",
            "taxVaultBudgetSummary",
            "subscriptionTier.isGold()",
            "onTaxVaultFilterSelected",
            "HomeWeekCalendar",
            "BoxWithConstraints"
        ).forEach { snippet ->
            assertTrue("HomeScreen missing tax vault wiring: $snippet", homeSource.contains(snippet))
        }
        assertTrue(
            "Tax Vault card must sit below expandable calendar and be bento-sized",
            homeSource.indexOf("HomeWeekCalendar") < homeSource.indexOf("TaxVaultVerticalFilterCard") &&
                homeSource.contains("BoxWithConstraints") &&
                homeSource.contains("BentoCellLayout.ASPECT_RATIO")
        )
        listOf(
            "enum class TaxVaultFilterState",
            "enum class TaxVaultVisibilityMode",
            "MutableStateFlow(TaxVaultFilterState.OFF)",
            "setTaxVaultFilterState",
            "setTaxVaultVisibilityMode",
            "isTaxVaultGoldUnlocked",
            "taxVaultBudgetSummary",
            "recordsForTaxVaultFilter",
            "isHsaEligible",
            "isFsaEligible"
        ).forEach { snippet ->
            assertTrue("EobViewModel/data missing tax vault logic: $snippet", vmSource.contains(snippet) || readSource("data/EobModels.kt").contains(snippet) || readSource("data/EobAnalyzer.kt").contains(snippet))
        }
        listOf(
            "TaxVaultVerticalFilterCard",
            "taxVaultFilterTitle",
            "taxVaultGoldLocked",
            "taxVaultCareTeamBorder",
            "VaultUiPhase"
        ).forEach { snippet ->
            assertTrue("TaxVaultVerticalFilterCard missing: $snippet", taxVaultSource.contains(snippet))
        }
        listOf(
            "taxVaultFilterState",
            "taxVaultVisibilityMode",
            "taxVaultBudgetSummary",
            "setTaxVaultFilterState",
            "setTaxVaultVisibilityMode"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing tax vault wiring: $snippet", navHostSource.contains(snippet))
        }
        assertTrue(
            "History route must recompute when tax vault filter changes",
            navHostSource.contains("taxVaultFilterState") &&
                navHostSource.contains("taxVaultVisibilityMode")
        )
    }

    @Test
    fun homeBentoGridNavigatesThroughDestinationRoutes() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val bentoSource = readSource("ui/components/bento/BentoGridCell.kt")
        listOf(
            "HubBentoDestination.gridRows",
            "onBentoSelected: (HubBentoDestination) -> Unit",
            "onClick = { onBentoSelected(destination) }",
            "BentoGridCell"
        ).forEach { snippet ->
            assertTrue("HomeScreen missing bento navigation: $snippet", homeSource.contains(snippet))
        }
        listOf(
            "HubBentoDestination.CptTracker ->",
            "CptTrackerBentoCell",
            "onClick = onClick"
        ).forEach { snippet ->
            assertTrue("BentoGridCell missing CPT tracker click wiring: $snippet", bentoSource.contains(snippet))
        }
        listOf(
            "HubBentoDestination.AppealGenerator ->",
            "AppealGeneratorBentoCell",
            "appealGeneratorBentoProcessing",
            "activateAppealGeneratorBento"
        ).forEach { snippet ->
            assertTrue("Appeal bento wiring missing: $snippet", bentoSource.contains(snippet) || navHostSource.contains(snippet))
        }
        listOf(
            "onBentoSelected = { destination ->",
            "navController.navigate(destination.route)"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing bento navigate wiring: $snippet", navHostSource.contains(snippet))
        }
        assertTrue(
            "History bento filter must navigate to History route",
            navHostSource.contains("navController.navigate(EobRoute.History.route)") &&
                navHostSource.contains("setHistoryBentoFilter")
        )
        assertTrue(
            "CPT feature screen must read selected category from EobViewModel uiState",
            navHostSource.contains("uiState.selectedCptCategory") &&
                navHostSource.contains("setSelectedCptCategory")
        )
    }

    @Test
    fun uploadAndCameraFlowsWireThroughViewModel() {
        listOf(
            "uploadEobFile",
            "prepareAndUpload",
            "libraryUploadLauncher",
            "EobRoute.CameraCapture.route",
            "cameraPermissionLauncher",
            "setLoadingInvoice",
            "acknowledgeInvoiceFileDropAnimation",
            "canUploadOnCurrentNetwork",
            "imageCompressionLevel"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing upload/camera wiring: $snippet", navHostSource.contains(snippet))
        }
    }

    @Test
    fun subscriptionBillingArchitectureWiresThroughViewModel() {
        val billingSource = readSource("billing/BillingRepository.kt")
        val subscriptionStateSource = readSource("billing/SubscriptionState.kt")
        val subscriptionVmSource = readSource("viewmodel/SubscriptionViewModel.kt")
        listOf(
            "LEGACY_PREMIUM_PRODUCT_ID",
            "SubscriptionCatalog.ALL_PRODUCT_IDS",
            "launchBillingFlow",
            "queryProductDetailsAsync",
            "BillingRepository",
            "WeakReference"
        ).forEach { snippet ->
            assertTrue("Billing layer missing: $snippet", billingSource.contains(snippet))
        }
        assertTrue(
            subscriptionStateSource.contains("sealed interface SubscriptionState") &&
                subscriptionStateSource.contains("Gold") &&
                subscriptionStateSource.contains("Silver") &&
                subscriptionStateSource.contains("Free") &&
                subscriptionStateSource.contains("Error")
        )
        listOf(
            "SubscriptionViewModel",
            "subscriptionState",
            "collectAsStateWithLifecycle",
            "applySubscriptionState",
            "FirestoreSubscriptionRepository",
            "observeSubscriptionTier",
            "subscriptionTier"
        ).forEach { snippet ->
            assertTrue(
                "Subscription wiring missing: $snippet",
                navHostSource.contains(snippet) ||
                    subscriptionVmSource.contains(snippet) ||
                    readSource("viewmodel/EobViewModel.kt").contains(snippet) ||
                    readSource("data/remote/FirestoreSubscriptionRepository.kt").contains(snippet)
            )
        }
        assertFalse(
            "PlayBillingManager must be replaced by BillingRepository",
            File(appModuleRoot, "billing/PlayBillingManager.kt").exists()
        )
        assertTrue(manifestSource.contains("com.android.vending.BILLING"))
        assertTrue(manifestSource.contains("com.android.vending"))
        assertTrue(
            "RevenueCat must initialize from EobApplication",
            readSource("EobApplication.kt").contains("Purchases.configure")
        )
        assertTrue(
            "Manifest must register EobApplication",
            manifestSource.contains("android:name=\".EobApplication\"")
        )
        assertTrue(
            "SubscriptionViewModel must use Application-only constructor for viewModel()",
            subscriptionVmSource.contains(
                "class SubscriptionViewModel(application: Application) : AndroidViewModel(application)"
            )
        )
        assertFalse(
            "SubscriptionViewModel constructor parameters break AndroidViewModelFactory",
            subscriptionVmSource.contains("class SubscriptionViewModel(\n    application: Application,")
        )
        assertTrue(
            "SubscriptionViewModel must initialize BillingRepository inside the ViewModel body",
            subscriptionVmSource.contains("BillingRepository(application.applicationContext)")
        )
        assertTrue(
            "AppViewModel and SubscriptionViewModel must share the same viewModel() constructor contract",
            readSource("viewmodel/AppViewModel.kt").contains("class AppViewModel(application: Application) : AndroidViewModel(application)")
        )
        assertFalse(
            "BillingRepository must remain independent from RevenueCat during Play Billing migration",
            billingSource.contains("com.revenuecat")
        )
        assertFalse(
            "SubscriptionViewModel must keep Play Billing as purchase source of truth",
            subscriptionVmSource.contains("com.revenuecat")
        )
        assertTrue(
            "Gradle must declare RevenueCat purchases SDK",
            File(appModuleRoot, "../../../../../../build.gradle.kts").readText()
                .contains("com.revenuecat.purchases:purchases")
        )
    }

    @Test
    fun revenueCatInitCoexistsWithHybridFirebaseVeryfiPipeline() {
        val applicationSource = readSource("EobApplication.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val remoteSource = readSource("data/remote/FirebaseEobRemoteDataSource.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        listOf(
            "Purchases.configure",
            "PurchasesConfiguration.Builder"
        ).forEach { snippet ->
            assertTrue("EobApplication missing RevenueCat init: $snippet", applicationSource.contains(snippet))
        }
        listOf(
            "processHybridDocument",
            "uploadEobFileAwaitDownload",
            "streamExtractDocument",
            "writeReconciliationFindings",
            "awaitVeryfiExtraction"
        ).forEach { snippet ->
            assertTrue(
                "Hybrid Firebase/Veryfi pipeline missing: $snippet",
                hybridRepoSource.contains(snippet) || veryfiSource.contains(snippet)
            )
        }
        assertTrue(
            "Remote data source must delegate hybrid scans through repository pipeline",
            remoteSource.contains("processHybridScannedDocument")
        )
        assertFalse(
            "EobViewModel must not call RevenueCat directly",
            viewModelSource.contains("com.revenuecat")
        )
        assertTrue(
            "EobViewModel remains scan pipeline source of truth",
            viewModelSource.contains("processScannedDocument") &&
                viewModelSource.contains("documentScanState")
        )
    }

    @Test
    fun hubNewsAndSubscriptionBillingCoexistInNavHost() {
        listOf(
            "personalizedNewsFeed",
            "eobViewModel.personalizedNewsFeed.collectAsStateWithLifecycle",
            "SubscriptionViewModel",
            "subscriptionViewModel.subscriptionState.collectAsStateWithLifecycle",
            "eobViewModel.applySubscriptionState",
            "launchManageSubscriptionFlow",
            "PaywallDialog",
            "launchTierPurchaseFlow",
            "handleBillingNoticeForPaywall"
        ).forEach { snippet ->
            assertTrue("MainHubNavHost must wire news and billing together: $snippet", navHostSource.contains(snippet))
        }
        val remoteSource = readSource("data/remote/FirebaseEobRemoteDataSource.kt")
        assertTrue(remoteSource.contains("override fun observeRegionalNews"))
        assertTrue(remoteSource.contains("override fun observeInsuranceNews"))
    }

    @Test
    fun settingsGearAndHubSettingsWireThroughViewModel() {
        listOf(
            "EobRoute.Settings.route",
            "HubSettingsGearIcon",
            "SettingsScreen",
            "hubSettings",
            "setPinLockEnabled",
            "saveAppPin",
            "verifyAppPinAndUnlock",
            "setUploadOverWifiOnly",
            "setImageCompressionLevel",
            "setAutoCropEnabled",
            "setDarkModeEnabled",
            "clearLocalCache",
            "deleteAccount",
            "settingsUploadWifiBlocked",
            "launchManageSubscriptionFlow",
            "updateBillingNotice",
            "HubCrashlyticsGate"
        ).forEach { snippet ->
            assertTrue("Settings flow missing: $snippet", navHostSource.contains(snippet) || readSource("viewmodel/EobViewModel.kt").contains(snippet) || readSource("util/HubCrashlyticsGate.kt").contains(snippet))
        }
        listOf(
            "onHubDarkModeChanged",
            "eobViewModel.setDarkModeEnabled",
            "eobLightAppBackgroundGradient"
        ).forEach { snippet ->
            assertTrue(
                "Dark mode hub wiring missing: $snippet",
                navHostSource.contains(snippet) ||
                    mainActivitySource.contains(snippet) ||
                    readSource("viewmodel/EobViewModel.kt").contains(snippet)
            )
        }
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("fun canUploadOnCurrentNetwork()"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("fetchLiveInsuranceNews"))
        assertTrue(readSource("network/NewsApiService.kt").contains("RetrofitClient"))
        assertTrue(readSource("network/RssNewsMapper.kt").contains("BECKERS_RSS_URL"))
        assertTrue(readSource("network/RssNetworkModels.kt").contains("data class RssResponse"))
        assertTrue(readSource("network/InsuranceNewsRotation.kt").contains("BECKERS_VISIBLE_COUNT"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("hasLiveInsuranceNewsPools"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("startInsuranceNewsRotationClock"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("processScannedDocument"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("documentScanState"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("processHybridScannedDocument"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("runDocumentOcrPreCheck(context, uri)"))
        assertTrue(readSource("data/DocumentScanPipelineRepository.kt").contains("runCatching { streamDeferred.await() }"))
        assertTrue(readSource("scanner/GmsDocumentScannerLauncher.kt").contains("SCANNER_MODE_FULL"))
        assertTrue(readSource("network/VeryfiDocumentClient.kt").contains("awaitVeryfiExtraction"))
        assertTrue(readSource("network/VeryfiDocumentClient.kt").contains("streamExtractDocument"))
        assertTrue(readSource("network/VeryfiDocumentClient.kt").contains("writeReconciliationFindings"))
        assertTrue(readSource("data/DocumentScanPipelineRepository.kt").contains("processHybridDocument"))
        assertTrue(readSource("util/EobDocumentOcrPreCheck.kt").contains("validate"))
        assertTrue(readSource("ui/components/DocumentProcessingOverlay.kt").contains("DocumentProcessingOverlay"))
        assertTrue(readSource("ui/screens/SettingsScreen.kt").contains("settingsPinLock"))
        assertTrue(readSource("ui/screens/SettingsScreen.kt").contains("HubHelpfulHintsIcon"))
        assertTrue(readSource("ui/screens/SettingsScreen.kt").contains("settingsHelpfulHintsTitle"))
        assertTrue(readSource("data/HubSettingsStore.kt").contains("saveAppPin"))
        assertTrue(readSource("ui/components/home/TaxVaultVerticalFilterCard.kt").contains("VaultNeonText"))
        assertTrue(readSource("ui/components/home/TaxVaultVerticalFilterCard.kt").contains("EobInsuranceGradientStart"))
        assertTrue(readSource("ui/components/home/TaxVaultVerticalFilterCard.kt").contains("TaxVaultShimmerOverlay"))
        assertTrue(readSource("ui/components/home/TaxVaultVerticalFilterCard.kt").contains("10_000"))
    }

    @Test
    fun historyFlowWiresFilterPaginationAndDelete() {
        listOf(
            "setHistoryBentoFilter",
            "historyBentoFilter",
            "historyRecordsForDisplay",
            "setHistoryPage",
            "deleteRecordRemote",
            "HistoryRoute"
        ).forEach { snippet ->
            assertTrue("History flow missing: $snippet", navHostSource.contains(snippet))
        }
        assertEquals(100, HistoryPagination.MAX_EOBS)
    }

    @Test
    fun appealNewsProfileAndDashboardFlowsWireViewModel() {
        listOf(
            "regenerateAppeal",
            "updateAppeal",
            "enableAppealLetterEditing",
            "saveAppealLetter",
            "appealLetterEditingEnabled",
            "currentNewsReleases",
            "deleteNews",
            "DashboardScreen"
        ).forEach { snippet ->
            assertTrue("Feature flow missing: $snippet", navHostSource.contains(snippet))
        }
        assertTrue(navHostSource.contains("saveProfileAndCredentials"))
    }

    @Test
    fun careTeamAndCalendarFlowsWireHomeScreen() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        listOf(
            "HomeCareTeamCards",
            "careTeamCards",
            "preferredDoctors",
            "HomeWeekCalendar",
            "onAddAppointment",
            "onUpdateAppointment",
            "BentoGridCell"
        ).forEach { snippet ->
            assertTrue("Home flow missing: $snippet", homeSource.contains(snippet))
        }
        assertTrue(
            "Care team cards must come from EobViewModel",
            navHostSource.contains("eobViewModel.careTeamCardStates")
        )
    }

    @Test
    fun hubBottomBarScanOpensCameraPermissionPath() {
        assertEquals(HubBottomTab.ScanEob, HubBottomTab.entries[1])
        assertTrue(navHostSource.contains("launchDocumentScanner"))
        assertTrue(navHostSource.contains("StartIntentSenderForResult"))
        assertTrue(navHostSource.contains("DocumentProcessingOverlay"))
        assertTrue(EobRoute.CameraCapture.route in hubRoutesWithoutBottomBar)
        assertTrue(EobRoute.Home.route !in hubBackRoutes)
        assertTrue(hubFeatureRoutes.contains(EobRoute.History.route))
    }

    @Test
    fun pasteAndDeleteFlowsExistOnViewModel() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        listOf("savePastedEob", "deleteRecord", "deleteRecordRemote", "uploadEobFile", "uploadEobBitmap", "processScannedDocument")
            .forEach { snippet ->
                assertTrue("EobViewModel missing $snippet", viewModelSource.contains(snippet))
            }
    }

    @Test
    fun firestoreSyncAndLogoutResetHubState() {
        listOf(
            "startFirestoreSync",
            "resetHubState",
            "attachRepository"
        ).forEach { snippet ->
            assertTrue("Sync flow missing: $snippet", navHostSource.contains(snippet))
        }
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("fetchHistoryFromFirestore"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("fetchLiveInsuranceNews()"))
    }

    @Test
    fun viewModelIsDocumentedAsHubSourceOfTruth() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(viewModelSource.contains("Single source of truth"))
        assertFalse(
            "EobViewModel must not use Compose mutableStateOf",
            viewModelSource.contains("mutableStateOf")
        )
        listOf(
            "HubUiState",
            "sortedEobRecords",
            "insuranceBriefings",
            "insuranceNewsRotationSlot",
            "invoiceProcessingPhase",
            "historyBentoFilter",
            "preferredDoctors",
            "applyRemoteRecords",
            "selectedCptCategory",
            "ytdBentoViewMode",
            "firebaseSyncStatus",
            "careTeamCardStates",
            "insuranceCardDisplay",
            "applyInsuranceCardEdits",
            "cptBentoSnapshot",
            "ytdDeductibleBentoSnapshot",
            "insuranceNewsBentoSnapshot",
            "fetchLiveInsuranceNews",
            "newsFeedRevision",
            "historyBentoSnapshot",
            "providerAvatarPreviews",
            "providerDirectory",
            "yearlyHealthCostSummary",
            "historyRecordsForDisplay",
            "currentNewsReleases",
            "personalizedNewsFeed",
            "observeRegionalNews",
            "setSelectedCptCategory",
            "setYtdBentoViewMode",
            "updateUploadText"
        ).forEach { snippet ->
            assertTrue("EobViewModel missing: $snippet", viewModelSource.contains(snippet))
        }
    }

    @Test
    fun hubHeaderUsesLocalizedAppBrand() {
        val navHostSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navHostSource.contains("EobStrings.t(language, \"appBrand\")"))
    }

    @Test
    fun hubNavRoutesAnalyticsThroughEobViewModel() {
        listOf(
            "eobViewModel.historyBentoSnapshot",
            "eobViewModel.providerAvatarPreviews",
            "eobViewModel.providerDirectory",
            "eobViewModel.yearlyHealthCostSummary",
            "eobViewModel.historyRecordsForDisplay",
            "eobViewModel.totalBillingErrors",
            "eobViewModel.insuranceBriefings",
            "eobViewModel.currentNewsReleases",
            "insuranceNewsRotationSlot",
            "uiState.firebaseSyncStatus",
            "eobViewModel.updateSyncProfile",
            "eobViewModel.hubTimeKey"
        ).forEach { snippet ->
            assertTrue("EobNavHost should route through ViewModel: $snippet", navHostSource.contains(snippet))
        }
        assertFalse(
            "EobNavHost should not call EobAnalyzer directly",
            navHostSource.contains("EobAnalyzer.")
        )
    }

    @Test
    fun insuranceNewsBentoCellWiredThroughHomeAndNavHost() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val bentoSource = readSource("ui/components/bento/BentoGridCell.kt")
        val newsCellSource = readSource("ui/components/bento/InsuranceNewsBentoCell.kt")
        assertTrue(
            "Missing InsuranceNewsBentoCell.kt",
            File(appModuleRoot, "ui/components/bento/InsuranceNewsBentoCell.kt").isFile
        )
        listOf("insuranceNewsBentoSnapshot").forEach { snippet ->
            assertTrue("HomeScreen missing: $snippet", homeSource.contains(snippet))
            assertTrue("EobNavHost missing: $snippet", navHostSource.contains(snippet))
        }
        listOf(
            "InsuranceNewsBentoCell",
            "HubBentoDestination.InsuranceNews"
        ).forEach { snippet ->
            assertTrue("BentoGridCell missing: $snippet", bentoSource.contains(snippet))
        }
        listOf(
            "eobViewModel.insuranceNewsBentoSnapshot",
            "uiState.newsFeedRevision",
            "personalizedNewsFeed"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing: $snippet", navHostSource.contains(snippet))
        }
        listOf(
            "Spring.StiffnessLow",
            "graphicsLayer",
            "infiniteRepeatable"
        ).forEach { snippet ->
            assertTrue("InsuranceNewsBentoCell missing: $snippet", newsCellSource.contains(snippet))
        }
    }

    @Test
    fun cptAndYtdBentoCellsWiredThroughHomeAndNavHost() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val bentoSource = readSource("ui/components/bento/BentoGridCell.kt")
        mapOf(
            "ui/components/bento/CptTrackerBentoCell.kt" to "CptTrackerBentoCell.kt",
            "ui/components/bento/YtdExpenseBentoCell.kt" to "YtdExpenseBentoCell.kt",
            "data/BentoSnapshotExtractor.kt" to "BentoSnapshotExtractor.kt",
            "data/FairHealthPricingIndex.kt" to "FairHealthPricingIndex.kt"
        ).forEach { (relativePath, label) ->
            assertTrue("Missing $label", File(appModuleRoot, relativePath).isFile)
        }
        listOf(
            "cptBentoSnapshot",
            "ytdBentoSnapshot",
            "ytdBentoViewMode",
            "onYtdViewModeSelected"
        ).forEach { snippet ->
            assertTrue("HomeScreen missing bento param: $snippet", homeSource.contains(snippet))
        }
        listOf(
            "HubBentoDestination.CptTracker",
            "HubBentoDestination.YtdExpense",
            "CptTrackerBentoCell",
            "YtdExpenseBentoCell"
        ).forEach { snippet ->
            assertTrue("BentoGridCell missing: $snippet", bentoSource.contains(snippet))
        }
        listOf(
            "eobViewModel.cptBentoSnapshot",
            "eobViewModel.ytdDeductibleBentoSnapshot",
            "setYtdBentoViewMode",
            "uiState.selectedCptCategory",
            "setSelectedCptCategory"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing CPT/YTD wiring: $snippet", navHostSource.contains(snippet))
        }
    }

    @Test
    fun homeScreenWiresEditableCleanInsuranceCardThroughEobViewModel() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val profileSource = readSource("ui/screens/ProfileScreen.kt")
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        listOf(
            "CleanInsuranceCard",
            "insuranceCardDisplay",
            "onSaveInsuranceCard",
            "isEditingInsuranceCard",
            "draftInsuranceName",
            "draftMemberId",
            "draftGroupNumber",
            "draftPcpCopay",
            "draftSpecialistCopay"
        ).forEach { snippet ->
            assertTrue("HomeScreen missing insurance card wiring: $snippet", homeSource.contains(snippet))
        }
        listOf(
            "eobViewModel.insuranceCardDisplay",
            "eobViewModel.applyInsuranceCardEdits",
            "eobViewModel.updateSyncProfile",
            "eobViewModel.saveProfileToRemote",
            "onProfileChanged(updatedProfile)"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing insurance card save wiring: $snippet", navHostSource.contains(snippet))
        }
        listOf(
            "fun insuranceCardDisplay",
            "fun applyInsuranceCardEdits",
            "InsuranceCardDisplay"
        ).forEach { snippet ->
            assertTrue("EobViewModel missing insurance card API: $snippet", viewModelSource.contains(snippet))
        }
        assertFalse(
            "ProfileScreen must not render CleanInsuranceCard",
            profileSource.contains("CleanInsuranceCard")
        )
        assertTrue(cardSource.contains("ElevatedCard"))
        assertTrue(cardSource.contains("InsuranceCardDisplay"))
        assertTrue(cardSource.contains("BasicTextField"))
        assertFalse("CleanInsuranceCard must stay stateless", cardSource.contains("mutableStateOf"))
    }

    @Test
    fun careTeamShimmerDelaysFourSecondsOnUnassignedCards() {
        val careTeamSource = readSource("ui/components/home/HomeCareTeamCards.kt")
        assertTrue(careTeamSource.contains("delay(4_000)"))
        assertTrue(careTeamSource.contains("!cardState.isAssigned && showShimmer"))
    }

    @Test
    fun allBentoCellsShareUniformAspectRatio() {
        val layoutSource = readSource("ui/components/bento/BentoCellLayout.kt")
        val bentoCells = listOf(
            "ui/components/bento/HistoryBentoCell.kt",
            "ui/components/bento/ProviderDirectoryBentoCell.kt",
            "ui/components/bento/CptTrackerBentoCell.kt",
            "ui/components/bento/YtdExpenseBentoCell.kt",
            "ui/components/bento/InsuranceNewsBentoCell.kt",
            "ui/components/bento/AppealGeneratorBentoCell.kt"
        )
        assertTrue(layoutSource.contains("ASPECT_RATIO"))
        bentoCells.forEach { relativePath ->
            val source = readSource(relativePath)
            assertTrue(
                "$relativePath must use BentoCellLayout.ASPECT_RATIO",
                source.contains("BentoCellLayout.ASPECT_RATIO")
            )
        }
    }

    @Test
    fun firebaseProfileMapperPersistsCopayFields() {
        val mapperSource = readSource("data/FirebaseEobMapper.kt")
        listOf(
            "\"pcpCopay\" to profile.pcpCopay",
            "\"specialistCopay\" to profile.specialistCopay",
            "pcpCopay = data.stringValue",
            "specialistCopay = data.stringValue"
        ).forEach { snippet ->
            assertTrue("FirebaseEobMapper missing copay field: $snippet", mapperSource.contains(snippet))
        }
    }

    private fun readSource(relativePath: String): String {
        val file = File(appModuleRoot, relativePath)
        require(file.isFile) { "Missing ${file.absolutePath}" }
        return file.readText()
    }

    companion object {
        private fun resolveAppModuleRoot(): File {
            val candidates = listOf(
                File("src/main/java/app/eob/me"),
                File("app/src/main/java/app/eob/me")
            )
            return candidates.firstOrNull { it.isDirectory }
                ?: error("Could not locate app sources; tried: ${candidates.map { it.path }}")
        }

        private fun resolveManifestFile(): File {
            val candidates = listOf(
                File("src/main/AndroidManifest.xml"),
                File("app/src/main/AndroidManifest.xml")
            )
            return candidates.firstOrNull { it.isFile }
                ?: error("Could not locate AndroidManifest.xml")
        }
    }
}
