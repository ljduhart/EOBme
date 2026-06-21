package app.eob.me.data

/**
 * Domain model for a Veryfi AnyDocs `health_insurance_eob` extraction.
 */
data class VeryfiHealthInsuranceEob(
    val documentId: String,
    val blueprintName: String,
    val insuranceCompanyName: String,
    val memberName: String,
    val memberId: String,
    val patientName: String,
    val claimId: String,
    val inNetworkOutOfPocketBalance: Double,
    val outOfNetworkOutOfPocketBalance: Double,
    val dateOfService: String = "",
    val providerName: String = ""
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
    data class Error(val message: String) : VeryfiAnyDocExtractionState()
}
