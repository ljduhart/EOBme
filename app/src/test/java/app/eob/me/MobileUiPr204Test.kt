package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.InsuranceCardPremiumFeature
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr204Test {
    @Test
    fun insuranceCardBackFeaturesAreTierGatedInNavHost() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val openVault = navSource.substringAfter("onOpenSmartRxVault = {")
            .substringBefore("onOpenClinicalNotes = {")
        val openNotes = navSource.substringAfter("onOpenClinicalNotes = {")
            .substringBefore("onOpenReverseDxLookup = {")
        val openReverseDx = navSource.substringAfter("onOpenReverseDxLookup = {")
            .substringBefore("blockInsuranceCardBackNavigation")

        assertTrue(openVault.contains("hasMedicationListReminder"))
        assertTrue(openVault.contains("paywallMessageForInsuranceCardFeature"))
        assertTrue(openVault.contains("InsuranceCardPremiumFeature.MedicationListReminder"))
        assertTrue(openNotes.contains("hasSmartNotepad"))
        assertTrue(openNotes.contains("InsuranceCardPremiumFeature.SmartNotepad"))
        assertTrue(openReverseDx.contains("hasDxCptReverseLookup"))
        assertTrue(openReverseDx.contains("InsuranceCardPremiumFeature.DxCptReverseLookup"))
        assertTrue(openVault.contains("showPaywall"))
        assertTrue(openNotes.contains("showPaywall"))
        assertTrue(openReverseDx.contains("showPaywall"))
    }

    @Test
    fun subscriptionCatalogListsMedicationNotepadAndDxLookup() {
        val silver = SubscriptionCatalog.features(SubscriptionTier.Silver)
        val gold = SubscriptionCatalog.features(SubscriptionTier.Gold)
        assertTrue(silver.contains("Medication List & Reminder"))
        assertFalse(silver.contains("Smart Notepad"))
        assertFalse(silver.contains("DX/CPT reverse lookup tool"))
        assertTrue(gold.contains("Medication List & Reminder"))
        assertTrue(gold.contains("Smart Notepad"))
        assertTrue(gold.contains("DX/CPT reverse lookup tool"))
    }

    @Test
    fun paywallUnlockMessagesResolveForInsuranceCardFeatures() {
        val keys = listOf(
            "paywallUnlockMedicationReminder",
            "paywallUnlockSmartNotepad",
            "paywallUnlockDxCptLookup"
        )
        AppLanguage.entries.forEach { language ->
            keys.forEach { key ->
                assertTrue(
                    "Missing $key for $language",
                    EobStrings.t(language, key).isNotBlank()
                )
            }
        }
        val viewModel = EobViewModel()
        AppLanguage.entries.forEach { language ->
            assertTrue(
                viewModel.paywallMessageForInsuranceCardFeature(
                    language,
                    InsuranceCardPremiumFeature.MedicationListReminder
                ).isNotBlank()
            )
            assertTrue(
                viewModel.paywallMessageForInsuranceCardFeature(
                    language,
                    InsuranceCardPremiumFeature.SmartNotepad
                ).isNotBlank()
            )
            assertTrue(
                viewModel.paywallMessageForInsuranceCardFeature(
                    language,
                    InsuranceCardPremiumFeature.DxCptReverseLookup
                ).isNotBlank()
            )
        }
    }

    @Test
    fun veryfiPipelineUntouchedByTierGating() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("InsuranceCardPremiumFeature"))
        assertFalse(pipelineSource.contains("hasMedicationListReminder"))
        assertFalse(pipelineSource.contains("hasSmartNotepad"))
        assertFalse(pipelineSource.contains("hasDxCptReverseLookup"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
