package app.eob.me

import app.eob.me.data.HybridDocumentRef
import app.eob.me.data.VeryfiAnyDocExtractionResult
import app.eob.me.data.VeryfiHealthInsuranceEob
import app.eob.me.network.VeryfiAnyDocMapper
import app.eob.me.network.veryfiPayloadToEobRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.math.abs
import org.junit.Test

/**
 * Verifies the on-device hybrid Veryfi extraction (Track B) produces an EOB that matches the
 * Storage-triggered Cloud Function (Track A) so both tracks reconcile onto the same Firestore
 * document instead of stalling or duplicating.
 */
class VeryfiHybridExtractionTest {

    /** Replicates the backend `stableId` hashing (`Math.abs(hash) || 1`) which uses JS/Java-identical string hashing. */
    private fun backendStableDocId(documentRefId: String): String {
        var hash = 0
        documentRefId.forEach { character ->
            hash = (hash shl 5) - hash + character.code
        }
        val positive = abs(hash.toLong())
        return if (positive == 0L) "1" else positive.toString()
    }

    @Test
    fun stableDocumentIdMatchesBackendCloudFunctionHashing() {
        listOf(
            "eob_1718932011000_jpg",
            "eob_1.pdf".replace(Regex("[^A-Za-z0-9_-]"), "_"),
            "users_doc_eob",
            ""
        ).forEach { refId ->
            assertEquals(
                "stableDocumentId diverged from backend for '$refId'",
                backendStableDocId(refId),
                HybridDocumentRef.stableDocumentId(refId)
            )
        }
    }

    @Test
    fun veryfiPayloadNormalizesIntoEobRecordWithSharedDocId() {
        val documentRefId = HybridDocumentRef.documentRefId("eob_1718932011000.jpg")
        val payload: Map<String, Any?> = mapOf(
            "provider_name" to "Sunrise Family Clinic",
            "payer_name" to "Aetna",
            "date_of_service" to "2026-01-15",
            "billed_amount" to 240.0,
            "insurance_paid" to 180.0,
            "copay" to 20.0,
            "deductible" to 40.0,
            "cpt_codes" to "99213"
        )

        val record = veryfiPayloadToEobRecord(payload, documentRefId, "Camera EOB")

        assertEquals(HybridDocumentRef.stableDocumentId(documentRefId), record.firestoreId)
        assertEquals("Sunrise Family Clinic", record.providerName)
        assertEquals("Aetna", record.insuranceName)
        assertEquals("01/15/2026", record.serviceDate)
        assertEquals(240.0, record.totalBilledAmount, 0.001)
        assertEquals(180.0, record.totalInsurancePaidAmount, 0.001)
        assertEquals(20.0, record.totalCopayAmount, 0.001)
        assertEquals(40.0, record.totalDeductibleAmount, 0.001)
        assertEquals(60.0, record.totalPatientResponsibility, 0.001)
        assertTrue("CPT charge should be synthesized", record.charges.any { it.cptCode == "99213" })
    }

    @Test
    fun veryfiPayloadFallsBackToRawTextWhenExplicitFieldsMissing() {
        val documentRefId = HybridDocumentRef.documentRefId("eob_22.jpg")
        val payload: Map<String, Any?> = mapOf(
            "vendor" to mapOf("name" to "Downtown Imaging"),
            "line_items" to listOf(mapOf("description" to "Office visit 99214")),
            "total" to 320.0
        )

        val record = veryfiPayloadToEobRecord(payload, documentRefId, "")

        assertEquals("Veryfi", record.sourceName)
        assertEquals("Downtown Imaging", record.providerName)
        assertEquals(320.0, record.totalBilledAmount, 0.001)
        assertTrue("CPT code from line items raw text should be detected", record.charges.any { it.cptCode == "99214" })
    }

    @Test
    fun extractionResultMergesIntoEobProcessedResult() {
        val documentRefId = HybridDocumentRef.documentRefId("eob_1718932011000.jpg")
        val payload: Map<String, Any?> = mapOf(
            "provider_name" to "Sunrise Family Clinic",
            "payer_name" to "Aetna",
            "date_of_service" to "2026-01-15",
            "billed_amount" to 240.0,
            "copay" to 20.0,
            "cpt_codes" to "99213"
        )
        val record = veryfiPayloadToEobRecord(payload, documentRefId, "Camera EOB")
        val extraction = VeryfiHealthInsuranceEob(
            documentId = documentRefId,
            blueprintName = "health_insurance_eob",
            insuranceCompanyName = "Aetna",
            memberName = "Jane Doe",
            memberId = "MEM1",
            patientName = "Jane Doe",
            claimId = "CLM1",
            inNetworkOutOfPocketBalance = 0.0,
            outOfNetworkOutOfPocketBalance = 0.0,
            dateOfService = "01/15/2026",
            providerName = "Sunrise Family Clinic"
        )
        val anyDocResult = VeryfiAnyDocExtractionResult(
            extraction = extraction,
            record = record,
            rawPayload = payload,
            downloadUrl = "https://storage.example/eob.jpg"
        )
        val processed = anyDocResult.toProcessedResult()
        assertEquals("https://storage.example/eob.jpg", processed.fileUrl)
        assertEquals("01/15/2026", processed.veryfiData.dateOfService)
        assertTrue(processed.veryfiData.cptCodes.contains("99213"))
        assertEquals(20.0, processed.veryfiData.copay, 0.001)
    }

