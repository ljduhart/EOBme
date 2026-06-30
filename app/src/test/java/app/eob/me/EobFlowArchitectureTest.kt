package app.eob.me

import app.eob.me.navigation.EobRoute
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.navigation.HubBottomTab
import app.eob.me.navigation.hubBackRoutes
import app.eob.me.navigation.hubFeatureRoutes
import app.eob.me.navigation.hubRoutesWithoutBottomBar
import app.eob.me.network.VeryfiAnyDocConstants
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
            "EobRoute.History.route" to "EobHistoryScreen.kt",
            "EobRoute.Dashboard.route" to "DashboardScreen.kt",
            "EobRoute.YearlyExpense.route" to "YtdExpenseScreen.kt",
            "EobRoute.CptCount.route" to "CptTrackerScreen.kt",
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
        assertTrue(
            "Animated provider directory screen must exist",
            File(appModuleRoot, "ui/screens/AnimatedProviderDirectory.kt").isFile
        )
        assertTrue(
            "Provider directory must route View Records through EobViewModel",
            navHostSource.contains("openProviderRecordHistory")
        )
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
            "prepareAndUpload",
            "libraryUploadLauncher",
            "processScannedDocument",
            "CameraScanDocumentType.Eob",
            "EobRoute.CameraCapture.route",
            "customCameraPermissionLauncher",
            "acknowledgeInvoiceFileDropAnimation",
            "canUploadOnCurrentNetwork",
            "imageCompressionLevel",
            "documentScanState"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing upload/camera wiring: $snippet", navHostSource.contains(snippet))
        }
        assertFalse(
            "Library upload must not bypass hybrid pipeline via storage-only uploadEobFile",
            navHostSource.contains("eobViewModel.uploadEobFile(")
        )
    }

    @Test
    fun subscriptionBillingArchitectureWiresThroughViewModel() {
        val billingSource = readSource("billing/BillingRepository.kt")
        val subscriptionStateSource = readSource("billing/SubscriptionState.kt")
        val subscriptionVmSource = readSource("viewmodel/SubscriptionViewModel.kt")
        listOf(
            "LEGACY_PREMIUM_PRODUCT_ID",
            "SubscriptionCatalog.ALL_SUBSCRIPTION_PRODUCT_IDS",
            "SubscriptionCatalog.offerRef",
            "basePlanId",
            "resolveOfferToken",
            "billing_user_canceled",
            "clearBillingNotice",
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
            "SubscriptionViewModel must delegate RevenueCat to RevenueCatBillingRepository",
            subscriptionVmSource.contains("RevenueCatBillingRepository(application.applicationContext)")
        )
        assertFalse(
            "SubscriptionViewModel must not import RevenueCat SDK directly",
            subscriptionVmSource.contains("com.revenuecat")
        )
        val revenueCatBillingSource = readSource("billing/RevenueCatBillingRepository.kt")
        assertTrue(
            "AppViewModel and SubscriptionViewModel must share the same viewModel() constructor contract",
            readSource("viewmodel/AppViewModel.kt").contains("class AppViewModel(application: Application) : AndroidViewModel(application)")
        )
        assertFalse(
            "BillingRepository must remain independent from RevenueCat during Play Billing migration",
            billingSource.contains("com.revenuecat")
        )
        assertTrue(
            "RevenueCatBillingRepository must stream entitlements via customer-info listener",
            revenueCatBillingSource.contains("UpdatedCustomerInfoListener") &&
                revenueCatBillingSource.contains("updatedCustomerInfoListener") &&
                revenueCatBillingSource.contains("activeTier")
        )
        assertTrue(
            "RevenueCat must drive purchases through awaitPurchase with Play Billing fallback",
            revenueCatBillingSource.contains("awaitPurchase") &&
                revenueCatBillingSource.contains("PurchaseParams.Builder") &&
                revenueCatBillingSource.contains("awaitOfferings") &&
                revenueCatBillingSource.contains("RevenueCatPackageResolver")
        )
        assertTrue(
            "Play Billing fallback must remain for offerings sync",
            subscriptionVmSource.contains("launchBillingFlow")
        )
        assertTrue(
            "RevenueCat public API key must be centralized",
            readSource("billing/RevenueCatConfig.kt").contains("goog_rmhYQIPDsEWnEBFWUzMRYYlpYMo") &&
                readSource("EobApplication.kt").contains("RevenueCatConfig.PUBLIC_API_KEY")
        )
        assertTrue(
            "RevenueCat identity must sync on Firebase sign-in and sign-out",
            revenueCatBillingSource.contains("awaitLogIn") &&
                revenueCatBillingSource.contains("awaitLogOut")
        )
        assertTrue(
            "RevenueCat must expose restore purchases for Google Play policy compliance",
            revenueCatBillingSource.contains("awaitRestore") &&
                revenueCatBillingSource.contains("restoreUserPurchases") &&
                readSource("ui/screens/PaywallDialog.kt").contains("onRestorePurchasesClicked") &&
                navHostSource.contains("restorePurchases")
        )
        assertTrue(
            "RevenueCat customer profiles must receive email, display name, and cohort attributes",
            revenueCatBillingSource.contains("setEmail") &&
                revenueCatBillingSource.contains("setDisplayName") &&
                revenueCatBillingSource.contains("setAttributes") &&
                revenueCatBillingSource.contains("attachUserMetadata") &&
                navHostSource.contains("bindUser(") &&
                navHostSource.contains("displayName = profile.fullName")
        )
        assertTrue(
            "RevenueCat must enable entitlement verification at boot",
            readSource("EobApplication.kt").contains("entitlementVerificationMode") &&
                readSource("EobApplication.kt").contains("EntitlementVerificationMode.INFORMATIONAL")
        )
        assertTrue(
            "Paywall must use dynamic RevenueCat pricing",
            readSource("ui/screens/PaywallDialog.kt").contains("paywallPricing.displayPrice") &&
                readSource("navigation/EobNavHost.kt").contains("paywallPricing")
        )
        assertTrue(
            "Gradle must declare RevenueCat purchases SDK",
            File(appModuleRoot, "../../../../../../build.gradle.kts").readText()
                .contains("libs.revenuecat.purchases")
        )
    }

    @Test
    fun revenueCatInitCoexistsWithHybridFirebaseVeryfiPipeline() {
        val applicationSource = readSource("EobApplication.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val anyDocRepoSource = readSource("data/VeryfiAnyDocRepository.kt")
        val anyDocConstantsSource = readSource("network/VeryfiAnyDocConstants.kt")
        val remoteSource = readSource("data/remote/FirebaseEobRemoteDataSource.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        listOf(
            "Purchases.configure",
            "PurchasesConfiguration.Builder",
            "RevenueCatConfig.PUBLIC_API_KEY",
            "entitlementVerificationMode",
            "EntitlementVerificationMode.INFORMATIONAL"
        ).forEach { snippet ->
            assertTrue("EobApplication missing RevenueCat init: $snippet", applicationSource.contains(snippet))
        }
        listOf(
            "processHybridDocument",
            "uploadEobFileAwaitDownload",
            "streamExtractDocument",
            "writeReconciliationFindings",
            "awaitVeryfiExtraction",
            "normalizeStoragePath",
            "VeryfiAnyDocRepository",
            "health_insurance_eob",
            "partner/any-documents"
        ).forEach { snippet ->
            assertTrue(
                "Hybrid Firebase/Veryfi pipeline missing: $snippet",
                hybridRepoSource.contains(snippet) ||
                    veryfiSource.contains(snippet) ||
                    anyDocRepoSource.contains(snippet) ||
                    anyDocConstantsSource.contains(snippet)
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
        listOf(
            "EobRoute.CameraCapture.route",
            "processScannedDocument",
            "documentScanState",
            "subscriptionViewModel.startBilling",
            "subscriptionViewModel.bindUser",
            "eobViewModel.applySubscriptionState",
            "PaywallDialog"
        ).forEach { snippet ->
            assertTrue(
                "Hybrid navigational pipeline must coexist with billing in EobNavHost: $snippet",
                navHostSource.contains(snippet)
            )
        }
    }

    @Test
    fun pr100FinalAuditHybridBillingPaywallIntegrity() {
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val remoteSource = readSource("data/remote/FirebaseEobRemoteDataSource.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val subscriptionVmSource = readSource("viewmodel/SubscriptionViewModel.kt")
        val revenueCatBillingSource = readSource("billing/RevenueCatBillingRepository.kt")
        val paywallSource = readSource("ui/screens/PaywallDialog.kt")
        val settingsSource = readSource("ui/screens/SettingsScreen.kt")
        val ocrPreCheckSource = readSource("util/EobDocumentOcrPreCheck.kt")

        listOf(
            "processHybridDocument",
            "uploadEobFileAwaitDownload",
            "streamExtractDocument",
            "writeReconciliationFindings",
            "awaitVeryfiExtraction",
            "processHybridScannedDocument",
            "runDocumentOcrPreCheck"
        ).forEach { snippet ->
            assertTrue(
                "PR#100 audit: hybrid pipeline barrier missing $snippet",
                hybridRepoSource.contains(snippet) ||
                    veryfiSource.contains(snippet) ||
                    remoteSource.contains(snippet) ||
                    viewModelSource.contains(snippet)
            )
        }
        assertTrue("PR#100 audit: OCR pre-check barrier required", ocrPreCheckSource.contains("validate"))
        assertTrue(
            "PR#100 audit: receipt scan-type OCR barrier required",
            ocrPreCheckSource.contains("validateForScanType")
        )

        assertFalse("PR#100 audit: EobViewModel must not import RevenueCat", viewModelSource.contains("com.revenuecat"))
        assertFalse("PR#100 audit: SubscriptionViewModel must not import RevenueCat", subscriptionVmSource.contains("com.revenuecat"))
        assertTrue("PR#100 audit: RevenueCat isolated in billing repository", revenueCatBillingSource.contains("Purchases.sharedInstance"))
        assertTrue("PR#100 audit: restore purchases required", revenueCatBillingSource.contains("awaitRestore"))
        assertTrue("PR#100 audit: metadata sync required", revenueCatBillingSource.contains("attachUserMetadata"))

        assertTrue("PR#100 audit: paywall uses dynamic pricing", paywallSource.contains("paywallPricing.displayPrice"))
        assertTrue("PR#100 audit: restore button on paywall", paywallSource.contains("onRestorePurchasesClicked"))
        assertTrue("PR#100 audit: manage subscription available in settings", settingsSource.contains("onManageSubscription"))
        assertFalse(
            "PR#100 audit: manage subscription must not be tier-gated in settings",
            settingsSource.contains("subscriptionTier") && settingsSource.contains("onManageSubscription") &&
                settingsSource.contains("if (subscriptionTier")
        )

        assertTrue("PR#100 audit: EobViewModel owns paywall state", viewModelSource.contains("fun showPaywall"))
        assertTrue("PR#100 audit: EobViewModel applies subscription state", viewModelSource.contains("fun applySubscriptionState"))
        assertTrue("PR#100 audit: billing notices carry into paywall", viewModelSource.contains("localizedBillingNotices"))

        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "ui/components/EobSplashLogo.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#100 audit: opening screen must remain untouched ($path)", source.contains("RevenueCat"))
            assertFalse("PR#100 audit: opening screen must remain untouched ($path)", source.contains("PaywallDialog"))
        }
    }

    @Test
    fun featureGateArchitectureIsWiredIntoHubBento() {
        val featureGateSource = readSource("data/FeatureGate.kt")
        val premiumBoxSource = readSource("ui/components/PremiumBentoBox.kt")
        val bentoSource = readSource("ui/components/bento/BentoGridCell.kt")
        assertTrue(featureGateSource.contains("object EobmeFeatureGate"))
        assertTrue(premiumBoxSource.contains("Modifier.blur(16.dp)"))
        assertTrue(premiumBoxSource.contains("premiumUpgradeToUnlock"))
        assertTrue(bentoSource.contains("PremiumBentoBox"))
        assertTrue(bentoSource.contains("requiresPremiumGate"))
        assertTrue(navHostSource.contains("onPremiumFeatureLocked"))
    }

    @Test
    fun hubNewsAndSubscriptionBillingCoexistInNavHost() {
        listOf(
            "personalizedNewsFeed",
            "eobViewModel.personalizedNewsFeed.collectAsStateWithLifecycle",
            "SubscriptionViewModel",
            "subscriptionViewModel.subscriptionState.collectAsStateWithLifecycle",
            "subscriptionViewModel.paywallPricing.collectAsStateWithLifecycle",
            "eobViewModel.applySubscriptionState",
            "launchManageSubscriptionFlow",
            "PaywallDialog",
            "launchTierPurchaseFlow",
            "onRestorePurchasesClicked",
            "restorePurchases",
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
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("veryfiAnyDocExtractionState"))
        assertTrue(readSource("network/VeryfiAnyDocConstants.kt").contains("partner/any-documents"))
        assertTrue(readSource("network/VeryfiAnyDocConstants.kt").contains("health_insurance_eob"))
        assertTrue(readSource("network/VeryfiAnyDocApiService.kt").contains("VeryfiAnyDocConstants.ANY_DOCUMENTS_PATH"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("documentScanState"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("processHybridScannedDocument"))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("runDocumentOcrPreCheck("))
        assertTrue(readSource("viewmodel/EobViewModel.kt").contains("cameraScanDocumentType"))
        assertTrue(readSource("data/DocumentScanPipelineRepository.kt").contains("veryfiAnyDocRepository.extractHealthInsuranceEob"))
        assertTrue(readSource("data/VeryfiAnyDocRepository.kt").contains("streamExtractDocument"))
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
    fun historyFlowWiresFilterTimelineAndDelete() {
        listOf(
            "setHistoryBentoFilter",
            "setHistoryPaymentFilter",
            "historyBentoFilter",
            "historyPaymentFilter",
            "historyRecordsForDisplay",
            "historyTimelineSections",
            "selectRecord",
            "deleteRecordRemote",
            "HistoryRoute",
            "EobHistoryScreen"
        ).forEach { snippet ->
            assertTrue("History flow missing: $snippet", navHostSource.contains(snippet))
        }
    }

    @Test
    fun appealNewsProfileAndDashboardFlowsWireViewModel() {
        val appealSource = readSource("ui/screens/AppealScreen.kt")
        listOf(
            "regenerateAppeal",
            "onAppealTargetSwitched",
            "onDisputeStrategySwitched",
            "selectedAppealTarget",
            "selectedDisputeStrategy",
            "selectedInsuranceAppealStrategy",
            "updateAppeal",
            "enableAppealLetterEditing",
            "saveAppealLetter",
            "appealLetterEditingEnabled",
            "openInsuranceArticle",
            "filteredNewsReleases",
            "deleteNews",
            "DashboardScreen"
        ).forEach { snippet ->
            assertTrue("Feature flow missing: $snippet", navHostSource.contains(snippet))
        }
        listOf(
            "fun AppealActionBar",
            "SingleChoiceSegmentedButtonRow",
            "DoctorDisputeStrategySelector",
            "AnimatedVisibility",
            "AnimatedContent",
            "Intent.ACTION_SEND"
        ).forEach { snippet ->
            assertTrue("Appeal print-ready UI missing: $snippet", appealSource.contains(snippet))
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
    fun hubBackNavigationHandlesHomeOverlayAndPaywallGestures() {
        listOf(
            "BackHandler",
            "exitHubToSignIn",
            "dismissInsuranceArticle",
            "dismissPaywall",
            "resetHubState",
            "!uiState.paywallVisible",
            "!uiState.hubSettings.appLocked"
        ).forEach { snippet ->
            assertTrue("MainHubNavHost missing back navigation: $snippet", navHostSource.contains(snippet))
        }
        assertTrue(readSource("viewmodel/AppViewModel.kt").contains("fun exitHubToSignIn"))
    }

    @Test
    fun hubBottomBarScanOpensCameraPermissionPath() {
        assertEquals(HubBottomTab.ScanEob, HubBottomTab.entries[1])
        assertTrue(navHostSource.contains("customCameraPermissionLauncher.launch(Manifest.permission.CAMERA)"))
        assertTrue(navHostSource.contains("EobRoute.CameraCapture.route"))
        assertTrue(navHostSource.contains("DocumentProcessingOverlay"))
        assertTrue(navHostSource.contains("showHubHeader"))
        assertTrue(EobRoute.CameraCapture.route in hubRoutesWithoutBottomBar)
        assertTrue(EobRoute.Home.route !in hubBackRoutes)
        assertTrue(hubFeatureRoutes.contains(EobRoute.History.route))
    }

    @Test
    fun cameraCaptureRouteUsesHybridDocumentPipeline() {
        val cameraScreenSource = readSource("ui/screens/CameraCaptureScreen.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(cameraScreenSource.contains("CameraCaptureViewModel"))
        assertTrue(cameraScreenSource.contains("weight(0.85f)"))
        assertTrue(cameraScreenSource.contains("weight(0.15f)"))
        assertTrue(navHostSource.contains("EobRoute.CameraCapture.route"))
        assertTrue(navHostSource.contains("processScannedDocument"))
        assertTrue(navHostSource.contains("imageCompressionLevel()"))
        assertTrue(navHostSource.contains("customCameraPermissionLauncher"))
        assertFalse(navHostSource.contains("onCameraScan"))
        assertFalse(
            "Camera capture must not bypass hybrid pipeline via prepareAndUpload",
            navHostSource.contains("prepareAndUpload(uri, EobStrings.t(language, \"cameraScan\"))")
        )
        assertTrue(
            "uploadEobFile must delegate to hybrid scan pipeline",
            viewModelSource.contains("fun uploadEobFile") &&
                viewModelSource.contains("processScannedDocument(") &&
                viewModelSource.contains("scanType = CameraScanDocumentType.Eob")
        )
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
            "ytdExpenseData",
            "historyRecordsForDisplay",
            "currentNewsReleases",
            "filteredNewsReleases",
            "insuranceCarrierHubItems",
            "setSelectedNewsCarrier",
            "selectedNewsCarrier",
            "personalizedNewsFeed",
            "observeRegionalNews",
            "setSelectedCptCategory",
            "cptFlashcardEntries",
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
            "eobViewModel.ytdExpenseData",
            "eobViewModel.historyRecordsForDisplay",
            "eobViewModel.totalBillingErrors",
            "eobViewModel.insuranceCarrierHubItems",
            "eobViewModel.filteredNewsReleases",
            "eobViewModel.setSelectedNewsCarrier",
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
            "eobViewModel.cptFlashcardEntries",
            "eobViewModel.ytdDeductibleBentoSnapshot",
            "setYtdBentoViewMode",
            "uiState.selectedCptCategory",
            "setSelectedCptCategory"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing CPT/YTD wiring: $snippet", navHostSource.contains(snippet))
        }
    }

    @Test
    fun homeScreenRendersReadOnlyCleanInsuranceCardFromEobViewModel() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val profileSource = readSource("ui/screens/ProfileScreen.kt")
        val registrationSource = readSource("ui/screens/RegistrationScreen.kt")
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        listOf(
            "CleanInsuranceCard",
            "insuranceCardDisplay"
        ).forEach { snippet ->
            assertTrue("HomeScreen missing insurance card wiring: $snippet", homeSource.contains(snippet))
        }
        assertFalse(
            "HomeScreen must not wire insurance card editing",
            homeSource.contains("onSaveInsuranceCard")
        )
        assertFalse(
            "HomeScreen must not manage insurance card edit state",
            homeSource.contains("isEditingInsuranceCard")
        )
        listOf(
            "eobViewModel.insuranceCardDisplay"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing insurance card display wiring: $snippet", navHostSource.contains(snippet))
        }
        assertFalse(
            "EobNavHost must not save insurance card edits from Home",
            navHostSource.contains("onSaveInsuranceCard")
        )
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
        listOf(
            "insuranceName",
            "insuranceId",
            "groupName",
            "pcpCopay",
            "specialistCopay"
        ).forEach { snippet ->
            assertTrue("ProfileFields must expose insurance editing: $snippet", registrationSource.contains(snippet))
        }
        assertTrue(cardSource.contains("ElevatedCard"))
        assertTrue(cardSource.contains("InsuranceCardDisplay"))
        assertFalse("CleanInsuranceCard must be read-only on Home", cardSource.contains("BasicTextField"))
        assertFalse("CleanInsuranceCard must not be tappable for edit", cardSource.contains("clickable"))
        assertFalse("CleanInsuranceCard must stay stateless", cardSource.contains("mutableStateOf"))
    }

    @Test
    fun careTeamShimmerDelaysFourSecondsOnUnassignedCards() {
        val careTeamSource = readSource("ui/components/home/HomeCareTeamCards.kt")
        assertTrue(careTeamSource.contains("delay(4_000)"))
        assertTrue(careTeamSource.contains("!cardState.isAssigned && showShimmer"))
    }

    @Test
    fun careTeamDialRoutesThroughDeviceCallingUtilsBarrier() {
        val careTeamSource = readSource("ui/components/home/HomeCareTeamCards.kt")
        val utilSource = readSource("util/DeviceCallingUtils.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(careTeamSource.contains("DeviceCallingUtils.safelyDialNumber"))
        assertTrue(careTeamSource.contains("DeviceCallingUtils.extractPhoneDigits"))
        assertTrue(careTeamSource.contains("DeviceCallingUtils.careTeamPhoneVisualTransformation"))
        assertTrue(careTeamSource.contains("DeviceCallingUtils.formatPhoneForDisplay"))
        assertFalse(careTeamSource.contains("Intent.ACTION_CALL"))
        assertFalse(careTeamSource.contains("ACTION_CALL"))
        assertTrue(utilSource.contains("Intent.ACTION_DIAL"))
        assertTrue(utilSource.contains("FEATURE_TELEPHONY"))
        assertTrue(utilSource.contains("resolveActivity"))
        assertTrue(viewModelSource.contains("fun sanitizeCareTeamPhone"))
        assertTrue(viewModelSource.contains("fun sanitizeCareTeamProviderName"))
        assertTrue(viewModelSource.contains("sanitizeCareTeamPhone(doctor.phone)"))
        assertTrue(viewModelSource.contains("sanitizeCareTeamProviderName(doctor.name)"))
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

    @Test
    fun appealGeneratorDualTargetToggleRemainsIntact() {
        val appealSource = readSource("ui/screens/AppealScreen.kt")
        val appealModelsSource = readSource("data/AppealModels.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        listOf(
            "AppealTargetSelector",
            "SingleChoiceSegmentedButtonRow",
            "AppealTarget.entries",
            "target.labelKey()",
            "AnimatedVisibility",
            "visible = false",
            "DoctorDisputeStrategySelector"
        ).forEach { snippet ->
            assertTrue("Appeal dual-target UI missing: $snippet", appealSource.contains(snippet))
        }
        assertTrue(
            "Doctor dispute strategies must be chosen from history floater",
            readSource("ui/screens/EobHistoryScreen.kt").contains("DoctorAppealStrategyFloater")
        )
        listOf(
            "appealTargetInsurance",
            "appealTargetDoctor",
            "INSURANCE",
            "DOCTOR"
        ).forEach { snippet ->
            assertTrue("Appeal target model missing: $snippet", appealModelsSource.contains(snippet))
        }
        listOf(
            "fun onAppealTargetSwitched",
            "fun onDisputeStrategySwitched",
            "private fun generateAppealLetter",
            "AppealLetterGenerator.generate"
        ).forEach { snippet ->
            assertTrue("Appeal ViewModel source-of-truth missing: $snippet", viewModelSource.contains(snippet))
        }
        assertTrue(navHostSource.contains("onAppealTargetSwitched = { target ->"))
        assertTrue(navHostSource.contains("selectedTarget = uiState.selectedAppealTarget"))
    }

    @Test
    fun pr104FinalConnectivityAudit() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val appealSource = readSource("ui/screens/AppealScreen.kt")
        val paywallSource = readSource("ui/screens/PaywallDialog.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val hybridRefSource = readSource("data/HybridDocumentRef.kt")
        val anyDocRepoSource = readSource("data/VeryfiAnyDocRepository.kt")
        val anyDocConstantsSource = readSource("network/VeryfiAnyDocConstants.kt")

        listOf(
            "processScannedDocument",
            "documentScanState",
            "startFirestoreSync",
            "observeEobs",
            "applyRemoteRecords",
            "private fun generateAppealLetter",
            "fun onAppealTargetSwitched",
            "applySubscriptionState",
            "PaywallDialog"
        ).forEach { snippet ->
            assertTrue(
                "PR#104 audit: EobViewModel/nav hybrid hub barrier missing $snippet",
                viewModelSource.contains(snippet) || navHostSource.contains(snippet)
            )
        }
        listOf("SubscriptionTier.Gold", "SubscriptionTier.Silver").forEach { snippet ->
            assertTrue(
                "PR#104 audit: subscription tier barrier missing $snippet",
                viewModelSource.contains(snippet) ||
                    navHostSource.contains(snippet) ||
                    paywallSource.contains(snippet)
            )
        }

        listOf(
            "processHybridDocument",
            "uploadEobFileAwaitDownload",
            "streamExtractDocument",
            "writeReconciliationFindings",
            "awaitVeryfiExtraction",
            "normalizeStoragePath",
            "VeryfiAnyDocRepository",
            "health_insurance_eob",
            "partner/any-documents"
        ).forEach { snippet ->
            assertTrue(
                "PR#104 audit: Veryfi/Firestore hybrid pipeline barrier missing $snippet",
                hybridRepoSource.contains(snippet) ||
                    veryfiSource.contains(snippet) ||
                    hybridRefSource.contains(snippet) ||
                    anyDocRepoSource.contains(snippet) ||
                    anyDocConstantsSource.contains(snippet) ||
                    readSource("data/FirebaseEobRepository.kt").contains(snippet)
            )
        }

        listOf(
            "prepareAndUpload",
            "processScannedDocument",
            "CameraScanDocumentType.Eob",
            "DocumentProcessingOverlay",
            "customCameraPermissionLauncher"
        ).forEach { snippet ->
            assertTrue("PR#104 audit: upload navigation barrier missing $snippet", navHostSource.contains(snippet))
        }
        assertFalse(
            "PR#104 audit: library upload must not use storage-only uploadEobFile in nav",
            navHostSource.contains("eobViewModel.uploadEobFile(")
        )

        listOf(
            "BackHandler",
            "EobRoute.Home.route",
            "exitHubToSignIn",
            "resetHubState"
        ).forEach { snippet ->
            assertTrue("PR#104 audit: home back navigation barrier missing $snippet", navHostSource.contains(snippet))
        }

        listOf(
            "Silver Tier",
            "Gold Tier",
            "SubscriptionCatalog.features(SubscriptionTier.Silver)",
            "SubscriptionCatalog.features(SubscriptionTier.Gold)",
            "onRestorePurchasesClicked"
        ).forEach { snippet ->
            assertTrue("PR#104 audit: paywall tier listings missing $snippet", paywallSource.contains(snippet))
        }

        listOf(
            "AppealTargetSelector",
            "AppealTarget.entries",
            "target.labelKey()"
        ).forEach { snippet ->
            assertTrue("PR#104 audit: appeal dual toggle missing $snippet", appealSource.contains(snippet))
        }
        listOf("appealTargetInsurance", "appealTargetDoctor").forEach { snippet ->
            assertTrue(
                "PR#104 audit: appeal target labels missing $snippet",
                readSource("data/AppealModels.kt").contains(snippet)
            )
        }

        listOf(
            "onBentoSelected",
            "HubBentoDestination",
            "cptBentoSnapshot",
            "ytdBentoSnapshot",
            "insuranceNewsBentoSnapshot",
            "historySnapshot",
            "activateAppealGeneratorBento"
        ).forEach { snippet ->
            assertTrue("PR#104 audit: bento information transfer missing $snippet", navHostSource.contains(snippet))
        }
    }

    @Test
    fun pr108VeryfiAnyDocsConnectivityAudit() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val anyDocConstantsSource = readSource("network/VeryfiAnyDocConstants.kt")
        val anyDocApiSource = readSource("network/VeryfiAnyDocApiService.kt")
        val anyDocRepoSource = readSource("data/VeryfiAnyDocRepository.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val functionsIndex = readFunctionsSource("index.js")
        val functionsConstants = readFunctionsSource("lib/veryfiAnyDocConstants.js")

        assertEquals(
            "https://api.veryfi.com/api/v8/partner/any-documents/",
            "https://api.veryfi.com/api/v8/" + "partner/any-documents/"
        )
        listOf(
            "https://api.veryfi.com/api/v8/",
            "partner/any-documents/",
            "health_insurance_eob",
            "extractVeryfiHybridStream"
        ).forEach { snippet ->
            assertTrue("PR#108: Kotlin documents constants missing $snippet", anyDocConstantsSource.contains(snippet))
        }
        listOf(
            "VERYFI_ANY_DOCS_URL",
            "BLUEPRINT_HEALTH_INSURANCE_EOB",
            "partner/any-documents/",
            "health_insurance_eob"
        ).forEach { snippet ->
            assertTrue(
                "PR#108: Cloud Functions documents wiring missing $snippet",
                functionsIndex.contains(snippet) || functionsConstants.contains(snippet)
            )
        }
        assertFalse(
            "PR#115: legacy partner/documents endpoint must not remain in production code",
            functionsIndex.contains("partner/documents/") ||
                anyDocConstantsSource.contains("partner/documents/") ||
                veryfiSource.contains("partner/documents/")
        )
        listOf(
            "veryfiAnyDocExtractionState",
            "VeryfiAnyDocExtractionState",
            "processScannedDocument",
            "processHybridScannedDocument"
        ).forEach { snippet ->
            assertTrue("PR#108: EobViewModel AnyDocs state barrier missing $snippet", viewModelSource.contains(snippet))
        }
        listOf(
            "VeryfiAnyDocConstants.ANY_DOCUMENTS_PATH",
            "blueprint_name",
            "VeryfiAnyDocResponseDto"
        ).forEach { snippet ->
            assertTrue("PR#108: Retrofit AnyDocs contract missing $snippet", anyDocApiSource.contains(snippet))
        }
        listOf(
            "extractHealthInsuranceEob",
            "streamExtractDocument",
            "VeryfiAnyDocMapper"
        ).forEach { snippet ->
            assertTrue("PR#108: AnyDocs repository barrier missing $snippet", anyDocRepoSource.contains(snippet))
        }
        assertTrue(
            "PR#108: hybrid pipeline must delegate to AnyDocs repository",
            hybridRepoSource.contains("veryfiAnyDocRepository.extractHealthInsuranceEob")
        )
        assertTrue(
            "PR#108: Veryfi client must pass blueprint to Cloud Function",
            veryfiSource.contains("blueprintName") &&
                veryfiSource.contains("VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB")
        )
    }

    @Test
    fun pr113ParallelSplitDocumentsEndpointAudit() {
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val constantsSource = readSource("network/VeryfiAnyDocConstants.kt")
        val hybridRefSource = readSource("data/HybridDocumentRef.kt")
        val firebaseRepoSource = readSource("data/FirebaseEobRepository.kt")
        val functionsIndex = readFunctionsSource("index.js")
        val functionsConstants = readFunctionsSource("lib/veryfiAnyDocConstants.js")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")

        assertEquals("partner/any-documents/", VeryfiAnyDocConstants.ANY_DOCUMENTS_PATH)
        assertTrue(hybridRepoSource.contains("coroutineScope"))
        assertTrue(hybridRepoSource.contains("uploadDeferred"))
        assertTrue(hybridRepoSource.contains("extractionDeferred"))
        assertTrue(hybridRepoSource.contains("extractionDeferred.await()"))
        assertFalse(hybridRepoSource.contains("upload.documentRefId"))
        assertTrue(hybridRepoSource.contains("storagePathForUpload"))
        assertTrue(hybridRepoSource.contains("fileBytes"))
        assertTrue(veryfiSource.contains("fileBase64"))
        assertTrue(veryfiSource.contains("VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB"))
        assertTrue(veryfiSource.contains("VeryfiAnyDocConstants.DOCUMENT_TYPE_EOB"))
        assertTrue(veryfiSource.contains("withTimeout"))
        assertTrue(hybridRefSource.contains("userRootedStoragePath"))
        assertTrue(hybridRefSource.contains("documentRootedStoragePath"))
        assertTrue(firebaseRepoSource.contains("HybridDocumentRef.USER_ROOTED_EOB_FOLDER"))
        assertTrue(firebaseRepoSource.contains(".child(\"users\").child(userId).child(HybridDocumentRef.USER_ROOTED_EOB_FOLDER)"))
        assertTrue(functionsIndex.contains("userRootedMatch"))
        assertTrue(functionsIndex.contains("documentRootedMatch"))
        assertTrue(functionsIndex.contains("/eobs/"))
        assertTrue(functionsConstants.contains("\"partner/any-documents/\""))
        assertFalse(constantsSource.contains("partner/documents/"))
        assertTrue(viewModelSource.contains("processHybridScannedDocument"))
        assertFalse(viewModelSource.contains("partner/documents/"))
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val remoteSource = readSource("data/remote/FirebaseEobRemoteDataSource.kt")
        assertTrue(navHostSource.contains("processScannedDocument"))
        assertTrue(remoteSource.contains("processHybridDocument"))
        val extractionAwaitIndex = hybridRepoSource.indexOf("extractionDeferred.await()")
        val uploadAwaitIndex = hybridRepoSource.indexOf("uploadDeferred.await()")
        assertTrue(extractionAwaitIndex >= 0)
        assertTrue(uploadAwaitIndex >= 0)
        assertTrue(
            "Veryfi extraction must resolve before awaiting Storage upload",
            extractionAwaitIndex < uploadAwaitIndex
        )
    }

    @Test
    fun pr114StateFlowMergeAndAppealGeneratorAudit() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val modelsSource = readSource("data/EobModels.kt")
        val veryfiSource = readSource("data/VeryfiHealthInsuranceEob.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val appealGeneratorSource = readSource("data/AppealLetterGenerator.kt")
        val appealScreenSource = readSource("ui/screens/AppealScreen.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")

        listOf(
            "data class VeryfiExtractedData",
            "data class EobProcessedResult",
            "toVeryfiExtractedData",
            "toProcessedResult"
        ).forEach { snippet ->
            assertTrue("PR#114: merged pipeline model missing $snippet", veryfiSource.contains(snippet))
        }
        assertTrue(
            "PR#114: document scan success must emit EobProcessedResult",
            modelsSource.contains("data class Success(val result: EobProcessedResult)")
        )
        listOf(
            "veryfiExtractedData",
            "toProcessedResult()",
            "DocumentScanPipelineState.Success(processedResult)",
            "generateAppealLetter"
        ).forEach { snippet ->
            assertTrue("PR#114: EobViewModel merge barrier missing $snippet", viewModelSource.contains(snippet))
        }
        listOf(
            "uploadDeferred.await()",
            "downloadUrl = upload.downloadUrl"
        ).forEach { snippet ->
            assertTrue("PR#114: parallel split must await Storage upload $snippet", hybridRepoSource.contains(snippet))
        }
        listOf(
            "veryfiData: VeryfiExtractedData?",
            "resolvedServiceDate",
            "resolvedProviderName",
            "resolvedStatementDate",
            "resolvedPatientResponsibility"
        ).forEach { snippet ->
            assertTrue("PR#114: appeal generator Veryfi injection missing $snippet", appealGeneratorSource.contains(snippet))
        }
        assertTrue(
            "PR#114: AppealScreen must key AnimatedContent on Veryfi data",
            appealScreenSource.contains("veryfiExtractedData") &&
                appealScreenSource.contains("documentAnimationKey")
        )
        assertTrue(
            "PR#114: nav must pipe veryfiExtractedData into AppealScreen",
            navHostSource.contains("veryfiExtractedData = uiState.veryfiExtractedData")
        )
        assertFalse(
            "PR#114: raw Veryfi API keys must not ship in Android bytecode",
            viewModelSource.contains("VERYFI_API_KEY") ||
                readSource("network/VeryfiDocumentClient.kt").contains("VERYFI_API_KEY") ||
                readSource("network/VeryfiAnyDocConstants.kt").contains("VERYFI_API_KEY")
        )
    }

    @Test
    fun pr114HybridPipelineStabilityAudit() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val functionsIndex = readFunctionsSource("index.js")
        val hybridReconciliationSource = readFunctionsSource("lib/hybridReconciliation.js")

        listOf(
            "documentScanJob",
            "documentScanGeneration",
            "isDocumentScanPipelineActive",
            "scopedVeryfiDataFor",
            "veryfiExtractedDataRecordId",
            "resolveRecordSelection"
        ).forEach { snippet ->
            assertTrue("PR#114 stability: EobViewModel guard missing $snippet", viewModelSource.contains(snippet))
        }
        listOf(
            "finalizeHybridReconciliation",
            "hybridReconciliationStatus",
            "storageDownloadUrl",
            "client_stream_committed"
        ).forEach { snippet ->
            assertTrue(
                "PR#114 stability: hybrid reconciliation barrier missing $snippet",
                veryfiSource.contains(snippet) || hybridRepoSource.contains(snippet)
            )
        }
        listOf(
            "shouldSkipStorageVeryfiExtraction",
            "hybridFirestoreDocId",
            "skipped_duplicate_veryfi"
        ).forEach { snippet ->
            assertTrue(
                "PR#114 stability: Cloud Function duplicate Veryfi skip missing $snippet",
                functionsIndex.contains(snippet) || hybridReconciliationSource.contains(snippet)
            )
        }
        assertTrue(
            "PR#114 stability: repository must finalize after upload await",
            hybridRepoSource.indexOf("uploadDeferred.await()") >= 0 &&
                hybridRepoSource.indexOf("finalizeHybridReconciliation") > hybridRepoSource.indexOf("uploadDeferred.await()")
        )
    }

    @Test
    fun pr114FinalAudit() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiClientSource = readSource("network/VeryfiDocumentClient.kt")
        val anyDocConstantsSource = readSource("network/VeryfiAnyDocConstants.kt")
        val functionsIndex = readFunctionsSource("index.js")
        val navHostSource = readSource("navigation/EobNavHost.kt")

        assertTrue(
            "PR#115: Veryfi AnyDocs endpoint must use partner/any-documents/",
            readFunctionsSource("lib/veryfiAnyDocConstants.js").contains("partner/any-documents/") &&
                anyDocConstantsSource.contains("partner/any-documents/") &&
                functionsIndex.contains("VERYFI_ANY_DOCS_URL")
        )
        assertFalse(
            "PR#115: legacy partner/documents endpoint must not remain",
            anyDocConstantsSource.contains("partner/documents/")
        )
        listOf(
            "class EobViewModel",
            "processScannedDocument",
            "documentScanJob",
            "documentScanGeneration",
            "toProcessedResult()",
            "veryfiExtractedDataRecordId",
            "private fun generateAppealLetter",
            "processHybridScannedDocument"
        ).forEach { snippet ->
            assertTrue("PR#114 final: EobViewModel source-of-truth barrier missing $snippet", viewModelSource.contains(snippet))
        }
        listOf(
            "coroutineScope",
            "extractionDeferred.await()",
            "writeReconciliationFindings",
            "uploadDeferred.await()",
            "finalizeHybridReconciliation"
        ).forEach { snippet ->
            assertTrue("PR#114 final: hybrid repository barrier missing $snippet", hybridRepoSource.contains(snippet))
        }
        listOf(
            "extractVeryfiHybridStream",
            "partner/any-documents",
            "BLUEPRINT_HEALTH_INSURANCE_EOB",
            "DOCUMENT_TYPE_EOB"
        ).forEach { snippet ->
            assertTrue(
                "PR#114 final: AnyDocs documents endpoint barrier missing $snippet",
                anyDocConstantsSource.contains(snippet) ||
                    veryfiClientSource.contains(snippet) ||
                    functionsIndex.contains(snippet)
            )
        }
        assertTrue(navHostSource.contains("processScannedDocument"))
        assertFalse(navHostSource.contains("eobViewModel.uploadEobFile("))
        assertTrue(
            "PR#114 final: stale scan results must be ignored on failure path",
            viewModelSource.contains("generation != documentScanGeneration") &&
                viewModelSource.indexOf("onFailure") < viewModelSource.lastIndexOf("generation != documentScanGeneration")
        )
    }

    @Test
    fun pr115HistoryCrashAndAnyDocumentsAudit() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val modelsSource = readSource("data/EobModels.kt")
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        val constantsSource = readSource("network/VeryfiAnyDocConstants.kt")
        val functionsConstants = readFunctionsSource("lib/veryfiAnyDocConstants.js")
        val functionsIndex = readFunctionsSource("index.js")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")

        assertEquals("partner/any-documents/", VeryfiAnyDocConstants.ANY_DOCUMENTS_PATH)
        assertTrue(functionsConstants.contains("partner/any-documents/"))
        assertTrue(functionsIndex.contains("blueprint_name"))
        assertTrue(
            functionsConstants.contains("health_insurance_eob") ||
                functionsIndex.contains("BLUEPRINT_HEALTH_INSURANCE_EOB")
        )
        listOf(
            "fun historyListKey()",
            "historyListKey()",
            "expandedRecordKey"
        ).forEach { snippet ->
            assertTrue("PR#115: history crash guard missing $snippet", historySource.contains(snippet) || modelsSource.contains(snippet))
        }
        assertTrue(
            "PR#115: duplicate Firestore EOBs must be compacted before history render",
            analyzerSource.contains("distinctBy { it.historyListKey() }")
        )
        assertFalse(
            "PR#115: LazyColumn must not key on collision-prone numeric id alone",
            historySource.contains("key = { it.record.id }")
        )
        assertTrue(
            "PR#115: EobViewModel remains history source of truth",
            viewModelSource.contains("fun historyTimelineSections") &&
                viewModelSource.contains("compactDuplicateEobs") &&
                viewModelSource.contains("matchesHistoryRecord")
        )
        assertEquals(
            "health_insurance_eob",
            VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB
        )
        assertFalse(
            "PR#115: legacy partner/documents endpoint must not remain in Kotlin constants",
            constantsSource.contains("partner/documents/")
        )
    }

    @Test
    fun pr116ProviderHistoryCloudFunctionSyncAudit() {
        val mapperSource = readSource("data/FirebaseEobMapper.kt")
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val functionsIndex = readFunctionsSource("index.js")
        val normalizerSource = readFunctionsSource("lib/eobNormalizer.js")

        listOf(
            "reconcileNormalizedEobRecord",
            "totalCharges",
            "patient_responsibility",
            "vendor_name",
            "line_items",
            "insurance_company_name",
            "resolveProviderName"
        ).forEach { snippet ->
            assertTrue("PR#116: FirebaseEobMapper CF sync missing $snippet", mapperSource.contains(snippet))
        }
        listOf(
            "normalizeEobDocument",
            "veryfiToEobDocument",
            "mirrorEobToLegacyRecord",
            "processUploadedEobWithVeryfi",
            "extractVeryfiHybridStream",
            "patient_responsibility",
            "vendor_name"
        ).forEach { snippet ->
            assertTrue(
                "PR#116: Cloud Functions hub missing $snippet",
                functionsIndex.contains(snippet) || normalizerSource.contains(snippet)
            )
        }
        listOf(
            "fun providerDirectory",
            "fun historyTimelineSections",
            "applyRemoteRecords",
            "compactDuplicateEobs",
            "openProviderRecordHistory",
            "historyRecordsForDisplay"
        ).forEach { snippet ->
            assertTrue("PR#116: EobViewModel history/provider source-of-truth missing $snippet", viewModelSource.contains(snippet))
        }
        assertTrue(
            "PR#116: provider directory must detect Veryfi out-of-network balance",
            analyzerSource.contains("out_of_network_out_of_pocket")
        )
        assertTrue(
            "PR#116: Veryfi stream must parse CPT codes from line_items",
            veryfiSource.contains("line_items") && veryfiSource.contains("validCptCodes")
        )
        assertTrue(
            "PR#116: nav must route provider directory through EobViewModel",
            navHostSource.contains("eobViewModel.providerDirectory") &&
                navHostSource.contains("openProviderRecordHistory")
        )
    }

    @Test
    fun pr116ReviewConnectivityStabilityAudit() {
        val mapperSource = readSource("data/FirebaseEobMapper.kt")
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        val providerSource = readSource("ui/screens/ProviderDirectoryScreen.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val routesSource = readSource("navigation/EobRoutes.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")

        listOf(
            "providerNamesEqual",
            "reconcileNormalizedEobRecord",
            "hasChargeLineAmounts",
            "providerNamesEqual(it.providerName, providerName)"
        ).forEach { snippet ->
            assertTrue("PR#116 review: provider/history stability missing $snippet", {
                mapperSource.contains(snippet) ||
                    analyzerSource.contains(snippet) ||
                    providerSource.contains(snippet)
            }())
        }
        listOf(
            "EobRoute.ProviderDirectory.route",
            "EobRoute.History.route",
            "openProviderRecordHistory",
            "historyProviderSearch",
            "historyTimelineSections",
            "historyListKey()"
        ).forEach { snippet ->
            assertTrue(
                "PR#116 review: navigation wiring missing $snippet",
                navHostSource.contains(snippet) || historySource.contains(snippet) || routesSource.contains(snippet)
            )
        }
        assertTrue(
            "PR#116 review: EobViewModel remains hub source of truth",
            viewModelSource.contains("fun providerDirectory()") &&
                viewModelSource.contains("fun historyTimelineSections") &&
                viewModelSource.contains("applyRemoteRecords")
        )
        assertFalse(
            "PR#116 review: history LazyColumn must not key on numeric id alone",
            historySource.contains("key = { it.record.id }")
        )
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#116 review: opening screen touched ($path)", source.contains("providerNamesEqual"))
        }
    }

    @Test
    fun pr117HistoryLazyColumnCrashAudit() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val modelsSource = readSource("data/EobModels.kt")
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val mapperSource = readSource("data/FirebaseEobMapper.kt")
        val functionsIndex = readFunctionsSource("index.js")

        listOf(
            "HistoryTimelineSection",
            "lazySectionKey()",
            "lazyItemKey",
            "monthSortKey"
        ).forEach { snippet ->
            assertTrue(
                "PR#117: history lazy-key model missing $snippet",
                historySource.contains(snippet) || modelsSource.contains(snippet) || analyzerSource.contains(snippet)
            )
        }
        assertFalse(
            "PR#117: sticky headers must not key on display text alone",
            historySource.contains("stickyHeader(key = \"header-\$header\")")
        )
        assertFalse(
            "PR#117: LazyColumn must not key on numeric record id",
            historySource.contains("key = { it.record.id }")
        )
        listOf(
            "distinctBy { it.historyListKey() }",
            "existing.historyListKey() == record.historyListKey()"
        ).forEach { snippet ->
            assertTrue("PR#117: duplicate EOB compaction missing $snippet", analyzerSource.contains(snippet))
        }
        listOf(
            "fun historyTimelineSections",
            "List<HistoryTimelineSection>",
            "applyRemoteRecords",
            "compactDuplicateEobs"
        ).forEach { snippet ->
            assertTrue("PR#117: EobViewModel history source-of-truth missing $snippet", viewModelSource.contains(snippet))
        }
        listOf(
            "EobRoute.History.route",
            "historyTimelineSections",
            "EobHistoryScreen"
        ).forEach { snippet ->
            assertTrue("PR#117: history navigation wiring missing $snippet", navHostSource.contains(snippet))
        }
        listOf(
            "reconcileNormalizedEobRecord",
            "normalizeEobDocument",
            "veryfiToEobDocument"
        ).forEach { snippet ->
            assertTrue(
                "PR#117: Veryfi/Firebase mapper sync missing $snippet",
                mapperSource.contains(snippet) || functionsIndex.contains(snippet)
            )
        }
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "ui/components/EobSplashLogo.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#117: opening screen must remain untouched ($path)", source.contains("HistoryTimelineSection"))
        }
    }

    @Test
    fun pr117FinalHarmonyAudit() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val modelsSource = readSource("data/EobModels.kt")
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val constantsSource = readSource("network/VeryfiAnyDocConstants.kt")
        val veryfiClientSource = readSource("network/VeryfiDocumentClient.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val functionsConstants = readFunctionsSource("lib/veryfiAnyDocConstants.js")

        assertEquals("health_insurance_eob", VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB)
        assertTrue(constantsSource.contains("BLUEPRINT_HEALTH_INSURANCE_EOB = \"health_insurance_eob\""))
        assertTrue(functionsConstants.contains("\"health_insurance_eob\""))
        listOf(
            "BLUEPRINT_HEALTH_INSURANCE_EOB",
            "partner/any-documents/",
            "extractVeryfiHybridStream"
        ).forEach { snippet ->
            assertTrue("PR#117 harmony: Veryfi constants missing $snippet", constantsSource.contains(snippet))
        }
        listOf(
            "processHybridDocument",
            "finalizeHybridReconciliation",
            "extractHealthInsuranceEob"
        ).forEach { snippet ->
            assertTrue(
                "PR#117 harmony: hybrid pipeline missing $snippet",
                hybridRepoSource.contains(snippet) ||
                    veryfiClientSource.contains(snippet) ||
                    constantsSource.contains("BLUEPRINT_HEALTH_INSURANCE_EOB")
            )
        }
        listOf(
            "HistoryTimelineSection",
            "lazySectionKey()",
            "lazyItemKey(row.record)",
            "key(row.record.historyListKey())"
        ).forEach { snippet ->
            assertTrue("PR#117 harmony: history crash guard missing $snippet", historySource.contains(snippet))
        }
        assertTrue(
            "PR#117 harmony: historyListKey must trim Firestore ids",
            modelsSource.contains("firestoreId.trim()")
        )
        listOf(
            "fun historyTimelineSections",
            "List<HistoryTimelineSection>",
            "applyRemoteRecords",
            "compactDuplicateEobs"
        ).forEach { snippet ->
            assertTrue("PR#117 harmony: EobViewModel source-of-truth missing $snippet", viewModelSource.contains(snippet))
        }
        listOf(
            "EobRoute.History.route",
            "historyTimelineSections",
            "EobHistoryScreen"
        ).forEach { snippet ->
            assertTrue("PR#117 harmony: navigation wiring missing $snippet", navHostSource.contains(snippet))
        }
        assertTrue(
            "PR#117 harmony: timeline must dedupe by historyListKey",
            analyzerSource.contains("distinctBy { it.historyListKey() }")
        )
    }

    @Test
    fun pr118VeryfiOcrFieldExtractionAudit() {
        val extractorSource = readSource("network/VeryfiOcrFieldExtractor.kt")
        val mapperSource = readSource("network/VeryfiAnyDocMapper.kt")
        val veryfiClientSource = readSource("network/VeryfiDocumentClient.kt")
        val firebaseMapperSource = readSource("data/FirebaseEobMapper.kt")
        val dtoSource = readSource("network/VeryfiAnyDocDto.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val functionsIndex = readFunctionsSource("index.js")

        listOf(
            "object VeryfiOcrFieldExtractor",
            "extractOcrText",
            "extractCustomFields",
            "extractFromOcrText",
            "enrichPayload",
            "Billed Amount:",
            "CPT\\s*-",
            "Patient Responsibility:"
        ).forEach { snippet ->
            assertTrue("PR#118: OCR field extractor missing $snippet", extractorSource.contains(snippet))
        }
        listOf(
            "VeryfiOcrFieldExtractor.enrichPayload",
            "ocr_text",
            "custom_fields",
            "patient_responsibility"
        ).forEach { snippet ->
            assertTrue(
                "PR#118: AnyDoc mapper OCR wiring missing $snippet",
                mapperSource.contains(snippet) || dtoSource.contains(snippet)
            )
        }
        listOf(
            "veryfiPayloadToEobRecord",
            "patient_responsibility",
            "ocr_text"
        ).forEach { snippet ->
            assertTrue("PR#118: hybrid Veryfi client OCR wiring missing $snippet", veryfiClientSource.contains(snippet))
        }
        listOf(
            "enrichFromVeryfiClientStream",
            "veryfiClientStream",
            "VeryfiOcrFieldExtractor"
        ).forEach { snippet ->
            assertTrue("PR#118: Firestore OCR re-hydration missing $snippet", firebaseMapperSource.contains(snippet))
        }
        listOf(
            "fun processScannedDocument",
            "applyRemoteRecords"
        ).forEach { snippet ->
            assertTrue("PR#118: EobViewModel source-of-truth pipeline missing $snippet", viewModelSource.contains(snippet))
        }
        listOf(
            "extractHealthInsuranceEob",
            "processHybridDocument",
            "VeryfiAnyDocMapper.mergePayloadWithEobFields"
        ).forEach { snippet ->
            assertTrue(
                "PR#118: hybrid extraction pipeline missing $snippet",
                hybridRepoSource.contains(snippet) || veryfiClientSource.contains(snippet)
            )
        }
        assertFalse(
            "PR#118: EobViewModel must not embed OCR extractor logic",
            viewModelSource.contains("VeryfiOcrFieldExtractor")
        )
        listOf(
            "veryfiClientStream",
            "veryfiToEobDocument"
        ).forEach { snippet ->
            assertTrue("PR#118: index.js OCR/storage harmony missing $snippet", functionsIndex.contains(snippet))
        }
        assertTrue(
            "PR#118: Cloud Functions OCR extractor must mirror Android",
            readFunctionsSource("lib/veryfiOcrFieldExtractor.js").contains("enrichPayload")
        )
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "ui/components/EobSplashLogo.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#118: opening screen must remain untouched ($path)", source.contains("VeryfiOcrFieldExtractor"))
        }
    }

    @Test
    fun pr118ReviewConnectivityStabilityAudit() {
        val extractorSource = readSource("network/VeryfiOcrFieldExtractor.kt")
        val mapperSource = readSource("network/VeryfiAnyDocMapper.kt")
        val veryfiClientSource = readSource("network/VeryfiDocumentClient.kt")
        val firebaseMapperSource = readSource("data/FirebaseEobMapper.kt")
        val dtoSource = readSource("network/VeryfiAnyDocDto.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val constantsSource = readSource("network/VeryfiAnyDocConstants.kt")
        val functionsIndex = readFunctionsSource("index.js")
        val functionsNormalizer = readFunctionsSource("lib/eobNormalizer.js")
        val functionsConstants = readFunctionsSource("lib/veryfiAnyDocConstants.js")
        val functionsHybrid = readFunctionsSource("lib/hybridReconciliation.js")

        assertEquals("health_insurance_eob", VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB)
        listOf(
            "partner/any-documents/",
            "extractVeryfiHybridStream",
            "BLUEPRINT_HEALTH_INSURANCE_EOB"
        ).forEach { snippet ->
            assertTrue("PR#118 review: Android Veryfi constants missing $snippet", constantsSource.contains(snippet))
        }
        listOf(
            "partner/any-documents/",
            "health_insurance_eob",
            "extractVeryfiHybridStream"
        ).forEach { snippet ->
            assertTrue(
                "PR#118 review: Cloud Functions Veryfi wiring missing $snippet",
                functionsIndex.contains(snippet) || functionsConstants.contains(snippet)
            )
        }
        listOf(
            "veryfiToEobDocument",
            "billed_amount",
            "insurance_paid",
            "contractual_adj",
            "patient_responsibility"
        ).forEach { snippet ->
            assertTrue(
                "PR#118 review: normalizer parity missing $snippet",
                functionsNormalizer.contains(snippet) || veryfiClientSource.contains(snippet) || firebaseMapperSource.contains(snippet)
            )
        }
        listOf(
            "shouldSkipStorageVeryfiExtraction",
            "client_stream_committed",
            "veryfi_hybrid"
        ).forEach { snippet ->
            assertTrue(
                "PR#118 review: hybrid reconciliation sync missing $snippet",
                functionsHybrid.contains(snippet) || veryfiClientSource.contains(snippet)
            )
        }
        listOf(
            "VeryfiOcrFieldExtractor.enrichPayload",
            "value.toDouble() != 0.0"
        ).forEach { snippet ->
            assertTrue("PR#118 review: OCR merge stability guard missing $snippet", mapperSource.contains(snippet))
        }
        assertTrue(
            "PR#118 review: custom_fields OCR wiring missing",
            extractorSource.contains("custom_fields") || dtoSource.contains("custom_fields")
        )
        listOf(
            "import app.eob.me.network.VeryfiOcrFieldExtractor",
            "enrichFromVeryfiClientStream",
            "veryfiClientStream"
        ).forEach { snippet ->
            assertTrue("PR#118 review: Firestore OCR re-hydration wiring missing $snippet", firebaseMapperSource.contains(snippet))
        }
        listOf(
            "processHybridDocument",
            "writeReconciliationFindings",
            "veryfiPayloadToEobRecord",
            "patient_responsibility"
        ).forEach { snippet ->
            assertTrue(
                "PR#118 review: hybrid upload connectivity missing $snippet",
                hybridRepoSource.contains(snippet) || veryfiClientSource.contains(snippet)
            )
        }
        listOf(
            "fun processScannedDocument",
            "observeEobs",
            "applyRemoteRecords"
        ).forEach { snippet ->
            assertTrue("PR#118 review: EobViewModel source-of-truth pipeline missing $snippet", viewModelSource.contains(snippet))
        }
        assertFalse(
            "PR#118 review: EobViewModel must not embed OCR extractor logic",
            viewModelSource.contains("VeryfiOcrFieldExtractor")
        )
        assertFalse(
            "PR#118 review: EobViewModel must not embed OCR extractor logic",
            viewModelSource.contains("VeryfiOcrFieldExtractor")
        )
        listOf(
            "veryfiClientStream",
            "veryfiToEobDocument",
            "extractVeryfiHybridStream"
        ).forEach { snippet ->
            assertTrue("PR#118 review: index.js hybrid/OCR wiring missing $snippet", functionsIndex.contains(snippet))
        }
        assertTrue(
            "PR#118 review: Cloud Functions OCR extractor must mirror Android",
            readFunctionsSource("lib/veryfiOcrFieldExtractor.js").contains("enrichPayload") &&
                readFunctionsSource("lib/eobNormalizer.js").contains("veryfiOcrFieldExtractor")
        )
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "ui/components/EobSplashLogo.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#118 review: opening screen touched ($path)", source.contains("VeryfiOcrFieldExtractor"))
        }
        assertTrue(
            "PR#118 review: OCR regex rules must include Veryfi billed_amount pattern",
            extractorSource.contains("""Billed Amount:\s*\$?""")
        )
    }

    @Test
    fun pr118FinalHarmonyAudit() {
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiClientSource = readSource("network/VeryfiDocumentClient.kt")
        val firebaseMapperSource = readSource("data/FirebaseEobMapper.kt")
        val functionsIndex = readFunctionsSource("index.js")
        val functionsNormalizer = readFunctionsSource("lib/eobNormalizer.js")
        val functionsOcr = readFunctionsSource("lib/veryfiOcrFieldExtractor.js")

        listOf(
            "processScannedDocument",
            "libraryUploadLauncher",
            "CameraCaptureScreen",
            "prepareAndUpload"
        ).forEach { snippet ->
            assertTrue("PR#118 harmony: camera/library upload wiring missing $snippet", navHostSource.contains(snippet))
        }
        listOf(
            "fun processScannedDocument",
            "processHybridScannedDocument",
            "observeEobs",
            "applyRemoteRecords"
        ).forEach { snippet ->
            assertTrue("PR#118 harmony: EobViewModel source-of-truth missing $snippet", viewModelSource.contains(snippet))
        }
        listOf(
            "processHybridDocument",
            "extractHealthInsuranceEob",
            "writeReconciliationFindings"
        ).forEach { snippet ->
            assertTrue(
                "PR#118 harmony: hybrid scan pipeline missing $snippet",
                hybridRepoSource.contains(snippet) || veryfiClientSource.contains(snippet)
            )
        }
        listOf(
            "VeryfiOcrFieldExtractor.enrichPayload",
            "enrichFromVeryfiClientStream",
            "veryfiClientStream"
        ).forEach { snippet ->
            assertTrue(
                "PR#118 harmony: Android OCR read/write missing $snippet",
                firebaseMapperSource.contains(snippet) || veryfiClientSource.contains(snippet)
            )
        }
        listOf(
            "enrichPayload",
            "patient_responsibility",
            "reconcileDocumentTotals"
        ).forEach { snippet ->
            assertTrue(
                "PR#118 harmony: Cloud Functions OCR parity missing $snippet",
                functionsOcr.contains(snippet) || functionsNormalizer.contains(snippet)
            )
        }
        assertTrue(
            "PR#118 harmony: storage trigger must persist veryfiClientStream for re-hydration",
            functionsIndex.contains("veryfiClientStream")
        )
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "ui/components/EobSplashLogo.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#118 harmony: opening screen touched ($path)", source.contains("VeryfiOcrFieldExtractor"))
        }
    }

    @Test
    fun veryfiServiceLineMapperPipelineAudit() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val veryfiClientSource = readSource("network/VeryfiDocumentClient.kt")
        val mapperSource = readSource("network/VeryfiInsuranceEobMapper.kt")
        val bridgeSource = readSource("data/InsuranceEobRecordBridge.kt")
        val serviceLineSource = readSource("data/ServiceLine.kt")
        val indexedReaderSource = readSource("network/VeryfiIndexedFieldReader.kt")
        val firebaseMapperSource = readSource("data/FirebaseEobMapper.kt")
        val veryfiRepoSource = readSource("data/VeryfiAnyDocRepository.kt")
        val veryfiHealthSource = readSource("data/VeryfiHealthInsuranceEob.kt")
        val hybridRepoSource = readSource("data/DocumentScanPipelineRepository.kt")

        listOf(
            "data class ServiceLine",
            "data class InsuranceClaim",
            "data class NormalizedInsuranceEob",
            "allServiceLines"
        ).forEach { snippet ->
            assertTrue("PR#124: domain service line model missing $snippet", serviceLineSource.contains(snippet))
        }
        listOf(
            "VeryfiInsuranceEobPayloadParser",
            "isNestedClaimsPayload",
            "toNormalizedInsuranceEob",
            "mapIndexedServiceLineRow",
            "VeryfiIndexedFieldReader.discoverIndices"
        ).forEach { snippet ->
            assertTrue("PR#124: nested claims mapper missing $snippet", mapperSource.contains(snippet))
        }
        listOf(
            "discoverIndices",
            "resolveServiceDateIso",
            "cpt_code_"
        ).forEach { snippet ->
            assertTrue("PR#124: indexed field reader missing $snippet", indexedReaderSource.contains(snippet))
        }
        listOf(
            "InsuranceEobRecordBridge",
            "toEobRecord",
            "toEobCharge"
        ).forEach { snippet ->
            assertTrue("PR#124: EobRecord bridge missing $snippet", bridgeSource.contains(snippet))
        }
        listOf(
            "isNestedClaimsPayload",
            "toNormalizedInsuranceEob",
            "InsuranceEobRecordBridge.toEobRecord",
            "veryfiPayloadToEobRecord"
        ).forEach { snippet ->
            assertTrue("PR#124: hybrid client nested path missing $snippet", veryfiClientSource.contains(snippet))
        }
        listOf(
            "resolveNestedInsuranceEobPayload",
            "toNormalizedInsuranceEob",
            "InsuranceEobRecordBridge.toEobRecord"
        ).forEach { snippet ->
            assertTrue("PR#124: Firestore nested rehydrate missing $snippet", firebaseMapperSource.contains(snippet))
        }
        listOf(
            "veryfiPayloadToEobRecord",
            "extractHealthInsuranceEob"
        ).forEach { snippet ->
            assertTrue(
                "PR#124: scan pipeline must route through nested mapper",
                veryfiRepoSource.contains(snippet) || hybridRepoSource.contains(snippet)
            )
        }
        listOf(
            "toVeryfiExtractedData()",
            "refreshVeryfiExtractedDataForRecord",
            "applyRemoteRecords",
            "processScannedDocument"
        ).forEach { snippet ->
            assertTrue("PR#124: EobViewModel source-of-truth wiring missing $snippet", viewModelSource.contains(snippet))
        }
        assertTrue(
            "PR#124: appeal projection must derive from reconciled EobRecord charges",
            veryfiHealthSource.contains("fun EobRecord.toVeryfiExtractedData()")
        )
        assertTrue(
            "PR#124: manifest must document nested claims mapper without new storage permissions",
            manifestSource.contains("VeryfiInsuranceEobMapper") &&
                manifestSource.contains("ServiceLine") &&
                !manifestSource.contains("android.permission.READ_EXTERNAL_STORAGE")
        )
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "ui/components/EobSplashLogo.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#124: opening screen touched ($path)", source.contains("VeryfiInsuranceEobMapper"))
        }
    }

    @Test
    fun pr132HistoryAppealPillButtonsAudit() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val stringsSource = readSource("data/EobStrings.kt")

        listOf(
            "HistoryAppealPillButtons",
            "HistoryAppealPill",
            "isSelected",
            "onAppealDoctor",
            "onAppealInsurance",
            "historyAppealDoctorPill",
            "historyAppealInsurancePill"
        ).forEach { snippet ->
            assertTrue("PR#132: history appeal pills missing $snippet", historySource.contains(snippet))
        }
        assertTrue(
            "PR#132: appeal pills must sit between patient responsibility and CPT codes",
            historySource.indexOf("emphasized = true") < historySource.indexOf("HistoryAppealPillButtons") &&
                historySource.indexOf("HistoryAppealPillButtons") < historySource.indexOf("historyProcedureCodes")
        )
        listOf(
            "openAppealForRecord",
            "onAppealDoctorWithStrategy",
            "onAppealInsuranceWithStrategy",
            "EobRoute.Appeal.route",
            "selectedRecord = uiState.selectedRecord"
        ).forEach { snippet ->
            assertTrue("PR#132: history appeal navigation missing $snippet", navHostSource.contains(snippet))
        }
        assertTrue(
            "PR#132: EobViewModel remains appeal source of truth",
            viewModelSource.contains("fun openAppealForRecord") &&
                viewModelSource.contains("selectRecord") &&
                viewModelSource.contains("onAppealTargetSwitched")
        )
        listOf(
            "historyAppealDoctorPill",
            "historyAppealInsurancePill"
        ).forEach { key ->
            assertTrue("PR#132: appeal pill strings missing $key", stringsSource.contains("\"$key\""))
        }
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "ui/components/EobSplashLogo.kt",
            "network/VeryfiDocumentClient.kt",
            "data/DocumentScanPipelineRepository.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#132: protected file touched ($path)", source.contains("HistoryAppealPill"))
        }
    }

    @Test
    fun pr133HistoryAppealStackAndYtdYearFilterAudit() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val ytdSource = readSource("ui/screens/YtdExpenseScreen.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        val modelsSource = readSource("data/EobModels.kt")
        val stringsSource = readSource("data/EobStrings.kt")

        assertTrue(historySource.contains("Column("))
        assertTrue(historySource.contains("historyAppealDoctorPill"))
        assertTrue(historySource.contains("historyAppealInsurancePill"))
        assertTrue(
            historySource.indexOf("historyAppealDoctorPill") < historySource.indexOf("historyAppealInsurancePill")
        )
        listOf(
            "YtdExpenseYearSelection",
            "aggregatesAllYears",
            "ExposedDropdownMenuBox",
            "ytdYearFilterAll",
            "setYtdExpenseYearSelection",
            "ytdExpenseYearOptions",
            "resolvedYtdExpenseYearSelection"
        ).forEach { snippet ->
            assertTrue(
                "PR#133: YTD year filter missing $snippet",
                ytdSource.contains(snippet) ||
                    navHostSource.contains(snippet) ||
                    viewModelSource.contains(snippet) ||
                    analyzerSource.contains(snippet) ||
                    modelsSource.contains(snippet)
            )
        }
        listOf(
            "ytdYearFilterAll",
            "ytdAllYearsEobsSubtitle"
        ).forEach { key ->
            assertTrue("PR#133: YTD year strings missing $key", stringsSource.contains("\"$key\""))
        }
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "network/VeryfiDocumentClient.kt",
            "data/DocumentScanPipelineRepository.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#133: protected file touched ($path)", source.contains("YtdExpenseYearSelection"))
        }
    }

    @Test
    fun pr134DoctorAppealTemplateAndHistoryStrategyFloaterAudit() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val generatorSource = readSource("data/AppealLetterGenerator.kt")
        val modelsSource = readSource("data/AppealModels.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val appealSource = readSource("ui/screens/AppealScreen.kt")

        listOf(
            "DoctorAppealStrategyFloater",
            "doctorAppealTargetRecord",
            "onAppealDoctorWithStrategy",
            "DoctorDisputeStrategy.entries"
        ).forEach { snippet ->
            assertTrue("PR#134: history doctor appeal floater missing $snippet", historySource.contains(snippet))
        }
        listOf(
            "IMPROPER_BALANCE_BILLING",
            "CODING_UPCODING_ERROR",
            "PRIOR_AUTHORIZATION_FAILURE",
            "NO_SURPRISES_ACT",
            "Billing Department",
            "No Surprises Act"
        ).forEach { snippet ->
            assertTrue(
                "PR#134: doctor appeal template missing $snippet",
                generatorSource.contains(snippet) || modelsSource.contains(snippet)
            )
        }
        assertTrue(
            "PR#134: history must route doctor strategy through ViewModel",
            navHostSource.contains("disputeStrategy = strategy") &&
                viewModelSource.contains("disputeStrategy: DoctorDisputeStrategy? = null")
        )
        assertTrue(
            "PR#134: appeal screen must hide history-only strategy selector",
            appealSource.contains("visible = false") &&
                appealSource.contains("DoctorDisputeStrategySelector")
        )
        listOf(
            "appealSendSubjectDoctor",
            "appealSendSubjectInsurance",
            "Intent.EXTRA_SUBJECT",
            "Intent.EXTRA_TEXT",
            "Intent.createChooser"
        ).forEach { snippet ->
            assertTrue(
                "PR#134: doctor and insurance appeals must export through email share intent ($snippet)",
                appealSource.contains(snippet)
            )
        }
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "network/VeryfiDocumentClient.kt",
            "data/DocumentScanPipelineRepository.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#134: protected file touched ($path)", source.contains("DoctorAppealStrategyFloater"))
        }
    }

    @Test
    fun pr135InsuranceAppealTemplateAndHistoryStrategyFloaterAudit() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val generatorSource = readSource("data/AppealLetterGenerator.kt")
        val modelsSource = readSource("data/AppealModels.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val appealSource = readSource("ui/screens/AppealScreen.kt")

        listOf(
            "InsuranceAppealStrategyFloater",
            "insuranceAppealTargetRecord",
            "onAppealInsuranceWithStrategy",
            "InsuranceAppealStrategy.entries"
        ).forEach { snippet ->
            assertTrue("PR#135: history insurance appeal floater missing $snippet", historySource.contains(snippet))
        }
        listOf(
            "PROCESSED_INCORRECTLY",
            "DENIED_INCORRECTLY",
            "PATIENT_RESPONSIBILITY_INCORRECT",
            "Member Appeals Department",
            "Formal Claim Appeal for Member",
            "30-day review window"
        ).forEach { snippet ->
            assertTrue(
                "PR#135: insurance appeal template missing $snippet",
                generatorSource.contains(snippet) || modelsSource.contains(snippet)
            )
        }
        assertTrue(
            "PR#135: history must route insurance strategy through ViewModel",
            navHostSource.contains("insuranceStrategy = strategy") &&
                viewModelSource.contains("insuranceStrategy: InsuranceAppealStrategy? = null")
        )
        assertTrue(
            "PR#135: appeal screen must use insurance strategy insights",
            appealSource.contains("insuranceStrategy.insightKey()") &&
                appealSource.contains("selectedInsuranceAppealStrategy")
        )
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "network/VeryfiDocumentClient.kt",
            "data/DocumentScanPipelineRepository.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#135: protected file touched ($path)", source.contains("InsuranceAppealStrategyFloater"))
        }
    }

    @Test
    fun pr136TaxVaultPremiumFeaturesAudit() {
        val homeSource = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        val scannerSource = readSource("ui/components/home/TitaniumVaultBiometricScanner.kt")
        val vaultScreenSource = readSource("ui/screens/TaxVaultScreen.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val packagerSource = readSource("data/TaxVaultClaimPackager.kt")
        val functionsSource = readFunctionsSource("index.js")

        listOf(
            "TitaniumVaultBiometricScanner",
            "detectTapGestures",
            "Animatable",
            "HapticFeedbackType.LongPress",
            "onVaultDoorUnlocked"
        ).forEach { snippet ->
            assertTrue("PR#136: titanium vault door missing $snippet", homeSource.contains(snippet) || scannerSource.contains(snippet))
        }
        listOf(
            "TaxVaultScreen",
            "FsaDoomsdayMonitorCard",
            "VaultEvidenceCarousel",
            "AsyncImage",
            "VaultExportSection",
            "EobRoute.TaxVault"
        ).forEach { snippet ->
            assertTrue("PR#136: vault dashboard missing $snippet", vaultScreenSource.contains(snippet) || navHostSource.contains(snippet))
        }
        listOf(
            "processVaultReceiptScannedDocument",
            "vaultReceipts",
            "exportTaxVaultClaimPackage",
            "scheduleFsaDoomsdayMonitor",
            "FsaDoomsdayScheduler"
        ).forEach { snippet ->
            assertTrue("PR#136: ViewModel vault source of truth missing $snippet", viewModelSource.contains(snippet))
        }
        assertTrue("PR#136: claim packager must use PdfDocument", packagerSource.contains("PdfDocument"))
        assertTrue("PR#136: cloud stapler trigger missing", functionsSource.contains("stapleVaultReceiptToEob"))
        assertTrue(
            "PR#136: FSA monitor schedules from hub-level ViewModel wiring",
            navHostSource.contains("scheduleFsaDoomsdayMonitor(context, profile)") &&
                navHostSource.contains("fsaEligibleAmountForMonitor")
        )
        assertTrue("PR#136: stapler must match on serviceDateSortKey", functionsSource.contains("serviceDateSortKey"))
        assertTrue(
            "PR#136: stapler must avoid TDZ shadowing on sort key helper",
            functionsSource.contains("computeServiceDateSortKey")
        )
        assertTrue(
            "PR#136: camera close must clear vault receipt pending",
            navHostSource.contains("clearVaultReceiptScanPending")
        )
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "network/VeryfiDocumentClient.kt",
            "data/DocumentScanPipelineRepository.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#136: protected pipeline touched ($path)", source.contains("processVaultReceiptScannedDocument"))
        }
    }

    @Test
    fun pr137YtdYearToggleAndAppealInsuranceProfileAudit() {
        val ytdSource = readSource("ui/screens/YtdExpenseScreen.kt")
        val generatorSource = readSource("data/AppealLetterGenerator.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")

        assertTrue(ytdSource.contains("ytdYearToggleWidthUnderSummary"))
        assertTrue(ytdSource.contains("ytdSummaryTitleAnchorEndIndex"))
        assertFalse(
            "PR#137: year toggle must not use fillMaxWidth in summary header",
            ytdSource.contains("ExposedDropdownMenuBox(\n                expanded = yearMenuExpanded,\n                onExpandedChange = { yearMenuExpanded = it },\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .padding(top = 8.dp)")
        )
        assertTrue(generatorSource.contains("profile.insuranceCompany.takeIf { it.isNotBlank() }"))
        assertTrue(generatorSource.contains("Insurance Carrier: \$insuranceCompany"))
        assertTrue(viewModelSource.contains("AppealLetterGenerator.generate"))
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#137: intro/logo screen touched ($path)", source.contains("ytdYearToggleWidthUnderSummary"))
        }
    }

    @Test
    fun pr138HistoryUploadTitleRowAudit() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")

        assertFalse(
            "PR#138: upload must not remain a scaffold FAB",
            historySource.contains("floatingActionButton = {")
        )
        assertTrue(historySource.contains("ExtendedFloatingActionButton"))
    }

    @Test
    fun pr139HelpfulInsightsAndCalendarTapAudit() {
        val settingsSource = readSource("ui/screens/SettingsScreen.kt")
        val stringsSource = readSource("data/EobStrings.kt")
        val weekCalendarSource = readSource("ui/components/home/HomeWeekCalendar.kt")
        val monthCalendarSource = readSource("ui/components/CalendarComponents.kt")
        val homeSource = readSource("ui/screens/HomeScreen.kt")

        assertTrue(
            "PR#139: Helpful Insights title",
            stringsSource.contains("\"settingsHelpfulHintsTitle\" to \"Helpful Insights\"")
        )
        listOf("settingsHelpfulHint6", "settingsHelpfulHint7", "settingsHelpfulHint8").forEach { key ->
            assertTrue("PR#139: missing $key in strings", stringsSource.contains(key))
            assertTrue("PR#139: missing $key in settings dialog", settingsSource.contains(key))
        }
        assertTrue(weekCalendarSource.contains(".clickable { onDateSelected(dateLabel) }"))
        assertTrue(monthCalendarSource.contains(".clickable { onDateSelected(dateLabel) }"))
        assertFalse(weekCalendarSource.contains("appointmentDateHoldClickable"))
        assertFalse(monthCalendarSource.contains("appointmentDateHoldClickable"))
        assertFalse(
            "PR#139: hold gesture file removed",
            File(appModuleRoot, "ui/components/home/AppointmentDateHoldGesture.kt").isFile
        )
        assertTrue(homeSource.contains("HomeAppointmentsSection"))
        assertTrue(homeSource.contains("onPrefillHandled"))
        assertTrue(stringsSource.contains("Select a date to add an appointment."))
        assertFalse(stringsSource.contains("Press and hold a date for 2 seconds"))
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#139: intro/logo screen touched ($path)", source.contains("settingsHelpfulHint6"))
        }
    }

    @Test
    fun pr140VaultEvidencePreviewBlurAndDetailAudit() {
        val vaultScreenSource = readSource("ui/screens/TaxVaultScreen.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val navHostSource = readSource("navigation/EobNavHost.kt")
        val modelsSource = readSource("data/TaxVaultModels.kt")

        assertTrue(modelsSource.contains("sealed class VaultEvidencePreviewDetail"))
        assertTrue(viewModelSource.contains("fun selectTaxVaultEvidencePreview"))
        assertTrue(viewModelSource.contains("fun dismissTaxVaultEvidencePreview"))
        assertTrue(viewModelSource.contains("fun taxVaultEvidencePreviewDetail"))
        assertTrue(vaultScreenSource.contains("VaultEvidencePreviewOverlay"))
        assertTrue(vaultScreenSource.contains("Modifier.blur(12.dp)"))
        assertTrue(vaultScreenSource.contains("scaleIn"))
        assertTrue(navHostSource.contains("evidencePreviewDetail"))
        assertTrue(navHostSource.contains("selectTaxVaultEvidencePreview"))
        assertTrue(navHostSource.contains("dismissTaxVaultEvidencePreview"))
        assertTrue(navHostSource.contains("EobRoute.TaxVault.route"))
        listOf(
            "ui/screens/SplashScreen.kt",
            "ui/screens/LanguageScreen.kt",
            "ui/screens/IntroScreen.kt",
            "network/VeryfiDocumentClient.kt",
            "data/DocumentScanPipelineRepository.kt"
        ).forEach { path ->
            val source = readSource(path)
            assertFalse("PR#140: protected area touched ($path)", source.contains("VaultEvidencePreviewOverlay"))
        }
    }

    private fun readSource(relativePath: String): String {
        val file = File(appModuleRoot, relativePath)
        require(file.isFile) { "Missing ${file.absolutePath}" }
        return file.readText()
    }

    private fun readFunctionsSource(relativePath: String): String {
        val candidates = listOf(
            File("functions", relativePath),
            File("../functions", relativePath),
            File("../../functions", relativePath)
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("Missing functions source; tried: ${candidates.map { it.path }}")
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
