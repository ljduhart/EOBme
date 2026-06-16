package app.eob.me

import android.os.Looper
import app.eob.me.data.CptCategory
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.TaxVaultVisibilityMode
import app.eob.me.data.UserProfile
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TaxVaultFilterTest {
    @Test
    fun detectTaxVaultEligibilityUsesExplicitHsaKeywords() {
        val eligibility = EobAnalyzer.detectTaxVaultEligibility(
            rawText = "Paid with HSA card",
            charges = emptyList()
        )
        assertTrue(eligibility.isHsaEligible)
        assertFalse(eligibility.isFsaEligible)
    }

    @Test
    fun detectTaxVaultEligibilityUsesQualifiedMedicalCharges() {
        val eligibility = EobAnalyzer.detectTaxVaultEligibility(
            rawText = "Office visit summary",
            charges = listOf(
                EobCharge(
                    cptCode = "99213",
                    cptDescription = "Office visit",
                    category = CptCategory.OfficeVisit,
                    billedAmount = 120.0,
                    insurancePaidAmount = 80.0,
                    contractualAdjustmentAmount = 10.0,
                    copayAmount = 20.0,
                    deductibleAmount = 10.0,
                    coinsuranceAmount = 0.0,
                    serviceDate = "01/15/2026"
                )
            )
        )
        assertTrue(eligibility.isHsaEligible)
        assertTrue(eligibility.isFsaEligible)
    }

    @Test
    fun recordsForTaxVaultFilterReturnsOnlyMatchingEligibility() {
        val records = listOf(
            sampleRecord(id = 1, hsa = true, fsa = false, responsibility = 50.0),
            sampleRecord(id = 2, hsa = false, fsa = true, responsibility = 75.0),
            sampleRecord(id = 3, hsa = false, fsa = false, responsibility = 25.0)
        )
        assertEquals(1, EobAnalyzer.recordsForTaxVaultFilter(records, TaxVaultFilterState.HSA).size)
        assertEquals(1, EobAnalyzer.recordsForTaxVaultFilter(records, TaxVaultFilterState.FSA).size)
        assertEquals(3, EobAnalyzer.recordsForTaxVaultFilter(records, TaxVaultFilterState.OFF).size)
    }

    @Test
    fun eobViewModelTaxVaultBudgetSummaryUsesProfileAllocations() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        viewModel.replaceRecords(
            listOf(
                sampleRecord(id = 1, hsa = true, fsa = true, responsibility = 1250.0),
                sampleRecord(id = 2, hsa = true, fsa = false, responsibility = 500.0)
            ),
            profile = UserProfile()
        )
        waitForHubRecords(viewModel, expectedCount = 2)
        viewModel.setTaxVaultFilterState(TaxVaultFilterState.HSA)
        val summary = viewModel.taxVaultBudgetSummary(
            UserProfile(hsaAllocation = 3050.0, fsaAllocation = 1500.0)
        )
        assertEquals(1750.0, summary.eligibleAmount, 0.01)
        assertEquals(3050.0, summary.allocationLimit, 0.01)
        assertEquals(1300.0, summary.savedAmount, 0.01)
    }

    @Test
    fun eobViewModelHistoryRecordsForDisplayRespectsTaxVaultFilter() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        viewModel.replaceRecords(
            listOf(
                sampleRecord(id = 1, hsa = true, fsa = false, responsibility = 100.0),
                sampleRecord(id = 2, hsa = false, fsa = false, responsibility = 200.0)
            ),
            profile = UserProfile()
        )
        waitForHubRecords(viewModel, expectedCount = 2)
        viewModel.setTaxVaultFilterState(TaxVaultFilterState.HSA)
        val records = viewModel.historyRecordsForDisplay(HistoryBentoFilter.All, searchQuery = "")
        assertEquals(1, records.size)
        assertTrue(records.first().isHsaEligible)
    }

    @Test
    fun taxVaultFilterBlockedForNonGoldTiers() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Silver)
        viewModel.setTaxVaultFilterState(TaxVaultFilterState.HSA)
        assertEquals(TaxVaultFilterState.OFF, viewModel.taxVaultFilterState.value)

        viewModel.setSubscriptionTier(SubscriptionTier.Free)
        viewModel.setTaxVaultFilterState(TaxVaultFilterState.FSA)
        assertEquals(TaxVaultFilterState.OFF, viewModel.taxVaultFilterState.value)
    }

    @Test
    fun taxVaultVisibilityAllShowsFullHistoryWhileFilterActive() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        viewModel.replaceRecords(
            listOf(
                sampleRecord(id = 1, hsa = true, fsa = false, responsibility = 100.0),
                sampleRecord(id = 2, hsa = false, fsa = false, responsibility = 200.0)
            ),
            profile = UserProfile()
        )
        waitForHubRecords(viewModel, expectedCount = 2)
        viewModel.setTaxVaultFilterState(TaxVaultFilterState.HSA)
        viewModel.setTaxVaultVisibilityMode(TaxVaultVisibilityMode.ALL)
        val records = viewModel.historyRecordsForDisplay(HistoryBentoFilter.All, searchQuery = "")
        assertEquals(2, records.size)
        assertTrue(viewModel.isTaxVaultActive())
        assertFalse(viewModel.isTaxVaultHistoryGated())
    }

    private fun waitForHubRecords(viewModel: EobViewModel, expectedCount: Int) {
        var attempts = 0
        while (viewModel.eobRecords.value.size < expectedCount && attempts < 1_000) {
            shadowOf(Looper.getMainLooper()).idle()
            attempts++
        }
        assertEquals(expectedCount, viewModel.eobRecords.value.size)
    }

    private fun sampleRecord(
        id: Int,
        hsa: Boolean,
        fsa: Boolean,
        responsibility: Double
    ): EobRecord {
        return EobRecord(
            id = id,
            sourceName = "Test",
            providerName = "Provider $id",
            insuranceName = "Insurance",
            serviceDate = "01/01/2026",
            serviceDateSortKey = 20260101,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = "Test",
            totalCopayAmount = responsibility,
            isHsaEligible = hsa,
            isFsaEligible = fsa
        )
    }
}
