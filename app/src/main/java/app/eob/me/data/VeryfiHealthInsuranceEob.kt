package app.eob.me.data

/**
 * Domain model for a Veryfi AnyDocs `health_insurance_eob` extraction.
 * All blueprint fields are nullable so partial API responses never crash parsing.
 */
data class VeryfiHealthInsuranceEob(
    val documentId: String? = null,
    val blueprintName: String? = null,
    val insuranceCompanyName: String? = null,
    val memberName: String? = null,
    val memberId: String? = null,
    val patientName: String? = null,
    val claimId: String? = null,
    val inNetworkOutOfPocketBalance: Double? = null,
    val outOfNetworkOutOfPocketBalance: Double? = null,
    val inNetworkDeductible: Double? = null,
    val outOfNetworkDeductible: Double? = null,
    val dateOfService: String? = null,
    val providerName: String? = null
)

data class VeryfiAnyDocExtractionResult(
    val extraction: VeryfiHealthInsuranceEob,
    val record: EobRecord,
    val rawPayload: Map<String, Any?> = emptyMap()
)

sealed class VeryfiAnyDocExtractionState {
    data object Idle : VeryfiAnyDocExtractionState()
    data object Loading : VeryfiAnyDocExtractionState()
    data class Success(val result: VeryfiAnyDocExtractionResult) : VeryfiAnyDocExtractionState()
    data class Error(
        val message: String,
        val detail: String? = null
    ) : VeryfiAnyDocExtractionState()
}
