package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr175Test {
    @Test
    fun homeMedicalProviderBackgroundAssetExists() {
        val drawable = homeBackgroundDrawable()
        assertTrue("home_medical_provider_background.png must exist", drawable.isFile)
        assertTrue("home_medical_provider_background.png must not be empty", drawable.length() > 0)
    }

    @Test
    fun homeProviderBackgroundUsesCropAndModeAwareScrims() {
        val backgroundSource = readSource("ui/components/home/HomeProviderBackground.kt")
        assertTrue(backgroundSource.contains("home_medical_provider_background"))
        assertTrue(backgroundSource.contains("ContentScale.Crop"))
        assertTrue(backgroundSource.contains("darkModeEnabled"))
        assertTrue(backgroundSource.contains("EobCyberBackground.copy(alpha = 0.74f)"))
        assertTrue(backgroundSource.contains("Color.White.copy(alpha = 0.70f)"))
        assertTrue(backgroundSource.contains("DarkHomeScrimGradient"))
        assertTrue(backgroundSource.contains("LightHomeScrimGradient"))
    }

    @Test
    fun homeScreenLayersProviderBackgroundWithoutChangingHubFeatures() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        assertTrue(homeSource.contains("HomeProviderBackground(darkModeEnabled = darkModeEnabled)"))
        assertFalse(homeSource.contains("DarkHomeBackground"))
        assertFalse(homeSource.contains("LightHomeBackground"))
        assertTrue(homeSource.contains("HomeCareTeamCards"))
        assertTrue(homeSource.contains("BentoGridCell"))
        assertTrue(homeSource.contains("HomeAppointmentsSection"))
        assertTrue(homeSource.contains("CleanInsuranceCard"))
    }

    @Test
    fun protectedAreasRemainUntouchedForHomeBackgroundUpdate() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(viewModelSource.contains("home_medical_provider_background"))
        assertFalse(splashSource.contains("HomeProviderBackground"))
        assertFalse(introSource.contains("HomeProviderBackground"))
        assertFalse(pipelineSource.contains("HomeProviderBackground"))
    }

    private fun homeBackgroundDrawable(): File {
        val candidates = listOf(
            File("src/main/res/drawable/home_medical_provider_background.png"),
            File("app/src/main/res/drawable/home_medical_provider_background.png")
        )
        return candidates.first { it.isFile }
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
