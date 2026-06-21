package app.eob.me.network

import com.google.gson.annotations.SerializedName

/**
 * Typed contract for Veryfi AnyDocs `health_insurance_eob` blueprint responses.
 * Field names mirror the blueprint schema; Gson tolerates absent keys.
 */
data class VeryfiAnyDocResponseDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("blueprint_name") val blueprintName: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("insurance_company_name") val insuranceCompanyName: String? = null,
    @SerializedName("insurance_company") val insuranceCompany: String? = null,
    @SerializedName("payer_name") val payerName: String? = null,
    @SerializedName("member_name") val memberName: String? = null,
    @SerializedName("member_id") val memberId: String? = null,
    @SerializedName("member_number") val memberNumber: String? = null,
    @SerializedName("patient_name") val patientName: String? = null,
    @SerializedName("claim_id") val claimId: String? = null,
    @SerializedName("claim_number") val claimNumber: String? = null,
    @SerializedName("date_of_service") val dateOfService: String? = null,
    @SerializedName("service_date") val serviceDate: String? = null,
    @SerializedName("provider_name") val providerName: String? = null,
    @SerializedName("in_network_out_of_pocket_balance") val inNetworkOutOfPocketBalance: Double? = null,
    @SerializedName("in_network_out_of_pocket") val inNetworkOutOfPocket: Double? = null,
    @SerializedName("out_of_network_out_of_pocket_balance") val outOfNetworkOutOfPocketBalance: Double? = null,
    @SerializedName("out_of_network_out_of_pocket") val outOfNetworkOutOfPocket: Double? = null,
    @SerializedName("billed_amount") val billedAmount: Double? = null,
    @SerializedName("insurance_paid") val insurancePaid: Double? = null,
    @SerializedName("copay") val copay: Double? = null,
    @SerializedName("deductible") val deductible: Double? = null,
    @SerializedName("coinsurance") val coinsurance: Double? = null,
    @SerializedName("line_items") val lineItems: List<VeryfiAnyDocLineItemDto>? = null
)

data class VeryfiAnyDocLineItemDto(
    @SerializedName("description") val description: String? = null,
    @SerializedName("cpt_code") val cptCode: String? = null,
    @SerializedName("total") val total: Double? = null,
    @SerializedName("amount") val amount: Double? = null
)

/**
 * JSON request body for AnyDocs when submitting via [file_data] (Cloud Function proxy).
 */
data class VeryfiAnyDocRequestDto(
    @SerializedName("file_data") val fileData: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("blueprint_name") val blueprintName: String = VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB
)
