package app.eob.me

import app.eob.me.data.BillingIssue
import app.eob.me.data.BillingIssueSeverity
import app.eob.me.data.BillingIssueType
import app.eob.me.data.ClaimStatus
import app.eob.me.data.CptCategory
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import app.eob.me.data.ExpenseAnalyticsCalculator
import app.eob.me.data.ExpenseAnalyticsSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseAnalyticsCalculatorTest {
    @Test
    fun buildStateAggregatesAllocationAcrossRecords() {
        val records = listOf(
            sampleRecord(
                id = 1,
                provider = "City Clinic",
                billed = 200.0,
                adjustment = 50.0,
                insurancePaid = 100.0,
                patient = 50.0
            ),
            sampleRecord(
                id = 2,
                provider = "City Clinic",
                billed = 100.0,
                adjustment = 20.0,
                insurancePaid = 60.0,
                patient = 20.0
            )
        )
        val state = ExpenseAnalyticsCalculator.buildState(
            records = records,
            sort = ExpenseAnalyticsSort.HighestBilledTotal,
            expandedFacilityIds = emptySet(),
            appealedClaimIds = emptySet(),
            issueDetector = { emptyList() },
            isLoading = false
        )
        assertEquals(300.0, state.totalBilled, 0.01)
        assertEquals(70.0, state.totalNetworkSavings, 0.01)
        assertEquals(160.0, state.totalCarrierContribution, 0.01)
        assertEquals(70.0, state.totalPatientOutOfPocket, 0.01)
        assertEquals(1, state.facilities.size)
        assertEquals(2, state.facilities.first().claims.size)
    }

    @Test
    fun sortHighestPatientShareOrdersFacilitiesByShareRatio() {
        val records = listOf(
            sampleRecord(id = 1, provider = "Alpha", billed = 100.0, patient = 10.0),
            sampleRecord(id = 2, provider = "Beta", billed = 100.0, patient = 40.0)
        )
        val state = ExpenseAnalyticsCalculator.buildState(
            records = records,
            sort = ExpenseAnalyticsSort.HighestPatientShare,
            expandedFacilityIds = emptySet(),
            appealedClaimIds = emptySet(),
            issueDetector = { emptyList() },
            isLoading = false
        )
        assertEquals("Beta", state.facilities.first().providerName)
    }

    @Test
    fun claimStatusFlagsPotentialErrorFromBillingIssues() {
        val record = sampleRecord(id = 3, provider = "Gamma", billed = 80.0, patient = 20.0)
        val state = ExpenseAnalyticsCalculator.buildState(
            records = listOf(record),
            sort = ExpenseAnalyticsSort.FacilityAlphabetical,
            expandedFacilityIds = emptySet(),
            appealedClaimIds = emptySet(),
            issueDetector = {
                listOf(
                    BillingIssue(
                        type = BillingIssueType.PossibleUnbundling,
                        severity = BillingIssueSeverity.Warning,
                        title = "Possible NCCI unbundling conflict",
                        explanation = "test",
                        recommendedAction = "test"
                    )
                )
            },
            isLoading = false
        )
        val status = state.facilities.first().claims.first().status
        assertTrue(status is ClaimStatus.PotentialError)
    }

    @Test
    fun appealedClaimIdMarksClaimAsAppealed() {
        val record = sampleRecord(id = 4, provider = "Delta", billed = 90.0, patient = 10.0)
        val state = ExpenseAnalyticsCalculator.buildState(
            records = listOf(record),
            sort = ExpenseAnalyticsSort.FacilityAlphabetical,
            expandedFacilityIds = emptySet(),
            appealedClaimIds = setOf(record.historyListKey()),
            issueDetector = { emptyList() },
            isLoading = false
        )
        assertEquals(ClaimStatus.Appealed, state.facilities.first().claims.first().status)
    }

    @Test
    fun titleCaseProviderNameFormatsDisplayName() {
        assertEquals("Jermaine Duhart", ExpenseAnalyticsCalculator.titleCaseProviderName("JERMAINE DUHART"))
    }

    @Test
    fun loadingStateReturnsLoadingFlag() {
        val state = ExpenseAnalyticsCalculator.buildState(
            records = emptyList(),
            sort = ExpenseAnalyticsSort.HighestPatientShare,
            expandedFacilityIds = emptySet(),
            appealedClaimIds = emptySet(),
            issueDetector = { emptyList() },
            isLoading = true
        )
        assertTrue(state.isLoading)
        assertFalse(state.facilities.isNotEmpty())
    }

    private fun sampleRecord(
        id: Int,
        provider: String,
        billed: Double,
        adjustment: Double = 0.0,
        insurancePaid: Double = 0.0,
        patient: Double = 0.0
    ): EobRecord {
        return EobRecord(
            id = id,
            sourceName = "test",
            providerName = provider,
            insuranceName = "Aetna",
            serviceDate = "01/15/2026",
            serviceDateSortKey = 20260115 + id,
            charges = listOf(
                EobCharge(
                    cptCode = "99213",
                    cptDescription = "Office visit",
                    category = CptCategory.OfficeVisit,
                    billedAmount = billed,
                    insurancePaidAmount = insurancePaid,
                    contractualAdjustmentAmount = adjustment,
                    copayAmount = patient,
                    deductibleAmount = 0.0,
                    coinsuranceAmount = 0.0,
                    serviceDate = "01/15/2026"
                )
            ),
            duplicateChargeWarnings = emptyList(),
            rawText = "",
            totalBilledAmount = billed,
            totalInsurancePaidAmount = insurancePaid,
            totalContractualAdjustmentAmount = adjustment,
            totalCopayAmount = patient
        )
    }
}
