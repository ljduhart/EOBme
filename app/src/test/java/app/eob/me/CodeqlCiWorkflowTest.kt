package app.eob.me

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Ensures GitHub CodeQL uses manual Android build steps instead of failing autobuild.
 */
class CodeqlCiWorkflowTest {

    @Test
    fun codeqlWorkflowUsesManualBuildWithAndroidSdk() {
        val workflow = readWorkflow(".github/workflows/codeql-analysis.yml")
        listOf(
            "build-mode: manual",
            "java-kotlin",
            "android-actions/setup-android",
            "platforms;android-36",
            "--no-daemon",
            "--no-build-cache",
            "assembleDebug"
        ).forEach { snippet ->
            assertTrue(
                "codeql-analysis.yml must contain: $snippet",
                workflow.contains(snippet)
            )
        }
    }

    @Test
    fun androidCiWorkflowInstallsAndroidSdk() {
        val workflow = readWorkflow(".github/workflows/android-ci.yml")
        assertTrue(workflow.contains("android-actions/setup-android"))
        assertTrue(workflow.contains("platforms;android-36"))
        assertTrue(workflow.contains("assembleDebug"))
    }

    private fun readWorkflow(relativePath: String): String {
        val candidates = listOf(
            File(relativePath),
            File("../$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
