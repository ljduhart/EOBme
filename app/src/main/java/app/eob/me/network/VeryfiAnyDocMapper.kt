package app.eob.me.network

import app.eob.me.data.VeryfiHealthInsuranceEob
import com.google.gson.Gson

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
        val documentId = dto.id?.toString()?.takeIf { it.isNotBlank() } ?: documentRefId
        return VeryfiHealthInsuranceEob(
            documentId = documentId,
            blueprintName = firstNonBlank(dto.blueprintName)
                ?: VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB,
            insuranceCompanyName = firstNonBlank(
                dto.insuranceCompanyName,
                dto.insuranceCompany,
                dto.payerName
            ),
            memberName = dto.memberName?.trim()?.takeIf { it.isNotBlank() },
            memberId = firstNonBlank(dto.memberId, dto.memberNumber),
            patientName = dto.patientName?.trim()?.takeIf { it.isNotBlank() },
            claimId = firstNonBlank(dto.claimId, dto.claimNumber),
            inNetworkOutOfPocketBalance = firstNonNull(dto.inNetworkOutOfPocketBalance, dto.inNetworkOutOfPocket),
            outOfNetworkOutOfPocketBalance = firstNonNull(
                dto.outOfNetworkOutOfPocketBalance,
                dto.outOfNetworkOutOfPocket
            ),
            inNetworkDeductible = dto.inNetworkDeductible,
            outOfNetworkDeductible = dto.outOfNetworkDeductible,
            dateOfService = firstNonBlank(dto.dateOfService, dto.serviceDate),
            providerName = dto.providerName?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    fun toEobNormalizationFields(
        extraction: VeryfiHealthInsuranceEob,
        dto: VeryfiAnyDocResponseDto
    ): Map<String, Any?> {
        val lineItemText = dto.lineItems.orEmpty().joinToString(" ") { item ->
            listOfNotNull(item.description, item.cptCode).joinToString(" ")
        }
        return mapOf(
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
            "in_network_deductible" to extraction.inNetworkDeductible,
            "out_of_network_deductible" to extraction.outOfNetworkDeductible,
            "billed_amount" to dto.billedAmount,
            "insurance_paid" to dto.insurancePaid,
            "copay" to dto.copay,
            "deductible" to dto.deductible,
            "coinsurance" to dto.coinsurance,
            "blueprint_name" to extraction.blueprintName,
            "line_items" to lineItemText.takeIf { it.isNotBlank() }
        )
    }

    fun mergePayloadWithEobFields(
        payload: Map<String, Any?>,
        documentRefId: String
    ): Map<String, Any?> {
        val dto = parseResponse(payload)
        val extraction = toDomain(dto, documentRefId)
        val eobFields = toEobNormalizationFields(extraction, dto)
        return payload + eobFields.filterValues { value ->
            when (value) {
                null -> false
                is String -> value.isNotBlank()
                is Number -> true
                else -> true
            }
        }
    }

    fun mapFromUntypedPayload(
        payload: Map<String, Any?>,
        documentRefId: String
    ): VeryfiHealthInsuranceEob = toDomain(parseResponse(payload), documentRefId)

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun firstNonNull(vararg values: Double?): Double? {
        return values.firstOrNull { it != null }
    }
}
