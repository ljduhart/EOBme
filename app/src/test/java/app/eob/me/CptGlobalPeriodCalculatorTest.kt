package app.eob.me

import app.eob.me.data.BillingIssueType
import app.eob.me.data.CptCategory
import app.eob.me.data.CptGlobalPeriodCalculator
import app.eob.me.data.CptGlobalPeriodMap
import app.eob.me.data.EobCharge
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.EobRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CptGlobalPeriodCalculatorTest {
    @Test
    fun globalPeriodMapContainsMajorAndMinorSurgeryCodes() {
        assertEquals(90, CptGlobalPeriodMap.globalDaysFor("27447"))
        assertEquals(10, CptGlobalPeriodMap.globalDaysFor("12001"))
        assertEquals(80, CptGlobalPeriodMap.entries.size)
        assertNull(CptGlobalPeriodMap.globalDaysFor("99213"))
    }

    @Test
    fun knowledgeBaseAssignsGlobalPeriodCodesToHospitalCategory() {
        val hipReplacement = EobKnowledgeBase.cptInfoFor("27130")
        assertEquals(CptCategory.Hospital, hipReplacement.category)
        assertTrue(hipReplacement.description.contains("Hip"))

        val officeVisit = EobKnowledgeBase.cptInfoFor("99214")
        assertEquals(CptCategory.OfficeVisit, officeVisit.category)
    }

    @Test
    fun globalPeriodAlertCalculatesExpirationAndActiveWindow() {
        val charge = EobCharge(
            cptCode = "27447",
            cptDescription = "Total Knee Arthroplasty (Replacement)",
            category = CptCategory.Hospital,
            billedAmount = 12000.0,
            insurancePaidAmount = 0.0,
            contractualAdjustmentAmount = 0.0,
            copayAmount = 0.0,
            deductibleAmount = 0.0,
            coinsuranceAmount = 0.0,
            serviceDate = "01/15/2026"
        )
        val alert = CptGlobalPeriodCalculator.globalPeriodAlertForCharge(
            charge = charge,
            today = LocalDate.of(2026, 2, 1)
        )
        assertNotNull(alert)
        assertEquals(90, alert!!.globalDays)
        assertEquals("01/15/2026", alert.serviceDate)
        assertEquals("04/15/2026", alert.expirationDate)
        assertTrue(alert.isActive)
    }

    @Test
    fun globalPeriodAlertInactiveAfterWindowExpires() {
        val charge = EobCharge(
            cptCode = "10060",
            cptDescription = "Incision & Drainage (I&D) of Abscess, simple",
            category = CptCategory.Hospital,
            billedAmount = 250.0,
            insurancePaidAmount = 0.0,
            contractualAdjustmentAmount = 0.0,
            copayAmount = 0.0,
            deductibleAmount = 0.0,
            coinsuranceAmount = 0.0,
            serviceDate = "01/01/2026"
        )
        val alert = CptGlobalPeriodCalculator.globalPeriodAlertForCharge(
            charge = charge,
            today = LocalDate.of(2026, 1, 20)
        )
        assertNotNull(alert)
        assertFalse(alert!!.isActive)
    }

    @Test
    fun billingIssueFlagsOfficeVisitDuringActiveGlobalPeriod() {
        val surgeryRecord = sampleRecord(
            id = 1,
            charges = listOf(
                sampleCharge(
                    code = "27447",
                    serviceDate = "01/01/2026",
                    category = CptCategory.Hospital
                )
            )
        )
        val visitRecord = sampleRecord(
            id = 2,
            charges = listOf(
                sampleCharge(
                    code = "99213",
                    serviceDate = "01/20/2026",
                    category = CptCategory.OfficeVisit
                )
            )
        )
        val issues = CptGlobalPeriodCalculator.billingIssuesFor(
            record = visitRecord,
            allRecords = listOf(surgeryRecord, visitRecord)
        )
        assertTrue(issues.isNotEmpty())
        assertEquals(BillingIssueType.VisitDuringGlobalPeriod, issues.first().type)
    }

    private fun sampleRecord(id: Int, charges: List<EobCharge>): EobRecord {
        return EobRecord(
            id = id,
            sourceName = "test",
            providerName = "Test Provider",
            insuranceName = "Aetna",
            serviceDate = charges.firstOrNull()?.serviceDate ?: "01/01/2026",
            serviceDateSortKey = 20260101,
            charges = charges,
            duplicateChargeWarnings = emptyList(),
            rawText = "",
            totalBilledAmount = charges.sumOf { it.billedAmount }
        )
    }

    private fun sampleCharge(
        code: String,
        serviceDate: String,
        category: CptCategory
    ): EobCharge {
        val info = EobKnowledgeBase.cptInfoFor(code)
        return EobCharge(
            cptCode = code,
            cptDescription = info.description,
            category = category,
            billedAmount = 100.0,
            insurancePaidAmount = 0.0,
            contractualAdjustmentAmount = 0.0,
            copayAmount = 0.0,
            deductibleAmount = 0.0,
            coinsuranceAmount = 0.0,
            serviceDate = serviceDate
        )
    }
}
