package com.eobme.app

import com.eobme.app.ocr.EobOcrParser
import com.eobme.app.reference.CptCodes
import com.eobme.app.reference.Icd10Codes
import com.eobme.app.reference.InsuranceCompanies
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun insuranceCompanyMatchesAetna() {
        val result = InsuranceCompanies.matchInsuranceName("Your claim was processed by Aetna Health Insurance")
        assertEquals("Aetna", result)
    }

    @Test
    fun insuranceCompanyMatchesUHC() {
        val result = InsuranceCompanies.matchInsuranceName("UnitedHealthcare EOB for member")
        assertEquals("UnitedHealthcare", result)
    }

    @Test
    fun insuranceCompanyMatchesBCBS() {
        val result = InsuranceCompanies.matchInsuranceName("Blue Cross Blue Shield of Texas")
        assertTrue(result.contains("Blue Cross Blue Shield"))
    }

    @Test
    fun insuranceCompanyNoMatch() {
        val result = InsuranceCompanies.matchInsuranceName("Random text with no insurance names")
        assertEquals("", result)
    }

    @Test
    fun cptCodeLookup() {
        val info = CptCodes.lookup("99213")
        assertNotNull(info)
        assertEquals("99213", info!!.code)
        assertTrue(info.description.contains("office visit", ignoreCase = true))
        assertEquals(CptCodes.CATEGORY_OV, info.category)
    }

    @Test
    fun cptCodeLookupDME() {
        val info = CptCodes.lookup("E0601")
        assertNotNull(info)
        assertEquals(CptCodes.CATEGORY_DME, info!!.category)
        assertTrue(info.description.contains("CPAP", ignoreCase = true))
    }

    @Test
    fun cptCodeValidation() {
        assertTrue(CptCodes.isValidCode("99213"))
        assertTrue(CptCodes.isValidCode("A4253"))
        assertTrue(CptCodes.isValidCode("E0601"))
        assertFalse(CptCodes.isValidCode("0123"))  // starts with 0
        assertFalse(CptCodes.isValidCode("K1234"))  // K not in A-J
        assertFalse(CptCodes.isValidCode("9921"))   // only 4 digits
        assertFalse(CptCodes.isValidCode("992133")) // 6 digits
    }

    @Test
    fun icd10CodeLookup() {
        val info = Icd10Codes.lookup("I10")
        assertNotNull(info)
        assertTrue(info!!.description.contains("hypertension", ignoreCase = true))
    }

    @Test
    fun icd10CodeLookupDiabetes() {
        val info = Icd10Codes.lookup("E11.9")
        assertNotNull(info)
        assertTrue(info!!.description.contains("diabetes", ignoreCase = true))
    }

    @Test
    fun ocrParserExtractsInsuranceName() {
        val text = """
            Explanation of Benefits
            UnitedHealthcare
            Provider: Dr. John Smith
            Date of Service: 01/15/2026
            Billed Amount: $250.00
            Plan Paid: $200.00
            Copay: $25.00
            Deductible: $0.00
            Coinsurance: $25.00
            CPT: 99213
        """.trimIndent()
        val parsed = EobOcrParser.parse(text)
        assertEquals("UnitedHealthcare", parsed.insuranceName)
    }

    @Test
    fun ocrParserExtractsCptCodes() {
        val text = "Services: 99213 99214 36415 performed on 01/15/2026"
        val parsed = EobOcrParser.parse(text)
        assertEquals(3, parsed.cptCodes.size)
        assertTrue(parsed.cptCodes.any { it.code == "99213" })
        assertTrue(parsed.cptCodes.any { it.code == "99214" })
        assertTrue(parsed.cptCodes.any { it.code == "36415" })
    }

    @Test
    fun ocrParserExtractsAmounts() {
        val text = """
            Billed Amount: $350.00
            Plan Paid: $280.00
            Copay: $30.00
            Deductible: $15.00
            Coinsurance: $25.00
            Contractual Adjustment: $70.00
        """.trimIndent()
        val parsed = EobOcrParser.parse(text)
        assertEquals(350.0, parsed.billedAmount, 0.01)
        assertEquals(280.0, parsed.insurancePaid, 0.01)
        assertEquals(30.0, parsed.copay, 0.01)
        assertEquals(15.0, parsed.deductible, 0.01)
        assertEquals(25.0, parsed.coinsurance, 0.01)
        assertEquals(70.0, parsed.contractualAdjustment, 0.01)
    }

    @Test
    fun ocrParserExtractsDate() {
        val text = "Date of service: 03/15/2026 Provider: Test"
        val parsed = EobOcrParser.parse(text)
        assertTrue(parsed.dateOfService > 0)
    }

    @Test
    fun validCptCodeFilteringRange() {
        assertTrue(EobOcrParser.isValidCptCode("10021"))
        assertTrue(EobOcrParser.isValidCptCode("99999"))
        assertTrue(EobOcrParser.isValidCptCode("A1234"))
        assertTrue(EobOcrParser.isValidCptCode("J9999"))
        assertFalse(EobOcrParser.isValidCptCode("01234"))
        assertFalse(EobOcrParser.isValidCptCode("Z1234"))
    }
}
