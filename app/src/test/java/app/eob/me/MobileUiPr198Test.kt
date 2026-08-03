package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr198Test {
    @Test
    fun smartRxVaultUsesRoomMedicationEntityAndTaxVaultLedger() {
        val repoSource = readSource("data/repository/RxVaultRepository.kt")
        val entitySource = readSource("data/local/entity/MedicationRecord.kt")
        val ledgerSource = readSource("data/local/entity/TaxVaultExpenseLedgerEntity.kt")
        assertTrue(entitySource.contains("data class MedicationRecord"))
        assertTrue(ledgerSource.contains("tax_vault_expense_ledger"))
        assertTrue(repoSource.contains("bridgeCopayToTaxVaultLedger"))
        assertTrue(repoSource.contains("record.copayAmount <= 0.0"))
        assertTrue(repoSource.contains("!record.isFsaEligible"))
    }

    @Test
    fun workManagerSchedulesPillAndRefillWorkers() {
        val schedulerSource = readSource("work/RxReminderScheduler.kt")
        val pillSource = readSource("work/PillReminderWorker.kt")
        val refillSource = readSource("work/RefillAlertWorker.kt")
        assertTrue(schedulerSource.contains("PeriodicWorkRequestBuilder<PillReminderWorker>"))
        assertTrue(schedulerSource.contains("OneTimeWorkRequestBuilder<RefillAlertWorker>"))
        assertTrue(pillSource.contains("CoroutineWorker"))
        assertTrue(refillSource.contains("CoroutineWorker"))
    }

    @Test
    fun composeUiUsesRxVaultViewModelAndBottomSheet() {
        val sheetSource = readSource("ui/components/rx/SmartRxVaultBottomSheet.kt")
        val vmSource = readSource("viewmodel/RxVaultViewModel.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(sheetSource.contains("ModalBottomSheet"))
        assertTrue(sheetSource.contains("LazyRow"))
        assertTrue(sheetSource.contains("LazyColumn"))
        assertTrue(vmSource.contains("StateFlow"))
        assertTrue(navSource.contains("SmartRxVaultSessionOverlay"))
        assertTrue(navSource.contains("RxVaultViewModel"))
    }

    @Test
    fun rxVaultOnlyEngagesAfterInsuranceCardPillSelection() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("var rxVaultEngaged"))
        assertTrue(navSource.contains("if (rxVaultEngaged)"))
        assertTrue(navSource.contains("rxVaultEngaged = true"))
    }

    @Test
    fun smartRxVaultOriginatesFromInsuranceCardPillBottleQuickAccess() {
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(cardSource.contains("InsuranceCardPillBottleIcon"))
        assertTrue(cardSource.contains("onClick = onOpenSmartRxVault"))
        assertTrue(navSource.contains("onOpenSmartRxVault"))
        assertFalse(navSource.contains("EobRoute.RxVault"))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr198() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("MedicationRecord"))
        assertFalse(pipelineSource.contains("RxVaultRepository"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