    @Test
    fun veryfiPayloadExtractsFieldsFromOcrTextWhenBlueprintFieldsMissing() {
        val documentRefId = HybridDocumentRef.documentRefId("eob_ocr.jpg")
        val payload: Map<String, Any?> = mapOf(
            "ocr_text" to """
                Sunrise Family Clinic
                Aetna
                Date of Service: 01/15/2026
                Billed Amount: ${'$'}240.00
                Insurance Paid: ${'$'}180.00
                Copay: ${'$'}20.00
                Deductible: ${'$'}40.00
                Patient Responsibility: ${'$'}60.00
                CPT - 99213
            """.trimIndent()
        )

        val record = veryfiPayloadToEobRecord(payload, documentRefId, "Camera EOB")

        assertEquals(240.0, record.totalBilledAmount, 0.001)
        assertEquals(180.0, record.totalInsurancePaidAmount, 0.001)
        assertEquals(20.0, record.totalCopayAmount, 0.001)
        assertEquals(40.0, record.totalDeductibleAmount, 0.001)
        assertEquals(60.0, record.totalPatientResponsibility, 0.001)
        assertTrue(record.charges.any { it.cptCode == "99213" })
    }

    @Test
    fun veryfiPayloadPrefersCustomFieldsOverOcrRegex() {
        val documentRefId = HybridDocumentRef.documentRefId("eob_custom.jpg")
        val payload: Map<String, Any?> = mapOf(
            "ocr_text" to "Billed Amount: ${'$'}100.00 CPT - 99211",
            "custom_fields" to mapOf(
                "billed_amount" to mapOf("value" to "350.00"),
                "cpt" to mapOf("value" to "99214"),
                "patient_responsibility" to mapOf("value" to "75.00")
            )
        )

        val record = veryfiPayloadToEobRecord(payload, documentRefId, "Camera EOB")

        assertEquals(350.0, record.totalBilledAmount, 0.001)
        assertEquals(75.0, record.totalPatientResponsibility, 0.001)
        assertTrue(record.charges.any { it.cptCode == "99214" })
    }

    @Test
    fun firestoreRecordRehydratesFromVeryfiClientStreamOcrPayload() {
        val documentId = HybridDocumentRef.stableDocumentId("eob_rehydrate.jpg")
        val firestoreDoc: Map<String, Any?> = mapOf(
            "id" to documentId,
            "billed_amount" to 0.0,
            "insurance_paid" to 0.0,
            "copay" to 0.0,
            "veryfiClientStream" to mapOf(
                "ocr_text" to "Billed Amount: ${'$'}425.50 Insurance Paid: ${'$'}300.00 Copay: ${'$'}25.00 CPT - 99213"
            )
        )

        val record = app.eob.me.data.FirebaseEobMapper.eobFromMap(firestoreDoc, documentId)

        assertEquals(425.5, record.totalBilledAmount, 0.001)
        assertEquals(300.0, record.totalInsurancePaidAmount, 0.001)
        assertEquals(25.0, record.totalCopayAmount, 0.001)
        assertTrue(record.charges.any { it.cptCode == "99213" })
    }

    @Test
    fun ocrExtractedAmountsAreNotOverwrittenByZeroBlueprintFields() {
        val documentRefId = HybridDocumentRef.documentRefId("eob_zero_guard.jpg")
        val payload: Map<String, Any?> = mapOf(
            "ocr_text" to "Billed Amount: ${'$'}500.00 Insurance Paid: ${'$'}400.00 Contractual Adjustment: ${'$'}50.00"
        )

        val merged = VeryfiAnyDocMapper.mergePayloadWithEobFields(payload, documentRefId)
        val record = veryfiPayloadToEobRecord(merged, documentRefId, "Camera EOB")

        assertEquals(500.0, (merged["billed_amount"] as Number).toDouble(), 0.001)
        assertEquals(400.0, (merged["insurance_paid"] as Number).toDouble(), 0.001)
        assertEquals(50.0, (merged["contractual_adj"] as Number).toDouble(), 0.001)
        assertEquals(500.0, record.totalBilledAmount, 0.001)
        assertEquals(400.0, record.totalInsurancePaidAmount, 0.001)
        assertEquals(50.0, record.totalContractualAdjustmentAmount, 0.001)
    }
}
