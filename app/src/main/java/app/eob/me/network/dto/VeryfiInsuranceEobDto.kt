package app.eob.me.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Raw Veryfi `health_insurance_eob` nested response DTO.
 *
 * Veryfi enforces a flat, indexed schema inside each `service_lines` row (`cpt_code_1` … `cpt_code_8`).
 * Those dynamic columns are modeled as [Map] entries and unpivoted by [VeryfiInsuranceEobMapper].
 */
data class VeryfiInsuranceEobResponseDto(
    @SerializedName("group_name") val groupName: String? = null,
    @SerializedName("groupName") val groupNameCamel: String? = null,
    @SerializedName("payer_name") val payerName: String? = null,
    @SerializedName("payerName") val payerNameCamel: String? = null,
    @SerializedName("benefit_type") val benefitType: String? = null,
    @SerializedName("benefitType") val benefitTypeCamel: String? = null,
    @SerializedName("group_number") val groupNumber: String? = null,
    @SerializedName("groupNumber") val groupNumberCamel: String? = null,
    @SerializedName("subscriber_id") val subscriberId: String? = null,
    @SerializedName("subscriberId") val subscriberIdCamel: String? = null,
    @SerializedName("member_id") val memberId: String? = null,
    @SerializedName("subscriber_name") val subscriberName: String? = null,
    @SerializedName("subscriberName") val subscriberNameCamel: String? = null,
    @SerializedName("patient_name") val patientName: String? = null,
    @SerializedName("benefit_year_max_remaining") val benefitYearMaxRemaining: Any? = null,
    @SerializedName("orthodontia_max_remaining") val orthodontiaMaxRemaining: Any? = null,
    @SerializedName("claims") val claims: List<VeryfiInsuranceClaimDto>? = null,
    @SerializedName("Claims") val claimsPascal: List<VeryfiInsuranceClaimDto>? = null
) {
    fun resolvedClaims(): List<VeryfiInsuranceClaimDto> =
        claims.orEmpty().ifEmpty { claimsPascal.orEmpty() }
}

data class VeryfiInsuranceClaimDto(
    @SerializedName("provider_name") val providerName: String? = null,
    @SerializedName("providerName") val providerNameCamel: String? = null,
    @SerializedName("claim_number_1") val claimNumber1: String? = null,
    @SerializedName("claim_number_2") val claimNumber2: String? = null,
    @SerializedName("claim_number") val claimNumber: String? = null,
    @SerializedName("claim_id") val claimId: String? = null,
    @SerializedName("processed_date") val processedDate: String? = null,
    @SerializedName("processedDate") val processedDateCamel: String? = null,
    @SerializedName("claim_totals") val claimTotals: Map<String, Any?>? = null,
    @SerializedName("claimTotals") val claimTotalsCamel: Map<String, Any?>? = null,
    @SerializedName("service_lines") val serviceLines: List<VeryfiIndexedServiceLineDto>? = null,
    @SerializedName("serviceLines") val serviceLinesCamel: List<VeryfiIndexedServiceLineDto>? = null
) {
    fun resolvedServiceLines(): List<VeryfiIndexedServiceLineDto> =
        serviceLines.orEmpty().ifEmpty { serviceLinesCamel.orEmpty() }

    fun resolvedClaimTotals(): Map<String, Any?> =
        claimTotals.orEmpty().ifEmpty { claimTotalsCamel.orEmpty() }
}

/**
 * One Veryfi service-line row containing numbered suffix columns up to index 8.
 * Keys are preserved verbatim so the indexed-field reader can unpivot without reflection.
 */
typealias VeryfiIndexedServiceLineDto = Map<String, @JvmSuppressWildcards Any?>
