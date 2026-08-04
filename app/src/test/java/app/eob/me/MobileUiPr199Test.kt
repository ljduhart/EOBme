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

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
