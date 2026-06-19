package app.eob.me

import app.eob.me.util.EobDocumentOcrPreCheck
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EobDocumentOcrPreCheckTest {
    @Test
    fun passesWhenEobKeywordsPresent() {
        val result = EobDocumentOcrPreCheck.validate(
            "Explanation of Benefits\nClaim #12345\nPatient Responsibility: $42.00"
        )
        assertTrue(result.passed)
        assertTrue(result.matchedKeywords.isNotEmpty())
    }

    @Test
    fun failsWhenNoInsuranceKeywordsPresent() {
        val result = EobDocumentOcrPreCheck.validate("Grocery receipt total $18.44")
        assertFalse(result.passed)
    }

    @Test
    fun receiptScanTypePassesMedicalReceiptKeywords() {
        val result = EobDocumentOcrPreCheck.validateForScanType(
            ocrText = "Medical receipt\nProvider: Downtown Clinic\nTotal: $84.00\nPayment received",
            scanType = app.eob.me.data.CameraScanDocumentType.Receipt
        )
        assertTrue(result.passed)
        assertTrue(result.matchedKeywords.any { it == "receipt" || it == "provider" })
    }

    @Test
    fun receiptScanTypeStillRejectsUnrelatedRetailReceipt() {
        val result = EobDocumentOcrPreCheck.validateForScanType(
            ocrText = "Grocery receipt total \$18.44",
            scanType = app.eob.me.data.CameraScanDocumentType.Receipt
        )
        assertFalse(result.passed)
    }
}
