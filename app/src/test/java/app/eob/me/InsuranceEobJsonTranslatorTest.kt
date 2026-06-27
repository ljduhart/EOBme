package app.eob.me

import app.eob.me.data.InsuranceEobJsonTranslator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsuranceEobJsonTranslatorTest {
    @Test
    fun translateNestedInsuranceEobUnpivotsAllServiceLinesAndDates() {
        val payload = sampleHealthTexasInsuranceEobPayload()
        val translation = requireNotNull(
            InsuranceEobJsonTranslator.translate(
                payload = payload,
                documentRefId = "eob_123_jpg",
                sourceName = "Camera"
            )
        )

        val record = translation.mergedRecord
        assertEquals("BlueCross BlueShield of Texas", record.insuranceName)
        assertEquals("ALAMO FAMILY & COSMETIC D", record.providerName)
        assertEquals(3, record.charges.size)
        assertEquals("D5225", record.charges[0].cptCode)
        assertEquals("D0120", record.charges[1].cptCode)
        assertEquals("D1110", record.charges[2].cptCode.trim())
        assertEquals("03/21/2026", record.charges[0].serviceDate)
        assertEquals("04/11/2026", record.charges[1].serviceDate.trim())
        assertTrue(record.totalBilledAmount > 0.0)
        assertTrue(record.totalPatientResponsibility > 0.0)
        assertTrue(record.charges.any { it.billedAmount == 1438.0 })
        assertTrue(record.charges.any { it.insurancePaidAmount == 30.49 })
    }

    @Test
    fun parseMoneyHandlesUsAndEuropeanFormats() {
        assertEquals(1578.0, InsuranceEobJsonTranslator.parseMoney("$ 1,578.00"), 0.01)
        assertEquals(511.42, InsuranceEobJsonTranslator.parseMoney("$ 511,42"), 0.01)
        assertEquals(896.7, InsuranceEobJsonTranslator.parseMoney(896.7), 0.01)
    }

    @Test
    fun normalizeServiceDateExpandsTwoDigitYear() {
        assertEquals("03/24/2026", InsuranceEobJsonTranslator.normalizeServiceDate("03/24/26"))
        assertEquals("04/11/2026", InsuranceEobJsonTranslator.normalizeServiceDate(" 04/11/26 "))
    }

    @Test
    fun isProcedureCodeAcceptsCptAndHcpcsCodes() {
        assertTrue(InsuranceEobJsonTranslator.isProcedureCode("D5225"))
        assertTrue(InsuranceEobJsonTranslator.isProcedureCode("D1110"))
    }

    private fun sampleHealthTexasInsuranceEobPayload(): Map<String, Any?> {
        return mapOf(
            "group_name" to "HEALTHTEXAS MEDICAL GROUP",
            "payer_name" to "BlueCross BlueShield of Texas",
            "benefit_type" to "Dental",
            "group_number" to "329372",
            "subscriber_id" to "829723024",
            "subscriber_name" to "DUHART,LESTER J",
            "benefit_year_max_remaining" to 576.65,
            "claims" to listOf(
                mapOf(
                    "provider_name" to "ALAMO FAMILY & COSMETIC D",
                    "claim_number_1" to "0020260831991150500000",
                    "processed_date" to "03/24/26",
                    "claim_totals" to mapOf(
                        "total_billed_1" to "$ 1,578.00",
                        "patient_responsibility_1" to 473.35,
                        "total_health_plan_responsibility_1" to "$ 511,42",
                        "total_contractual_adjustment_1" to "$ 593.23",
                        "total_patient_responsibility_1" to 473.35
                    ),
                    "service_lines" to listOf(
                        mapOf(
                            "cpt_code_1" to "D5225",
                            "cpt_code_2" to "D0120",
                            "cpt_code_3" to "D1110 ",
                            "service_date_1" to "03/21/26",
                            "service_date_2" to "04/11/26 ",
                            "service_date_3" to " 04/11/26",
                            "amount_billed_1" to 1,
                            "amount_billed_2" to 45,
                            "amount_billed_3" to 95,
                            "allowed_amount_1" to 896.7,
                            "allowed_amount_2" to 30.49,
                            "allowed_amount_3" to 57.58,
                            "contractual_adjustment_1" to 541.3,
                            "contractual_adjustment_2" to 14.51,
                            "contractual_adjustment_3" to 37.42,
                            "deductible_amount_1" to 50,
                            "coinsurance_amount_1" to 423.35,
                            "patient_responsibility_1" to 473.35,
                            "health_plan_responsibility_2" to 30.49,
                            "health_plan_responsibility_3" to 57.58,
                            "service_description_1" to "Maxillary Partial Denture - Flexible Bas",
                            "total_amount_billed_1" to "$ 1,438.00",
                            "total_amount_billed_2" to "$ 140.00"
                        )
                    )
                )
            )
        )
    }
}
