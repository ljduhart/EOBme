package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Locks release builds to the minimal R8 shrinking configuration.
 */
class ReleaseBuildConfigTest {

    @Test
    fun releaseBuildTypeEnablesR8AndProGuard() {
        val buildScript = readAppBuildGradle()
        val releaseBlockStart = buildScript.indexOf("release {")
        val releaseBlockEnd = buildScript.indexOf("\n        }", releaseBlockStart)
        val releaseBlock = buildScript.substring(releaseBlockStart, releaseBlockEnd)

        assertTrue(releaseBlock.contains("isMinifyEnabled = true"))
        assertTrue(releaseBlock.contains("isShrinkResources = true"))
        assertTrue(releaseBlock.contains("proguardFiles("))
        assertTrue(releaseBlock.contains("proguard-rules.pro"))
    }

    @Test
    fun proGuardRulesFileProtectsEntireApplicationPackage() {
        val proguardFile = resolveProguardFile()
        val rules = proguardFile.readText()
        assertTrue(rules.contains("-keep class app.eob.me.** { *; }"))
        assertTrue(rules.contains("-dontwarn okhttp3.**"))
        assertTrue(rules.contains("-dontwarn retrofit2.**"))
        assertTrue(rules.contains("-dontwarn coil.**"))
    }

    @Test
    fun r8CursorRuleIsNotCheckedIn() {
        val ruleFile = File(".cursor/rules/r8-firebase-veryfi.mdc")
        val parentRuleFile = File("../.cursor/rules/r8-firebase-veryfi.mdc")
        assertFalse(
            "R8 cursor rule must not exist when shrinking is permanently disabled",
            ruleFile.isFile || parentRuleFile.isFile
        )
    }

    private fun readAppBuildGradle(): String {
        val candidates = listOf(
            File("app/build.gradle.kts"),
            File("../app/build.gradle.kts")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun resolveProguardFile(): File {
        val candidates = listOf(
            File("app/proguard-rules.pro"),
            File("../app/proguard-rules.pro")
        )
        return candidates.first { it.isFile }
    }
}
