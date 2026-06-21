package app.eob.me

import app.eob.me.data.HybridDocumentRef
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
}
