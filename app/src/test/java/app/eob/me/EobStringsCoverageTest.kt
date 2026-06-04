package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.navigation.EobRoute
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.navigation.HubBottomTab
import app.eob.me.navigation.Screen
import app.eob.me.navigation.hubBackRoutes
import app.eob.me.navigation.hubFeatureRoutes
import app.eob.me.navigation.hubRoutesWithoutBottomBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EobStringsCoverageTest {
    @Test
    fun allReferencedKeysExistInEnglishDictionary() {
        val sourceRoot = File("src/main/java/app/eob/me")
        require(sourceRoot.isDirectory) { "Run from :app module; missing ${sourceRoot.absolutePath}" }

        val keyRegex = Regex("""EobStrings\.t\([^,]+,\s*"([^"]+)"\)""")
        val tfKeyRegex = Regex("""EobStrings\.tf\([^,]+,\s*"([^"]+)"\)""")
        val firebaseKeyRegex = Regex("""t\(language,\s*"([^"]+)"\)""")

        val referenced = linkedSetOf<String>()
        sourceRoot.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                keyRegex.findAll(text).forEach { referenced += it.groupValues[1] }
                tfKeyRegex.findAll(text).forEach { referenced += it.groupValues[1] }
                if (file.name == "EobStrings.kt") {
                    firebaseKeyRegex.findAll(text).forEach { referenced += it.groupValues[1] }
                }
            }

        val missing = referenced.filter { it !in EobStrings.allEnglishKeys }.sorted()
        assertFalse("Missing EobStrings English keys: $missing", missing.isNotEmpty())
    }

    @Test
    fun bentoAndBottomBarTitleKeysExist() {
        val keys = EobStrings.allEnglishKeys
        HubBentoDestination.entries.forEach { destination ->
            assertTrue(
                "Missing bento titleKey ${destination.titleKey}",
                destination.titleKey in keys
            )
        }
        HubBottomTab.entries.forEach { tab ->
            assertTrue(
                "Missing bottom tab labelKey ${tab.labelKey}",
                tab.labelKey in keys
            )
        }
    }

    @Test
    fun hubBentoRoutesMatchEobRouteDefinitions() {
        val registeredRoutes = setOf(
            EobRoute.Home.route,
            EobRoute.History.route,
            EobRoute.Dashboard.route,
            EobRoute.YearlyExpense.route,
            EobRoute.CptCount.route,
            EobRoute.News.route,
            EobRoute.Appeal.route,
            EobRoute.Profile.route,
            EobRoute.CameraCapture.route,
            EobRoute.ProviderDirectory.route
        )
        HubBentoDestination.entries.forEach { destination ->
            assertTrue(
                "Bento route ${destination.route} is not defined in EobRoute",
                destination.route in registeredRoutes
            )
        }
    }

    @Test
    fun mainHubNavHostRegistersAllEobRoutes() {
        val navHostSource = File("src/main/java/app/eob/me/navigation/EobNavHost.kt").readText()
        val routeNames = listOf(
            "Home", "History", "Dashboard", "YearlyExpense", "CptCount",
            "News", "Appeal", "Profile", "CameraCapture", "ProviderDirectory"
        )
        routeNames.forEach { name ->
            assertTrue(
                "EobNavHost missing composable for EobRoute.$name",
                navHostSource.contains("composable(EobRoute.$name.route)")
            )
        }
    }

    @Test
    fun outerNavHostRegistersAllScreenRoutes() {
        val navHostSource = File("src/main/java/app/eob/me/navigation/EobNavHost.kt").readText()
        val screenNames = listOf("Splash", "Language", "Intro", "AuthChoice", "Auth", "MainHub")
        screenNames.forEach { name ->
            assertTrue(
                "EobNavHost missing composable for Screen.$name",
                navHostSource.contains("composable(Screen.$name.route)")
            )
        }
    }

    @Test
    fun firebaseStatusKeysResolveForEveryLanguage() {
        AppLanguage.entries.forEach { language ->
            listOf("firebaseNotConfigured", "firebaseActive", "firebaseConfigured").forEach { key ->
                val value = EobStrings.t(language, key)
                assertNotEquals(key, value)
            }
        }
    }

    @Test
    fun hubNavigationRouteSetsAreConsistent() {
        val registeredRoutes = setOf(
            EobRoute.Home.route,
            EobRoute.History.route,
            EobRoute.Dashboard.route,
            EobRoute.YearlyExpense.route,
            EobRoute.CptCount.route,
            EobRoute.News.route,
            EobRoute.Appeal.route,
            EobRoute.Profile.route,
            EobRoute.CameraCapture.route,
            EobRoute.ProviderDirectory.route
        )
        assertTrue(hubFeatureRoutes.all { it in registeredRoutes })
        assertTrue(hubBackRoutes.all { it in registeredRoutes })
        assertTrue(hubRoutesWithoutBottomBar.all { it in registeredRoutes })
        assertTrue(EobRoute.Home.route !in hubBackRoutes)
        assertTrue(EobRoute.CameraCapture.route in hubRoutesWithoutBottomBar)
    }

    @Test
    fun hubBottomBarIncludesCenterScanEobTab() {
        val tabs = HubBottomTab.entries
        assertEquals(3, tabs.size)
        assertEquals(HubBottomTab.ScanEob, tabs[1])
        assertEquals(null, HubBottomTab.ScanEob.route)
        assertTrue(HubBottomTab.ScanEob.labelKey in EobStrings.allEnglishKeys)
    }

    @Test
    fun homeRouteWiresCareTeamAndAppointmentsThroughEobViewModel() {
        val navHostSource = File("src/main/java/app/eob/me/navigation/EobNavHost.kt").readText()
        val homeScreenSource = File("src/main/java/app/eob/me/ui/screens/HomeScreen.kt").readText()
        assertTrue(
            "HomeScreen must pass preferredDoctors and onSavePreferredDoctor",
            homeScreenSource.contains("preferredDoctors = preferredDoctors") &&
                homeScreenSource.contains("onSaveDoctor = onSavePreferredDoctor")
        )
        listOf(
            "preferredDoctors = uiState.preferredDoctors",
            "updatePreferredDoctor",
            "eobViewModel.addAppointment",
            "eobViewModel.updateAppointment",
            "providerType",
            "onSavePreferredDoctor"
        ).forEach { snippet ->
            assertTrue(
                "EobNavHost missing care team / appointment wiring: $snippet",
                navHostSource.contains(snippet)
            )
        }
    }

    @Test
    fun careTeamKeysResolveForEveryLanguage() {
        val keys = listOf(
            "careTeamPcp",
            "careTeamDentist",
            "careTeamSpecialist",
            "careTeamTherapist",
            "careTeamTapToEdit",
            "careTeamTapToFlip",
            "careTeamLongPressEdit",
            "careTeamEditTitle",
            "careTeamDoctorName",
            "careTeamSpecialty",
            "careTeamAddress",
            "careTeamPhone",
            "careTeamSaveDoctor",
            "selectAppointmentDate"
        )
        AppLanguage.entries.forEach { language ->
            keys.forEach { key ->
                assertNotEquals(key, EobStrings.t(language, key))
            }
            val title = EobStrings.tf(language, "careTeamEditTitle", "PCP")
            assertTrue(title.isNotBlank())
        }
    }

    @Test
    fun providerAndYearlyChartKeysResolveForEveryLanguage() {
        val keys = listOf(
            "yearlyExpenseChartTitle",
            "yearlyExpenseChartEmpty",
            "providerShowEobs",
            "providerHideEobs"
        )
        AppLanguage.entries.forEach { language ->
            keys.forEach { key ->
                val value = EobStrings.t(language, key)
                assertNotEquals(key, value)
            }
            val formatted = EobStrings.tf(language, "providerShowEobs", 3)
            assertTrue(formatted.contains("3"))
        }
    }

    @Test
    fun appointmentAndCameraKeysResolveForEveryLanguage() {
        val keys = listOf(
            "editAppointment",
            "updateAppointment",
            "saveAppointment",
            "addAppointment",
            "cameraOpenFailed",
            "cameraStarting",
            "cameraCaptureFailed",
            "contractualAdjustment"
        )
        AppLanguage.entries.forEach { language ->
            keys.forEach { key ->
                val value = EobStrings.t(language, key)
                assertNotEquals(key, value)
            }
        }
    }

    @Test
    fun homeScreenPassesBentoHubStateFromViewModel() {
        val navHostSource = File("src/main/java/app/eob/me/navigation/EobNavHost.kt").readText()
        listOf(
            "historySnapshot = historySnapshot",
            "processingPhase = uiState.invoiceProcessingPhase",
            "providerAvatars = providerAvatars",
            "setHistoryBentoFilter",
            "acknowledgeInvoiceFileDropAnimation"
        ).forEach { snippet ->
            assertTrue("EobNavHost missing bento hub wiring: $snippet", navHostSource.contains(snippet))
        }
    }

    @Test
    fun hubUiStateExposesCareTeamDefaultsFromEobViewModel() {
        val viewModelSource = File("src/main/java/app/eob/me/viewmodel/EobViewModel.kt").readText()
        listOf(
            "preferredDoctors",
            "updatePreferredDoctor",
            "addAppointment",
            "updateAppointment",
            "providerType: CareTeamProviderType",
            "fun addAppointment"
        ).forEach { snippet ->
            assertTrue(
                "EobViewModel missing hub care team API: $snippet",
                viewModelSource.contains(snippet)
            )
        }
    }

    @Test
    fun repositoryMessagesLocalizeWithoutReturningRawKeys() {
        val sample = "EOB uploaded. Veryfi processing started."
        AppLanguage.entries.forEach { language ->
            val localized = EobStrings.localizeRepositoryMessage(language, sample)
            assertTrue(localized.isNotBlank())
            assertFalse("Returned dictionary key instead of text", localized == "eobUploadVeryfiStarted")
        }
    }
}
