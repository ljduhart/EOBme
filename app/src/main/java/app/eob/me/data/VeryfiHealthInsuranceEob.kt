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

/**
 * Appeal-ready fields extracted from a Veryfi `health_insurance_eob` response.
 * Populated from the reconciled [EobRecord] and [VeryfiHealthInsuranceEob] domain model.
 */
data class VeryfiExtractedData(
    val dateOfService: String,
    val cptCodes: List<String>,
    val patientResponsibility: Double,
    val copay: Double,
    val providerName: String = "",
    val insuranceCompanyName: String = ""
)

data class EobProcessedResult(
    val fileUrl: String,
    val veryfiData: VeryfiExtractedData
)

data class VeryfiAnyDocExtractionResult(
    val extraction: VeryfiHealthInsuranceEob,
    val record: EobRecord,
    val rawPayload: Map<String, Any?> = emptyMap(),
    val downloadUrl: String = ""
) {
    fun toVeryfiExtractedData(): VeryfiExtractedData {
        val cptCodes = record.charges
            .map { it.cptCode.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val resolvedCopay = record.totalCopayAmount.takeIf { it > 0.0 }
            ?: record.charges.map { it.copayAmount }.filter { it > 0.0 }.maxOrNull()
            ?: 0.0
        return VeryfiExtractedData(
            dateOfService = extraction.dateOfService.ifBlank { record.serviceDate },
            cptCodes = cptCodes,
            patientResponsibility = record.totalPatientResponsibility,
            copay = resolvedCopay,
            providerName = extraction.providerName.ifBlank { record.providerName },
            insuranceCompanyName = extraction.insuranceCompanyName.ifBlank { record.insuranceName }
        )
    }

    fun toProcessedResult(): EobProcessedResult = EobProcessedResult(
        fileUrl = downloadUrl,
        veryfiData = toVeryfiExtractedData()
    )
}

fun EobRecord.toVeryfiExtractedData(): VeryfiExtractedData {
    val cptCodes = charges
        .map { it.cptCode.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val resolvedCopay = totalCopayAmount.takeIf { it > 0.0 }
        ?: charges.map { it.copayAmount }.filter { it > 0.0 }.maxOrNull()
        ?: 0.0
    return VeryfiExtractedData(
        dateOfService = serviceDate,
        cptCodes = cptCodes,
        patientResponsibility = totalPatientResponsibility,
        copay = resolvedCopay,
        providerName = providerName,
        insuranceCompanyName = insuranceName
    )
}

sealed class VeryfiAnyDocExtractionState {
    data object Idle : VeryfiAnyDocExtractionState()
    data object Loading : VeryfiAnyDocExtractionState()
    data class Success(val result: VeryfiAnyDocExtractionResult) : VeryfiAnyDocExtractionState()
    data class Error(val message: String) : VeryfiAnyDocExtractionState()
}
