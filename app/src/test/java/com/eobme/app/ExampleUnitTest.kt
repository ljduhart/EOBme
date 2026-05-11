package com.eobme.app

import com.eobme.app.data.CptCategory
import com.eobme.app.data.EobAnalyzer
import com.eobme.app.data.EobKnowledgeBase
import com.eobme.app.data.FirebaseEobMapper
import com.eobme.app.data.UserProfile
import org.junit.Test

import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun recognizesMajorInsuranceAndAmounts() {
        val record = EobAnalyzer.analyze(
            """
                Aetna Explanation of Benefits
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00 copay $30.00 deductible $25.00 coinsurance $20.00
            """.trimIndent(),
            "library",
            1
        )

        assertEquals("Aetna", record.insuranceName)
        assertEquals("Downtown Medical Group", record.providerName)
        assertEquals("02/03/2025", record.serviceDate)
        assertEquals(300.0, record.totalBilledAmount, 0.001)
        assertEquals(125.0, record.totalInsurancePaidAmount, 0.001)
        assertEquals(100.0, record.totalContractualAdjustmentAmount, 0.001)
        assertEquals(30.0, record.totalCopayAmount, 0.001)
        assertEquals(25.0, record.totalDeductibleAmount, 0.001)
        assertEquals(20.0, record.totalCoinsuranceAmount, 0.001)
    }

    @Test
    fun storesOnlyValidFiveCharacterCptCodes() {
        val codes = EobAnalyzer.validCptCodes("99215 01234 Z9999 A0425 J3301 123456")

        assertEquals(listOf("99215", "A0425", "J3301"), codes)
    }

    @Test
    fun cptUsageCountsByYearAndCategory() {
        val first = EobAnalyzer.analyze(
            """
                United Healthcare
                Provider: Lakeside Clinic
                Date of Service: 01/10/2025
                99215 billed $200.00 paid $80.00 adjustment $50.00
                80053 billed $40.00 paid $20.00 adjustment $10.00
            """.trimIndent(),
            "camera",
            1
        )
        val second = EobAnalyzer.analyze(
            """
                Blue Cross BlueShield
                Provider: Lakeside Clinic
                Date of Service: 03/15/2025
                99215 billed $210.00 paid $90.00 adjustment $60.00
            """.trimIndent(),
            "library",
            2
        )

        val usage = EobAnalyzer.cptUsage(listOf(first, second), 2025)
        val officeVisit = usage.first { it.info.code == "99215" }
        val lab = usage.first { it.info.code == "80053" }

        assertEquals(2, officeVisit.count)
        assertEquals(CptCategory.OfficeVisit, officeVisit.info.category)
        assertEquals(1, lab.count)
        assertEquals(CptCategory.Lab, lab.info.category)
    }

    @Test
    fun libraryUploadDoesNotCreateFalseDuplicateWarning() {
        val record = EobAnalyzer.analyze(
            """
                Cigna
                Provider: North Hospital
                Date of Service: 04/01/2025
                99213 billed $160.00 paid $75.00 adjustment $50.00
                99214 billed $220.00 paid $100.00 adjustment $80.00
            """.trimIndent(),
            "library",
            3
        )

        assertTrue(record.duplicateChargeWarnings.isEmpty())
    }

    @Test
    fun duplicateWarningsRequireSameDateCodeAndBilledAmount() {
        val record = EobAnalyzer.analyze(
            """
                Humana
                Provider: North Hospital
                Date of Service: 04/01/2025
                99213 billed $160.00 paid $75.00 adjustment $50.00
                99213 billed $160.00 paid $75.00 adjustment $50.00
            """.trimIndent(),
            "camera",
            4
        )

        assertEquals(1, record.duplicateChargeWarnings.size)
    }

    @Test
    fun eobsSortByDateOfServiceFromBeginningOfYear() {
        val dates = listOf("03/01/2025", "01/02/2025", "02/10/2025")
            .sortedBy { EobAnalyzer.serviceDateSortKey(it) }

        assertEquals(listOf("01/02/2025", "02/10/2025", "03/01/2025"), dates)
    }

    @Test
    fun commonIcd10MemoryContainsDescriptions() {
        assertEquals("Essential hypertension.", EobKnowledgeBase.commonIcd10Codes["I10"])
        assertTrue(EobKnowledgeBase.commonIcd10Codes.containsKey("E11.9"))
    }

    @Test
    fun firebaseMapperRoundTripsProfileAndEobData() {
        val profile = UserProfile(
            firstName = "Lester",
            lastName = "Duhart",
            email = "member@example.com",
            password = "private",
            city = "Atlanta",
            state = "GA",
            subscriberId = "SUB123",
            insuranceCardSummary = "Aetna card on file",
            insuranceCardDownloadUrl = "https://firebasestorage.example/card.jpg"
        )
        val profileMap = FirebaseEobMapper.profileToMap(profile)
        val restoredProfile = FirebaseEobMapper.profileFromMap(profileMap, currentPassword = profile.password)
        val record = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00
            """.trimIndent(),
            "library",
            10
        )
        val restoredRecord = FirebaseEobMapper.eobFromMap(FirebaseEobMapper.eobToMap(record))

        assertEquals(profile, restoredProfile)
        assertEquals(record.insuranceName, restoredRecord.insuranceName)
        assertEquals(record.providerName, restoredRecord.providerName)
        assertEquals(record.serviceDateSortKey, restoredRecord.serviceDateSortKey)
        assertEquals(record.charges.first().cptCode, restoredRecord.charges.first().cptCode)
    }
}