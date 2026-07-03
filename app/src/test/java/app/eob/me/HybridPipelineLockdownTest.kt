package app.eob.me

import app.eob.me.network.VeryfiAnyDocConstants
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Enforces the immutable hybrid pipeline charter in [hybrid-pipeline/HYBRID_PIPELINE_MANIFEST.json].
 * Protected pipeline and OCR files must not change without explicit authorization and manifest update.
 */
class HybridPipelineLockdownTest {

    private data class ProtectedFileEntry(
        val path: String,
        val sha256: String,
        val tier: String
    )

    private data class PipelineManifest(
        val protectedFiles: List<ProtectedFileEntry>
    )

    @Test
    fun protectedPipelineFilesMatchManifestSha256() {
        val manifest = loadManifest()
        manifest.protectedFiles.forEach { entry ->
            val file = resolveRepoFile(entry.path)
            assertTrue("Protected file missing: ${entry.path}", file.isFile)
            assertEquals(
                "Protected file modified without manifest update: ${entry.path}\n" +
                    "Update hybrid-pipeline/HYBRID_PIPELINE_MANIFEST.json only with explicit owner authorization.",
                entry.sha256,
                sha256(file)
            )
        }
    }

    @Test
    fun zeroClientSideVeryfiCredentialsBarrier() {
        val androidMainRoot = resolveRepoFile("app/src/main/java/app/eob/me")
        val forbidden = listOf(
            "VERYFI_API_KEY",
            "VERYFI_CLIENT_ID",
            "VERYFI_USERNAME",
            "apikey ",
            "Authorization: apikey"
        )
        androidMainRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val source = file.readText()
                forbidden.forEach { token ->
                    assertFalse(
                        "Client-side Veryfi credential leak in ${file.relativeTo(resolveRepoRoot())}: $token",
                        source.contains(token)
                    )
                }
            }
    }

    @Test
    fun base64HybridStreamContractIsIntact() {
        val veryfiClient = readAppSource("network/VeryfiDocumentClient.kt")
        val pipeline = readAppSource("data/DocumentScanPipelineRepository.kt")
        val viewModel = readAppSource("viewmodel/EobViewModel.kt")

        assertTrue(veryfiClient.contains("Base64.encodeToString(fileBytes, Base64.NO_WRAP)"))
        assertTrue(veryfiClient.contains("\"fileBase64\" to encoded"))
        assertTrue(veryfiClient.contains("EXTRACT_VERYFI_HYBRID_STREAM"))
        assertTrue(pipeline.contains("processHybridDocument"))
        assertTrue(pipeline.contains("extractionDeferred"))
        assertTrue(pipeline.contains("uploadDeferred"))
        assertTrue(viewModel.contains("processHybridScannedDocument"))
        assertFalse(
            "Live stream must not depend on Storage download URL for Veryfi extraction",
            veryfiClient.contains("downloadUrl") && veryfiClient.contains("streamExtractDocument") &&
                veryfiClient.indexOf("fileUrl") > veryfiClient.indexOf("streamExtractDocument")
        )
    }

    @Test
    fun veryfiMapperDefaultsAndSanitizationBarrier() {
        val veryfiClient = readAppSource("network/VeryfiDocumentClient.kt")
        val firebaseMapper = readAppSource("data/FirebaseEobMapper.kt")

        assertTrue(veryfiClient.contains("fun sanitizeForFirestore"))
        assertTrue(veryfiClient.contains("veryfiClientStream\" to sanitizeForFirestore(rawPayload)"))
        assertTrue(veryfiClient.contains("private fun Map<String, Any?>.veryfiNumberField"))
        assertTrue(veryfiClient.contains("return 0.0"))
        assertTrue(veryfiClient.contains("private fun Map<String, Any?>.veryfiStringField"))
        assertTrue(veryfiClient.contains("return \"\""))
        assertTrue(firebaseMapper.contains("hybridDocumentRefId"))
    }

    @Test
    fun anyDocsConstantsAndBlueprintLocked() {
        val constants = readAppSource("network/VeryfiAnyDocConstants.kt")
        val functionsConstants = readFunctionsSource("lib/veryfiAnyDocConstants.js")
        val anyDocClient = readFunctionsSource("lib/veryfiAnyDocClient.js")

        assertEquals("health_insurance_eob", VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB)
        assertEquals("partner/any-documents/", VeryfiAnyDocConstants.ANY_DOCUMENTS_PATH)
        assertEquals("extractVeryfiHybridStream", VeryfiAnyDocConstants.EXTRACT_VERYFI_HYBRID_STREAM)
        assertTrue(functionsConstants.contains("health_insurance_eob"))
        assertTrue(functionsConstants.contains("partner/any-documents/"))
        assertTrue(anyDocClient.contains("form.append(\"blueprint_name\""))
        assertFalse(anyDocClient.contains("form.append(\"document_type\""))
        assertFalse(anyDocClient.contains("form.append(\"categories\""))
    }

    @Test
    fun ocrPreCheckFilesAreManifestProtected() {
        val ocrPaths = loadManifest().protectedFiles
            .filter { it.tier == "ocr" }
            .map { it.path }
        assertTrue(ocrPaths.contains("app/src/main/java/app/eob/me/util/OcrProcessor.kt"))
        assertTrue(ocrPaths.contains("app/src/main/java/app/eob/me/util/EobDocumentOcrPreCheck.kt"))
        assertTrue(readAppSource("data/DocumentScanPipelineRepository.kt").contains("runOcrPreCheck"))
        assertTrue(readAppSource("viewmodel/EobViewModel.kt").contains("runDocumentOcrPreCheck"))
    }

    @Test
    fun mathematicalUiSafetyPatternsPresent() {
        val bentoModels = readAppSource("data/EobModels.kt")
        val bentoExtractor = readAppSource("data/BentoSnapshotExtractor.kt")
        val historyBento = readAppSource("ui/components/bento/HistoryBentoCell.kt")

        assertTrue(bentoModels.contains("coerceAtLeast(1.0)"))
        assertTrue(bentoExtractor.contains("coerceAtLeast(1.0)"))
        assertTrue(historyBento.contains("maxValue"))
    }

    private fun loadManifest(): PipelineManifest {
        val manifestFile = resolveRepoFile("hybrid-pipeline/HYBRID_PIPELINE_MANIFEST.json")
        assertTrue("Missing hybrid pipeline manifest", manifestFile.isFile)
        return Gson().fromJson(manifestFile.readText(), PipelineManifest::class.java)
    }

    private fun readAppSource(relativePath: String): String {
        return resolveRepoFile("app/src/main/java/app/eob/me/$relativePath").readText()
    }

    private fun readFunctionsSource(relativePath: String): String {
        return resolveRepoFile("functions/$relativePath").readText()
    }

    private fun resolveRepoRoot(): File {
        var current = File(System.getProperty("user.dir"))
        repeat(6) {
            if (File(current, "hybrid-pipeline/HYBRID_PIPELINE_MANIFEST.json").isFile) {
                return current
            }
            current = current.parentFile ?: return current
        }
        return File(System.getProperty("user.dir"))
    }

    private fun resolveRepoFile(relativePath: String): File {
        return File(resolveRepoRoot(), relativePath)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(file.readBytes())
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }
}
