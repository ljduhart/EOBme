package app.eob.me

import app.eob.me.data.InsuranceEobRecordBridge
import app.eob.me.network.VeryfiCurrencyParser
import app.eob.me.network.VeryfiDateNormalizer
import app.eob.me.network.VeryfiInsuranceEobPayloadParser
import app.eob.me.network.dto.VeryfiInsuranceEobResponseDto
import app.eob.me.network.toNormalizedInsuranceEob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VeryfiInsuranceEobMapperTest {
    @Test
    fun mapsSingleIndexedServiceLine() {
        val payload = mapOf(
            "payer_name" to "BlueCross BlueShield of Texas",
            "claims" to listOf(
                mapOf(
                    "provider_name" to "Sunrise Clinic",
                    "processed_date" to "03/24/26",
                    "service_lines" to listOf(
                        mapOf(
                            "cpt_code_1" to "99213",
                            "service_date_1" to "03/21/26",
                            "amount_billed_1" to "$ 140.00",
                            "health_plan_responsibility_1" to 95.0,
                            "patient_responsibility_1" to 20.0,
                            "service_description_1" to "Office visit"
                        )
                    )
                )
            )
        )

        val result = requireNotNull(payload.toNormalizedInsuranceEob().getOrNull())
        val lines = result.document.allServiceLines
        assertEquals(1, lines.size)
        assertEquals("99213", lines[0].procedureCode)
        assertEquals("2026-03-21", lines[0].serviceDateIso)
        assertEquals(140.0, lines[0].billedAmount, 0.01)
        assertEquals(95.0, lines[0].insurancePaidAmount, 0.01)
        assertEquals("Office visit", lines[0].description)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun mapsEightIndexedServiceLinesFromHealthTexasPayload() {
        val payload = healthTexasInsuranceEobPayload()
        val dto = VeryfiInsuranceEobPayloadParser.parse(payload)
        val result = requireNotNull(dto.toNormalizedInsuranceEob().getOrNull())
        val lines = result.document.allServiceLines

        assertEquals("HEALTHTEXAS MEDICAL GROUP", result.document.groupName)
        assertEquals("BlueCross BlueShield of Texas", result.document.payerName)
        assertEquals("DUHART,LESTER J", result.document.subscriberName)
        assertEquals(8, lines.size)

        assertEquals("D5225", lines[0].procedureCode)
        assertEquals("D0120", lines[1].procedureCode)
        assertEquals("D1110", lines[2].procedureCode)
        assertEquals("99213", lines[3].procedureCode)
        assertEquals("99214", lines[4].procedureCode)
        assertEquals("36415", lines[5].procedureCode)
        assertEquals("81003", lines[6].procedureCode)
        assertEquals("99395", lines[7].procedureCode)

        assertEquals("2026-03-21", lines[0].serviceDateIso)
        assertEquals("2026-04-11", lines[1].serviceDateIso)
        assertEquals("2026-04-11", lines[6].serviceDateIso)
        assertEquals(1438.0, lines[0].billedAmount, 0.01)
        assertEquals(140.0, lines[1].billedAmount, 0.01)
        assertEquals(95.0, lines[2].billedAmount, 0.01)
        assertEquals(30.49, lines[1].insurancePaidAmount, 0.01)
        assertEquals(473.35, lines[0].patientResponsibilityAmount, 0.01)
    }

    @Test
    fun mapsSingleDateOfServiceWithThreeCptCodes() {
        val payload = mapOf(
            "payer_name" to "Aetna",
            "claims" to listOf(
                mapOf(
                    "provider_name" to "Family Health Center",
                    "processed_date" to "02/10/26",
                    "service_lines" to listOf(
                        mapOf(
                            "cpt_code_1" to "99213",
                            "cpt_code_2" to "99214",
                            "cpt_code_3" to "36415",
                            "service_date_1" to "02/05/26",
                            "amount_billed_1" to 150.0,
                            "amount_billed_2" to 220.0,
                            "amount_billed_3" to 25.0,
                            "health_plan_responsibility_1" to 120.0,
                            "health_plan_responsibility_2" to 180.0,
                            "health_plan_responsibility_3" to 20.0
                        )
                    )
                )
            )
        )

        val result = requireNotNull(payload.toNormalizedInsuranceEob().getOrNull())
        val lines = result.document.allServiceLines
        assertEquals(3, lines.size)
        assertEquals(listOf("99213", "99214", "36415"), lines.map { it.procedureCode })
        assertEquals(setOf("2026-02-05"), lines.map { it.serviceDateIso }.toSet())
        assertTrue(result.warnings.isEmpty())

        val record = InsuranceEobRecordBridge.toEobRecord(
            document = result.document,
            documentRefId = "eob_single_date_jpg",
            sourceName = "Camera"
        )
        assertEquals(3, record.charges.size)
        assertEquals("02/05/2026", record.serviceDate)
        assertEquals(setOf("02/05/2026"), record.charges.map { it.serviceDate }.toSet())
    }

    @Test
    fun mapsTwoServiceDatesWithSevenCptCodesThroughHybridPipeline() {
        val payload = twoDateSevenCptPayload()
        val documentRefId = "eob_two_dates_jpg"
        val record = app.eob.me.network.veryfiPayloadToEobRecord(
            payload = payload,
            documentRefId = documentRefId,
            sourceName = "Camera EOB"
        )

        assertEquals(7, record.charges.size)
        assertEquals(
            listOf("D5225", "D0120", "D1110", "99213", "99214", "36415", "81003"),
            record.charges.map { it.cptCode }
        )
        assertEquals(setOf("03/21/2026", "04/11/2026"), record.charges.map { it.serviceDate }.toSet())
        assertEquals("03/21/2026", record.serviceDate)
        assertEquals(3, record.charges.count { it.serviceDate == "03/21/2026" })
        assertEquals(4, record.charges.count { it.serviceDate == "04/11/2026" })
        assertTrue(record.totalBilledAmount > 0.0)
    }

    @Test
    fun firestoreRoundTripRehydratesNestedClaimsFromVeryfiClientStream() {
        val payload = twoDateSevenCptPayload()
        val firestoreRow = mapOf(
            "sourceName" to "Camera",
            "veryfiClientStream" to payload,
            "providerName" to "",
            "billed_amount" to 0.0
        )

        val record = app.eob.me.data.FirebaseEobMapper.eobFromMap(firestoreRow, documentId = "doc_42")

        assertEquals(7, record.charges.size)
        assertEquals(setOf("03/21/2026", "04/11/2026"), record.charges.map { it.serviceDate }.toSet())
        assertTrue(record.totalBilledAmount > 0.0)
    }

    @Test
    fun malformedIndexIsSkippedWithoutFailingRemainingLines() {
        val payload = mapOf(
            "payer_name" to "Test Payer",
            "claims" to listOf(
                mapOf(
                    "provider_name" to "Test Provider",
                    "service_lines" to listOf(
                        mapOf(
                            "cpt_code_1" to "99213",
                            "amount_billed_1" to 100.0,
                            "cpt_code_2" to "   ",
                            "cpt_code_3" to "99214",
                            "amount_billed_3" to 75.0
                        )
                    )
                )
            )
        )

        val result = requireNotNull(payload.toNormalizedInsuranceEob().getOrNull())
        val codes = result.document.allServiceLines.map { it.procedureCode }
        assertEquals(listOf("99213", "99214"), codes)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun bridgeProducesEobRecordWithCharges() {
        val payload = healthTexasInsuranceEobPayload()
        val normalized = requireNotNull(payload.toNormalizedInsuranceEob().getOrNull())
        val record = InsuranceEobRecordBridge.toEobRecord(
            document = normalized.document,
            documentRefId = "eob_123_jpg",
            sourceName = "Camera"
        )

        assertEquals("BlueCross BlueShield of Texas", record.insuranceName)
        assertEquals("ALAMO FAMILY & COSMETIC D", record.providerName)
        assertEquals(8, record.charges.size)
        assertTrue(record.totalBilledAmount > 0.0)
        assertTrue(record.totalPatientResponsibility > 0.0)
        assertEquals("03/21/2026", record.charges.first().serviceDate)
    }

    @Test
    fun currencyParserSanitizesDollarCommaAndWhitespace() {
        assertEquals(1578.0, VeryfiCurrencyParser.parse("$ 1,578.00"), 0.01)
        assertEquals(511.42, VeryfiCurrencyParser.parse("$ 511,42"), 0.01)
        assertEquals(0.0, VeryfiCurrencyParser.parse("   "), 0.01)
    }

    @Test
    fun dateNormalizerOutputsIso8601() {
        assertEquals("2026-03-24", VeryfiDateNormalizer.toIsoDate("03/24/26"))
        assertEquals("2026-04-11", VeryfiDateNormalizer.toIsoDate(" 04/11/26 "))
        assertEquals("2026-03-21", VeryfiDateNormalizer.toIsoDate("2026-03-21"))
    }

    @Test
    fun dtoExtensionMapsUntypedPayload() {
        val dto: VeryfiInsuranceEobResponseDto = VeryfiInsuranceEobPayloadParser.parse(
            healthTexasInsuranceEobPayload()
        )
        assertEquals(1, dto.resolvedClaims().size)
        val lines = requireNotNull(dto.toNormalizedInsuranceEob().getOrNull()).document.allServiceLines
        assertEquals(8, lines.size)
    }

    private fun healthTexasInsuranceEobPayload(): Map<String, Any?> {
        return mapOf(
            "group_name" to "HEALTHTEXAS MEDICAL GROUP",
            "payer_name" to "BlueCross BlueShield of Texas",
            "benefit_type" to "Dental",
            "group_number" to "329372",
            "subscriber_id" to "829723024",
            "subscriber_name" to "DUHART,LESTER J",
            "payer_mailing_address" to "P.O Box 660247 Dallas TX 75266-0247",
            "orthodontia_max_remaining" to 1,
            "benefit_year_max_remaining" to 576.65,
            "subscriber_mailing_address" to "8603 N LOOP 1604 W APT 7111 SAN ANTONIO TX 78249",
            "claims" to listOf(
                mapOf(
                    "provider_name" to "ALAMO FAMILY & COSMETIC D",
                    "claim_number_1" to "0020260831991150500000",
                    "claim_number_2" to "0020261041991236100000",
                    "processed_date" to "03/24/26",
                    "claim_totals" to mapOf(
                        "total_billed_1" to "$ 1,578.00",
                        "amount_billed_1" to 1,
                        "allowed_amount_1" to 896.7,
                        "contractual_adjustment_1" to 541.3,
                        "patient_responsibility_1" to 473.35,
                        "health_plan_responsibility" to 423.35,
                        "total_contractual_adjustment_1" to "$ 593.23",
                        "total_patient_responsibility_1" to 473.35,
                        "total_health_plan_responsibility_1" to "$ 511,42"
                    ),
                    "service_lines" to listOf(
                        mapOf(
                            "cpt_code_1" to "D5225",
                            "cpt_code_2" to "D0120",
                            "cpt_code_3" to "D1110 ",
                            "cpt_code_4" to "99213",
                            "cpt_code_5" to "99214",
                            "cpt_code_6" to "36415",
                            "cpt_code_7" to "81003",
                            "cpt_code_8" to "99395",
                            "copay_amount_1" to 0,
                            "copay_amount_2" to 0,
                            "copay_amount_3" to 0,
                            "service_date_1" to "03/21/26",
                            "service_date_2" to "04/11/26 ",
                            "service_date_3" to " 04/11/26",
                            "amount_billed_1" to 1,
                            "amount_billed_2" to 45,
                            "amount_billed_3" to 95,
                            "allowed_amount_1" to 896.7,
                            "allowed_amount_2" to 30.49,
                            "allowed_amount_3" to 57.58,
                            "deductible_amount_1" to 50,
                            "coinsurance_amount_1" to 423.35,
                            "coinsurance_amount_2" to 0,
                            "coinsurance_amount_3" to 0,
                            "service_description_1" to "Maxillary Partial Denture - Flexible Bas",
                            "total_amount_billed_1" to "$ 1,438.00",
                            "total_amount_billed_2" to "$ 140.00",
                            "contractual_adjustment_1" to 541.3,
                            "contractual_adjustment_2" to 14.51,
                            "contractual_adjustment_3" to 37.42,
                            "patient_responsibility_1" to 473.35,
                            "patient_responsibility_2" to 0,
                            "patient_responsibility_3" to 0,
                            "health_plan_responsibility_1" to 423.35,
                            "health_plan_responsibility_2" to 30.49,
                            "health_plan_responsibility_3" to 57.58
                        )
                    )
                )
            )
        )
    }

    private fun twoDateSevenCptPayload(): Map<String, Any?> {
        return mapOf(
            "payer_name" to "BlueCross BlueShield of Texas",
            "claims" to listOf(
                mapOf(
                    "provider_name" to "ALAMO FAMILY & COSMETIC D",
                    "processed_date" to "03/24/26",
                    "claim_totals" to mapOf(
                        "total_billed_1" to "$ 1,578.00",
                        "total_patient_responsibility_1" to 473.35,
                        "total_health_plan_responsibility_1" to "$ 511,42"
                    ),
                    "service_lines" to listOf(
                        mapOf(
                            "cpt_code_1" to "D5225",
                            "cpt_code_2" to "D0120",
                            "cpt_code_3" to "D1110",
                            "cpt_code_4" to "99213",
                            "cpt_code_5" to "99214",
                            "cpt_code_6" to "36415",
                            "cpt_code_7" to "81003",
                            "service_date_1" to "03/21/26",
                            "service_date_4" to "04/11/26",
                            "total_amount_billed_1" to "$ 1,438.00",
                            "total_amount_billed_2" to "$ 140.00",
                            "amount_billed_3" to 95.0,
                            "amount_billed_4" to 120.0,
                            "amount_billed_5" to 180.0,
                            "amount_billed_6" to 25.0,
                            "amount_billed_7" to 40.0,
                            "health_plan_responsibility_2" to 30.49,
                            "health_plan_responsibility_3" to 57.58,
                            "patient_responsibility_1" to 473.35
                        )
                    )
                )
            )
        )
    }
}
