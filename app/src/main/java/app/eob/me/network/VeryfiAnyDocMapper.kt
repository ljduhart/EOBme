package app.eob.me.network

import app.eob.me.data.VeryfiHealthInsuranceEob
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object VeryfiAnyDocMapper {
    private val gson = Gson()

    fun parseResponse(payload: Map<String, Any?>): VeryfiAnyDocResponseDto {
        val json = gson.toJson(payload)
        return gson.fromJson(json, VeryfiAnyDocResponseDto::class.java)
    }

    fun toDomain(
        dto: VeryfiAnyDocResponseDto,
        documentRefId: String
    ): VeryfiHealthInsuranceEob {
        val documentId = dto.id?.toString().orEmpty().ifBlank { documentRefId }
        return VeryfiHealthInsuranceEob(
            documentId = documentId,
            blueprintName = dto.blueprintName?.trim().orEmpty()
                .ifBlank { VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB },
            insuranceCompanyName = firstNonBlank(
                dto.insuranceCompanyName,
                dto.insuranceCompany,
                dto.payerName
            ),
            memberName = dto.memberName?.trim().orEmpty(),
            memberId = firstNonBlank(dto.memberId, dto.memberNumber),
            patientName = dto.patientName?.trim().orEmpty(),
            claimId = firstNonBlank(dto.claimId, dto.claimNumber),
            inNetworkOutOfPocketBalance = firstNonNull(dto.inNetworkOutOfPocketBalance, dto.inNetworkOutOfPocket),
            outOfNetworkOutOfPocketBalance = firstNonNull(
                dto.outOfNetworkOutOfPocketBalance,
                dto.outOfNetworkOutOfPocket
            ),
            dateOfService = firstNonBlank(dto.dateOfService, dto.serviceDate),
            providerName = dto.providerName?.trim().orEmpty()
        )
    }

    fun toEobNormalizationFields(
        extraction: VeryfiHealthInsuranceEob,
        dto: VeryfiAnyDocResponseDto
    ): Map<String, Any?> {
        val lineItemText = dto.lineItems.orEmpty().joinToString(" ") { item ->
            listOfNotNull(item.description, item.cptCode).joinToString(" ")
        }
        return mapOf<String, Any?>(
            "insurance_name" to extraction.insuranceCompanyName,
            "payer_name" to extraction.insuranceCompanyName,
            "provider_name" to extraction.providerName,
            "member_name" to extraction.memberName,
            "member_id" to extraction.memberId,
            "patient_name" to extraction.patientName,
            "claim_id" to extraction.claimId,
            "date_of_service" to extraction.dateOfService,
            "in_network_out_of_pocket_balance" to extraction.inNetworkOutOfPocketBalance,
            "out_of_network_out_of_pocket_balance" to extraction.outOfNetworkOutOfPocketBalance,
            "billed_amount" to dto.billedAmount,
            "insurance_paid" to dto.insurancePaid,
            "contractual_adj" to listOfNotNull(dto.contractualAdj, dto.contractualAdjustment).firstOrNull(),
            "copay" to dto.copay,
            "deductible" to dto.deductible,
            "coinsurance" to dto.coinsurance,
            "patient_responsibility" to dto.patientResponsibility,
            "cpt" to firstNonBlank(dto.cpt, dto.cptCode, dto.cptCodes),
            "cpt_codes" to firstNonBlank(dto.cptCodes, dto.cptCode, dto.cpt),
            "ocr_text" to firstNonBlank(dto.ocrText, dto.text),
            "blueprint_name" to extraction.blueprintName,
            "line_items" to lineItemText
        )
    }

    fun mergePayloadWithEobFields(
        payload: Map<String, Any?>,
        documentRefId: String
    ): Map<String, Any?> {
        val ocrEnriched = VeryfiOcrFieldExtractor.enrichPayload(payload)
        val dto = parseResponse(ocrEnriched)
        val extraction = toDomain(dto, documentRefId)
        val eobFields = toEobNormalizationFields(extraction, dto)
        return ocrEnriched + eobFields.filterValues { value ->
            when (value) {
                null -> false
                is String -> value.isNotBlank()
                is Number -> value.toDouble() != 0.0
                else -> true
            }
        }
    }

    fun mapFromUntypedPayload(
        payload: Map<String, Any?>,
        documentRefId: String
    ): VeryfiHealthInsuranceEob = toDomain(parseResponse(payload), documentRefId)

    private fun firstNonBlank(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
    }

    private fun firstNonNull(vararg values: Double?): Double {
        return values.firstOrNull { it != null } ?: 0.0
    }
}
