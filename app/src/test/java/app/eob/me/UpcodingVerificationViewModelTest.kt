package app.eob.me

import app.eob.me.data.BillingIssueType
import app.eob.me.data.CptCategory
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpcodingVerificationViewModelTest {
    @Test
    fun disputedUpcodingAddsSuspectedUpcodingBillingIssue() {
        val viewModel = EobViewModel()
        val record = highLevelEmRecord()
        val charge = record.charges.first()
        assertTrue(viewModel.upcodingVerificationForCharge(record, charge) != null)

        viewModel.recordUpcodingVerificationDisputed(record, charge)

        assertNull(viewModel.upcodingVerificationForCharge(record, charge))
        val issues = viewModel.detectBillingIssuesForRecord(record)
        assertTrue(issues.any { it.type == BillingIssueType.SuspectedUpcoding })
    }

    @Test
    fun affirmedUpcodingDismissesBubbleWithoutSuspectedIssue() {
        val viewModel = EobViewModel()
        val record = highLevelEmRecord()
        val charge = record.charges.first()

        viewModel.recordUpcodingVerificationAffirmed(record, charge)

        assertNull(viewModel.upcodingVerificationForCharge(record, charge))
        val issues = viewModel.detectBillingIssuesForRecord(record)
        assertTrue(issues.none { it.type == BillingIssueType.SuspectedUpcoding })
    }

    private fun highLevelEmRecord(): EobRecord {
        return EobRecord(
            id = 1,
            sourceName = "test",
            providerName = "Clinic",
            insuranceName = "Aetna",
            serviceDate = "01/15/2026",
            serviceDateSortKey = 20260115,
            charges = listOf(
                EobCharge(
                    cptCode = "99215",
                    cptDescription = "Office visit",
                    category = CptCategory.OfficeVisit,
                    billedAmount = 250.0,
                    insurancePaidAmount = 0.0,
                    contractualAdjustmentAmount = 0.0,
                    copayAmount = 0.0,
                    deductibleAmount = 0.0,
                    coinsuranceAmount = 0.0,
                    serviceDate = "01/15/2026"
                )
            ),
            duplicateChargeWarnings = emptyList(),
            rawText = ""
        )
    }
}
