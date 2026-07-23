package app.eob.me

import app.eob.me.data.SubscriptionTier
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr182Test {
    @Test
    fun accountProfileUiStateIsExposedFromEobViewModel() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(viewModelSource.contains("val accountProfileUiState: StateFlow<AccountProfileUiState>"))
        assertTrue(viewModelSource.contains("fun syncAccountProfileSource"))
        assertTrue(viewModelSource.contains("fun profileInitials"))
        assertTrue(viewModelSource.contains("fun updateSettingsAccountDraftFirstName"))
        assertTrue(viewModelSource.contains("fun updateSettingsAccountDraftLastName"))
    }

    @Test
    fun accountProfileSettingsUsesBentoCardsAndTopAppBar() {
        val contentSource = readSource("ui/screens/AccountProfileSettingsContent.kt")
        val settingsSource = readSource("ui/screens/SettingsScreen.kt")
        val stringsSource = readFile("app/src/main/res/values/strings.xml")
        assertTrue(contentSource.contains("CenterAlignedTopAppBar"))
        assertTrue(contentSource.contains("PrimaryScrollableTabRow"))
        assertTrue(contentSource.contains("ProfileSettingsBentoCard"))
        assertTrue(contentSource.contains("SubscriptionBentoCard"))
        assertTrue(contentSource.contains("AccountActionsBentoCard"))
        assertTrue(contentSource.contains("ElevatedCard"))
        assertTrue(contentSource.contains("FilledTonalButton"))
        assertTrue(settingsSource.contains("AccountProfileSettingsScaffold"))
        assertTrue(stringsSource.contains("account_profile_manage_subscription"))
        assertTrue(stringsSource.contains("manage billing through Google Play"))
        assertFalse(stringsSource.contains("App Store"))
        assertFalse(stringsSource.contains("iOS"))
    }

    @Test
    fun subscriptionTierColorsAreMappedForFreeSilverAndGold() {
        val iconSource = readSource("ui/components/SubscriptionTierIcon.kt")
        assertTrue(iconSource.contains("SubscriptionTier.Free"))
        assertTrue(iconSource.contains("SubscriptionTier.Silver"))
        assertTrue(iconSource.contains("SubscriptionTier.Gold"))
        assertTrue(iconSource.contains("EobSubscriptionGold"))
    }

    @Test
    fun settingsRouteHidesHubHeaderAndUsesAccountProfileState() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("currentRoute != EobRoute.Settings.route"))
        assertTrue(navSource.contains("accountProfileUiState"))
        assertTrue(navSource.contains("syncAccountProfileSource"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr182() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("AccountProfileUiState"))
        assertFalse(veryfiSource.contains("ProfileSettingsBentoCard"))
        assertFalse(splashSource.contains("account_profile_section_title"))
        assertFalse(introSource.contains("SubscriptionBentoCard"))
    }

    @Test
    fun viewModelBuildsProfileInitialsFromName() {
        val viewModel = EobViewModel()
        assertEquals("JD", viewModel.profileInitials("Jermaine", "Duhart"))
        assertEquals("J", viewModel.profileInitials("Jermaine", ""))
        assertEquals("?", viewModel.profileInitials("", ""))
    }

    @Test
    fun subscriptionTierEnumIncludesFreeSilverAndGold() {
        assertEquals(3, SubscriptionTier.entries.size)
        assertTrue(SubscriptionTier.entries.contains(SubscriptionTier.Free))
        assertTrue(SubscriptionTier.entries.contains(SubscriptionTier.Silver))
        assertTrue(SubscriptionTier.entries.contains(SubscriptionTier.Gold))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readFile(path: String): String {
        val candidates = listOf(File(path), File("../$path"))
        return candidates.first { it.isFile }.readText()
    }
}
