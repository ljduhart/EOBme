package app.eob.me

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr200Test {
    @Test
    fun clinicalNoteRoomEntityAndDaoPresent() {
        val entitySource = readSource("data/local/entity/ClinicalNote.kt")
        val daoSource = readSource("data/local/dao/ClinicalNoteDao.kt")
        val dbSource = readSource("data/local/EobmeRoomDatabase.kt")
        assertTrue(entitySource.contains("data class ClinicalNote"))
        assertTrue(entitySource.contains("providerId"))
        assertTrue(daoSource.contains("getNotesForProvider"))
        assertTrue(dbSource.contains("MIGRATION_1_2"))
        assertTrue(dbSource.contains("provider_directory"))
    }

    @Test
    fun speechRecognizerManagerUsesOfflineNativeApi() {
        val source = readSource("speech/SpeechRecognizerManager.kt")
        assertTrue(source.contains("SpeechRecognizer.createSpeechRecognizer"))
        assertTrue(source.contains("RecognitionListener"))
        assertTrue(source.contains("EXTRA_PREFER_OFFLINE"))
        assertTrue(source.contains("StateFlow"))
        assertTrue(source.contains("destroy()"))
    }

    @Test
    fun clinicalNotesBottomSheetAndViewModelWiredFromNotepadQuickAction() {
        val sheetSource = readSource("ui/components/clinical/ClinicalNotesBottomSheet.kt")
        val vmSource = readSource("viewmodel/ClinicalNotesViewModel.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(sheetSource.contains("ModalBottomSheet"))
        assertTrue(sheetSource.contains("ExposedDropdownMenuBox"))
        assertTrue(vmSource.contains("ClinicalNotesRepository"))
        assertTrue(navSource.contains("ClinicalNotesSessionOverlay"))
        assertTrue(navSource.contains("clinicalNotesEngaged"))
        assertTrue(cardSource.contains("onOpenClinicalNotes"))
        assertTrue(cardSource.contains("onClick = onOpenClinicalNotes"))
        assertFalse(cardSource.contains("InsuranceCardDigitalNotepadPanel"))
        assertFalse(cardSource.contains("InsuranceCardBackMode.Notepad"))
        val hubBlock = cardSource.substringAfter("private fun InsuranceCardBackHub")
            .substringBefore("private fun InsuranceCardBackLauncher")
        assertEquals(2, hubBlock.split("InsuranceCardBackLauncher").size - 1)
    }

    @Test
    fun openingClinicalNotesOrRxVaultMutuallyExcludesOtherSheet() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val openVault = navSource.substringAfter("onOpenSmartRxVault = {")
            .substringBefore("onOpenClinicalNotes = {")
        val openNotes = navSource.substringAfter("onOpenClinicalNotes = {")
            .substringBefore("blockInsuranceCardBackNavigation")
        assertTrue(openVault.contains("clinicalNotesVisible = false"))
        assertTrue(openNotes.contains("smartRxVaultVisible = false"))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr200() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("ClinicalNote"))
        assertFalse(pipelineSource.contains("SpeechRecognizerManager"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
