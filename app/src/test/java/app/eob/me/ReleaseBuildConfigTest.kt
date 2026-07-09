package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Locks release builds to production-grade non-shrinking configuration.
 */
class ReleaseBuildConfigTest {

    @Test
    fun releaseBuildTypeDisablesR8AndProGuard() {
        val buildScript = readAppBuildGradle()
        val releaseBlockStart = buildScript.indexOf("release {")
        val releaseBlockEnd = buildScript.indexOf("\n        }", releaseBlockStart)
        val releaseBlock = buildScript.substring(releaseBlockStart, releaseBlockEnd)

        assertTrue(releaseBlock.contains("isMinifyEnabled = false"))
        assertTrue(releaseBlock.contains("isShrinkResources = false"))
        assertFalse(releaseBlock.contains("isMinifyEnabled = true"))
        assertFalse(releaseBlock.contains("proguardFiles("))
    }

    @Test
    fun proGuardRulesFileIsNotCheckedIn() {
        val proguardFile = File("app/proguard-rules.pro")
        val parentProguardFile = File("../app/proguard-rules.pro")
        assertFalse(
            "proguard-rules.pro must not exist when R8 shrinking is disabled",
            proguardFile.isFile || parentProguardFile.isFile
        )
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
}
