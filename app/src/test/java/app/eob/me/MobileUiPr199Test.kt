package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr199Test {
    @Test
    fun insuranceCardQuickAccessShowsOnlyPillAndNotepadIcons() {
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(cardSource.contains("InsuranceCardPillBottleIcon"))
        assertTrue(cardSource.contains("InsuranceCardNotepadIcon"))
        assertFalse(cardSource.contains("insuranceCardMedicationsNotesLink"))
        assertFalse(cardSource.contains("TextButton(onClick = onOpenMedications)"))
    }

    @Test
    fun smartRxVaultSheetScrollsWithKeyboardPaddingForAddForm() {
        val sheetSource = readSource("ui/components/rx/SmartRxVaultBottomSheet.kt")
        assertTrue(sheetSource.contains("imePadding()"))
        assertTrue(sheetSource.contains("rememberLazyListState"))
        assertTrue(sheetSource.contains("animateScrollToItem"))
        assertFalse(sheetSource.contains(".height(320.dp)"))
    }

    @Test
    fun insuranceCardBackDeferredWhileSmartRxVaultVisible() {
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(cardSource.contains("blockInsuranceCardBackNavigation"))
        assertTrue(cardSource.contains("flipped && !blockInsuranceCardBackNavigation"))
        assertTrue(homeSource.contains("blockInsuranceCardBackNavigation"))
        assertTrue(navSource.contains("blockInsuranceCardBackNavigation = smartRxVaultVisible ||"))
        assertTrue(navSource.contains("reverseDxLookupVisible"))
    }

    @Test
    fun smartRxVaultDismissResetsAddFormState() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("closeVault"))
        assertTrue(navSource.contains("setShowAddForm(false)"))
    }

    @Test
    fun pr199PreservesLazyRxVaultEngagementAndProtectedPipeline() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertTrue(navSource.contains("if (rxVaultEngaged)"))
        assertTrue(navSource.contains("BackHandler(enabled = smartRxVaultVisible)"))
        assertFalse(pipelineSource.contains("RxVaultRepository"))
        assertFalse(pipelineSource.contains("MedicationRecord"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
