package app.eob.me

import app.eob.me.data.BillingIssueType
import app.eob.me.data.CptCategory
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import app.eob.me.data.NcciBundlingCalculator
import app.eob.me.data.ncci.NcciBundlingMap
import app.eob.me.data.ncci.NcciEmBundlingMap
import app.eob.me.data.ncci.NcciLabBundlingMap
import app.eob.me.data.ncci.NcciRadiologyBundlingMap
import app.eob.me.data.ncci.NcciSpecialtyBundlingMap
import app.eob.me.data.ncci.NcciSurgeryBundlingMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NcciBundlingCalculatorTest {
    @Test
    fun masterMapContainsAtLeast2500BundlingPairs() {
        assertTrue(NcciBundlingMap.totalPairCount >= 2500)
    }

    @Test
    fun categoryMapsMeetMinimumCoverageTargets() {
        assertTrue(pairCount(NcciEmBundlingMap.rules) >= 300)
        assertTrue(pairCount(NcciLabBundlingMap.rules) >= 400)
        assertTrue(pairCount(NcciSurgeryBundlingMap.rules) >= 600)
        assertTrue(pairCount(NcciRadiologyBundlingMap.rules) >= 300)
        assertTrue(pairCount(NcciSpecialtyBundlingMap.rules) >= 900)
    }

    @Test
    fun emMapIncludesHighViolationOfficeVisitPairs() {
        val bundled = NcciBundlingMap.bundledCodesFor("99213")
        assertNotNull(bundled)
        assertTrue(bundled!!.contains("36415"))
        assertTrue(bundled.contains("93000"))
        assertTrue(bundled.contains("99211"))
    }

    @Test
    fun labMapIncludesCmpComponentPairs() {
        val bundled = NcciBundlingMap.bundledCodesFor("80053")
        assertNotNull(bundled)
        assertTrue(bundled!!.contains("82310"))
        assertTrue(bundled.contains("82565"))
    }

    @Test
    fun surgeryMapIncludesKneeReplacementPairs() {
        val bundled = NcciBundlingMap.bundledCodesFor("27447")
        assertNotNull(bundled)
        assertTrue(bundled!!.contains("29881"))
        assertTrue(bundled.contains("20610"))
    }

    @Test
    fun radiologyMapIncludesChestXrayPairs() {
        val bundled = NcciBundlingMap.bundledCodesFor("71048")
        assertNotNull(bundled)
        assertTrue(bundled!!.contains("71045"))
    }

    @Test
    fun specialtyMapIncludesColonoscopyAndIvPairs() {
        val colonoscopy = NcciBundlingMap.bundledCodesFor("45385")
        val ivTherapy = NcciBundlingMap.bundledCodesFor("96374")
        assertNotNull(colonoscopy)
        assertNotNull(ivTherapy)
        assertTrue(colonoscopy!!.contains("45378"))
        assertTrue(ivTherapy!!.contains("96360"))
    }

    @Test
    fun calculatorFlagsUnbundledEmAndVenipunctureOnSameDate() {
        val record = sampleRecord(
            charges = listOf(
                sampleCharge("99213", 120.0),
                sampleCharge("36415", 15.0)
            )
        )
        val alerts = NcciBundlingCalculator.bundlingAlertsForRecord(record)
        assertEquals(1, alerts.size)
        assertEquals("99213", alerts.first().columnOneCode)
        assertEquals("36415", alerts.first().columnTwoCode)
    }

    @Test
    fun calculatorProducesBillingIssueForUnbundlingConflict() {
        val record = sampleRecord(
            charges = listOf(
                sampleCharge("99214", 180.0),
                sampleCharge("93000", 35.0)
            )
        )
        val issues = NcciBundlingCalculator.billingIssuesFor(record)
        assertEquals(1, issues.size)
        assertEquals(BillingIssueType.PossibleUnbundling, issues.first().type)
    }

    @Test
    fun calculatorIgnoresCodesOnDifferentDates() {
        val record = sampleRecord(
            charges = listOf(
                sampleCharge("99213", 120.0, "01/15/2026"),
                sampleCharge("36415", 15.0, "01/16/2026")
            )
        )
        assertTrue(NcciBundlingCalculator.bundlingAlertsForRecord(record).isEmpty())
    }

    private fun pairCount(rules: Map<String, Set<String>>): Int {
        return rules.values.sumOf { it.size }
    }

    private fun sampleCharge(
        cptCode: String,
        billedAmount: Double,
        serviceDate: String = "01/15/2026"
    ): EobCharge {
        return EobCharge(
            cptCode = cptCode,
            cptDescription = "Test charge",
            category = CptCategory.OfficeVisit,
            billedAmount = billedAmount,
            insurancePaidAmount = 0.0,
            contractualAdjustmentAmount = 0.0,
            copayAmount = 0.0,
            deductibleAmount = 0.0,
            coinsuranceAmount = 0.0,
            serviceDate = serviceDate
        )
    }

    private fun sampleRecord(charges: List<EobCharge>): EobRecord {
        return EobRecord(
            id = 1,
            sourceName = "test",
            providerName = "Clinic",
            insuranceName = "Aetna",
            serviceDate = "01/15/2026",
            serviceDateSortKey = 20260115,
            charges = charges,
            duplicateChargeWarnings = emptyList(),
            rawText = ""
        )
    }
}
