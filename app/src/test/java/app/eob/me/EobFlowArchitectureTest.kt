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
        val appViewModelSource = readSource("viewmodel/AppViewModel.kt")
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
        assertTrue(
            "Auth toggle should switch sign-up mode in place",
            appViewModelSource.contains("true -> false") &&
                appViewModelSource.contains("false -> true")
        )
        assertTrue(
            "Outer nav should guard concurrent navigation",
            navHostSource.contains("runCatching")
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
        }
        assertTrue(File(appModuleRoot, "ui/components/bento/HistoryBentoCell.kt").isFile)
        assertTrue(File(appModuleRoot, "ui/components/bento/ProviderDirectoryBentoCell.kt").isFile)
        assertTrue(File(appModuleRoot, "ui/components/bento/BentoGridCell.kt").isFile)
        assertEquals(6, HubBentoDestination.entries.size)
        assertEquals(2, HubBentoDestination.gridRows.size)
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
            "acknowledgeInvoiceFileDropAnimation"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing upload/camera wiring: $snippet", navHostSource.contains(snippet))
        }
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
            "preferredDoctors",
            "HomeCareTeamCards",
            "HomeWeekCalendar",
            "onAddAppointment",
            "onUpdateAppointment",
            "BentoGridCell"
        ).forEach { snippet ->
            assertTrue("Home flow missing: $snippet", homeSource.contains(snippet))
        }
    }

    @Test
    fun hubBottomBarScanOpensCameraPermissionPath() {
        assertEquals(HubBottomTab.ScanEob, HubBottomTab.entries[1])
        assertTrue(navHostSource.contains("Manifest.permission.CAMERA"))
        assertTrue(EobRoute.CameraCapture.route in hubRoutesWithoutBottomBar)
        assertTrue(EobRoute.Home.route !in hubBackRoutes)
        assertTrue(hubFeatureRoutes.contains(EobRoute.History.route))
    }

    @Test
    fun pasteAndDeleteFlowsExistOnViewModel() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        listOf("savePastedEob", "deleteRecord", "deleteRecordRemote", "uploadEobFile", "uploadEobBitmap")
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
    }

    @Test
    fun viewModelIsDocumentedAsHubSourceOfTruth() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(viewModelSource.contains("Single source of truth"))
        listOf(
            "HubUiState",
            "invoiceProcessingPhase",
            "historyBentoFilter",
            "preferredDoctors",
            "applyRemoteRecords",
            "selectedCptCategory",
            "ytdBentoViewMode",
            "firebaseSyncStatus",
            "cptBentoSnapshot",
            "ytdDeductibleBentoSnapshot",
            "historyBentoSnapshot",
            "providerAvatarPreviews",
            "providerDirectory",
            "yearlyHealthCostSummary",
            "historyRecordsForDisplay",
            "currentNewsReleases",
            "setSelectedCptCategory",
            "setYtdBentoViewMode"
        ).forEach { snippet ->
            assertTrue("EobViewModel missing: $snippet", viewModelSource.contains(snippet))
        }
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
            "eobViewModel.currentNewsReleases",
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
