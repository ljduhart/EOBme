package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BuildGradlePr174Test {
    @Test
    fun appBuildGradleSyncsPr172And173ReleaseMetadata() {
        val buildScript = readAppBuildGradle()
        assertTrue(buildScript.contains("import org.gradle.api.tasks.testing.Test"))
        assertTrue(buildScript.contains("compileSdk = 36"))
        assertFalse(buildScript.contains("release(36)"))
        assertTrue(buildScript.contains("versionCode = 245"))
        assertTrue(buildScript.contains("versionName = \"25.4\""))
        assertTrue(buildScript.contains("targetSdk = 35"))
    }

    @Test
    fun appBuildGradlePreservesBillingFirebaseAndVerificationGuards() {
        val buildScript = readAppBuildGradle()
        assertTrue(buildScript.contains("resolutionStrategy.force"))
        assertTrue(buildScript.contains("com.android.billingclient:billing"))
        assertTrue(buildScript.contains("verifyGoogleServicesJson"))
        assertTrue(buildScript.contains("uploadCrashlyticsMappingFile"))
        assertTrue(buildScript.contains("libs.revenuecat.purchases"))
        assertTrue(buildScript.contains("isMinifyEnabled = false"))
    }

    private fun readAppBuildGradle(): String {
        val candidates = listOf(
            File("app/build.gradle.kts"),
            File("../app/build.gradle.kts")
        )
        return candidates.first { it.isFile }.readText()
    }
}
