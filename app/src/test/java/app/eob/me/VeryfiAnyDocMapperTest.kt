package app.eob.me

import app.eob.me.network.VeryfiAnyDocConstants
import app.eob.me.network.VeryfiAnyDocMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VeryfiAnyDocMapperTest {
    @Test
    fun mapsHealthInsuranceEobBlueprintFieldsToDomainModel() {
        val payload = mapOf(
            "id" to 42L,
            "blueprint_name" to VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB,
            "insurance_company_name" to "UnitedHealthcare",
            "member_name" to "Jane Doe",
            "member_id" to "UHC123456",
            "patient_name" to "Jane Doe",
            "claim_id" to "CLM-908877",
            "provider_name" to "Metro Health Clinic",
            "date_of_service" to "2026-02-10",
            "in_network_out_of_pocket_balance" to 1250.0,
            "out_of_network_out_of_pocket_balance" to 3200.0,
            "billed_amount" to 480.0,
            "insurance_paid" to 360.0,
            "copay" to 25.0
        )

        val extraction = VeryfiAnyDocMapper.mapFromUntypedPayload(payload, "eob_42_jpg")

        assertEquals("42", extraction.documentId)
        assertEquals(VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB, extraction.blueprintName)
        assertEquals("UnitedHealthcare", extraction.insuranceCompanyName)
        assertEquals("Jane Doe", extraction.memberName)
        assertEquals("UHC123456", extraction.memberId)
        assertEquals("Jane Doe", extraction.patientName)
        assertEquals("CLM-908877", extraction.claimId)
        assertEquals(1250.0, extraction.inNetworkOutOfPocketBalance, 0.001)
        assertEquals(3200.0, extraction.outOfNetworkOutOfPocketBalance, 0.001)
        assertEquals("Metro Health Clinic", extraction.providerName)
    }

    @Test
    fun mergePayloadAddsEobNormalizationFieldsForRecordMapping() {
        val payload = mapOf(
            "blueprint_name" to VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB,
            "payer_name" to "Aetna",
            "patient_name" to "John Smith",
            "claim_number" to "A-4455",
            "line_items" to listOf(mapOf("description" to "99213 office visit", "cpt_code" to "99213"))
        )

        val merged = VeryfiAnyDocMapper.mergePayloadWithEobFields(payload, "eob_test_jpg")

        assertEquals("Aetna", merged["insurance_name"])
        assertEquals("John Smith", merged["patient_name"])
        assertEquals("A-4455", merged["claim_id"])
        assertTrue(merged.containsKey("line_items"))
    }
}
