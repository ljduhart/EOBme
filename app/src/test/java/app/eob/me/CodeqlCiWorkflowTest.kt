package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards CI workflows and Gradle release behavior so GitHub CodeQL default autobuild succeeds.
 */
class CodeqlCiWorkflowTest {

    @Test
    fun androidCiWorkflowUsesManualAndroidBuildSteps() {
        val workflow = readWorkflow(".github/workflows/android-ci.yml")
        listOf(
            "actions/setup-java@v4",
            "java-version: '17'",
            "distribution: 'temurin'",
            "cache: gradle",
            "android-actions/setup-android",
            "platforms;android-36",
            "chmod +x ./gradlew",
            "assembleDebug --no-daemon"
        ).forEach { snippet ->
            assertTrue(
                "android-ci.yml must contain: $snippet",
                workflow.contains(snippet)
            )
        }
    }

    @Test
    fun advancedCodeqlWorkflowIsNotCheckedInWithDefaultSetup() {
        val workflow = File(".github/workflows/codeql-analysis.yml")
        val parentWorkflow = File("../.github/workflows/codeql-analysis.yml")
        assertFalse(
            "codeql-analysis.yml conflicts with GitHub default CodeQL setup; use android-ci + Gradle CI guards instead",
            workflow.isFile || parentWorkflow.isFile
        )
    }

    @Test
    fun buildGradleDisablesCrashlyticsMappingUploadOnCi() {
        val buildScript = readAppBuildGradle()
        assertTrue(buildScript.contains("uploadCrashlyticsMappingFile"))
        assertTrue(buildScript.contains("GITHUB_ACTIONS"))
        assertTrue(buildScript.contains("isCiEnvironment"))
    }

    private fun readWorkflow(relativePath: String): String {
        val candidates = listOf(
            File(relativePath),
            File("../$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readAppBuildGradle(): String {
        val candidates = listOf(
            File("app/build.gradle.kts"),
            File("../app/build.gradle.kts")
        )
        return candidates.first { it.isFile }.readText()
    }
}
