package app.eob.me

import app.eob.me.data.CptCategory
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import app.eob.me.data.UpcodingVerificationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpcodingVerificationCalculatorTest {
    @Test
    fun returnsAlertForEachHighLevelEmCode() {
        val codes = listOf(
            "99204" to "45-59 minutes",
            "99205" to "60-74 minutes",
            "99214" to "30-39 minutes",
            "99215" to "40-54 minutes"
        )
        codes.forEach { (code, expectedTime) ->
            val alert = UpcodingVerificationCalculator.upcodingVerificationForCharge(
                sampleCharge(cptCode = code)
            )
            assertEquals(expectedTime, alert?.requiredTimeRange)
        }
    }

    @Test
    fun ignoresNonUpcodingCodes() {
        val alert = UpcodingVerificationCalculator.upcodingVerificationForCharge(
            sampleCharge(cptCode = "99213")
        )
        assertNull(alert)
    }

    @Test
    fun recordLookupReturnsActiveAlertsOnly() {
        val record = EobRecord(
            id = 1,
            sourceName = "test",
            providerName = "Clinic",
            insuranceName = "Aetna",
            serviceDate = "01/15/2026",
            serviceDateSortKey = 20260115,
            charges = listOf(
                sampleCharge(cptCode = "99215", billedAmount = 250.0),
                sampleCharge(
                    cptCode = "80053",
                    cptDescription = "Lab panel",
                    billedAmount = 40.0,
                    category = CptCategory.Lab
                )
            ),
            duplicateChargeWarnings = emptyList(),
            rawText = ""
        )
        val alerts = UpcodingVerificationCalculator.upcodingVerificationsForRecord(record)
        assertEquals(1, alerts.size)
        assertEquals("99215", alerts.first().cptCode)
        assertEquals("40-54 minutes", alerts.first().requiredTimeRange)
    }

    private fun sampleCharge(
        cptCode: String,
        cptDescription: String = "Office visit",
        billedAmount: Double = 200.0,
        category: CptCategory = CptCategory.OfficeVisit
    ): EobCharge {
        return EobCharge(
            cptCode = cptCode,
            cptDescription = cptDescription,
            category = category,
            billedAmount = billedAmount,
            insurancePaidAmount = 0.0,
            contractualAdjustmentAmount = 0.0,
            copayAmount = 0.0,
            deductibleAmount = 0.0,
            coinsuranceAmount = 0.0,
            serviceDate = "01/15/2026"
        )
    }
}
