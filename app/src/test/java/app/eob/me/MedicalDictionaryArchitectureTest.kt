package app.eob.me

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MedicalDictionaryArchitectureTest {
    @Test
    fun cleanInsuranceCardUsesSymmetricalQuickActionGrid() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("CardQuickActionButton"))
        assertTrue(source.contains("insuranceCardMedicalDictionaryLauncher"))
        assertTrue(source.contains("Arrangement.SpaceEvenly"))
        assertTrue(source.contains("onOpenMedicalDictionary"))
    }

    @Test
    fun eobViewModelOwnsMedicalDictionarySearchState() {
        val source = readSource("viewmodel/EobViewModel.kt")
        assertTrue(source.contains("medicalDictQuery"))
        assertTrue(source.contains("medicalDictResults"))
        assertTrue(source.contains("debounce(MEDICAL_DICT_SEARCH_DEBOUNCE_MS)"))
        assertTrue(source.contains("flatMapLatest"))
        assertTrue(source.contains("MedicalDictionaryRepository"))
    }

    @Test
    fun medicalDictionaryAssetAndGeneratorExist() {
        val dbFile = resolveProjectFile("app/src/main/assets/databases/medical_dictionary.db")
        val scriptFile = resolveProjectFile("scripts/generate_medical_db.py")
        assertTrue(dbFile.isFile)
        assertTrue(scriptFile.isFile)
        val proguard = readProjectFile("app/proguard-rules.pro")
        assertTrue(proguard.contains("-keep class app.eob.me.** { *; }"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readProjectFile(relativePath: String): String {
        return resolveProjectFile(relativePath).readText()
    }

    private fun resolveProjectFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("../$relativePath")
        )
        return candidates.first { it.isFile }
    }
}
