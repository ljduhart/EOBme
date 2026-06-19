package app.eob.me.util

import app.eob.me.data.CameraScanDocumentType

/**
 * On-device keyword gate for scanned documents before cloud upload.
 */
object EobDocumentOcrPreCheck {
    private val EOB_KEYWORDS = listOf(
        "eob",
        "explanation of benefits",
        "benefit",
        "claim",
        "insurance",
        "deductible",
        "coinsurance",
        "copay",
        "co-pay",
        "patient responsibility",
        "member id",
        "member number",
        "provider",
        "billed",
        "allowed amount",
        "remittance",
        "payer"
    )

    private val RECEIPT_KEYWORDS = listOf(
        "receipt",
        "invoice",
        "amount due",
        "amount paid",
        "payment",
        "balance due"
    )

    private val MEDICAL_INDICATORS = listOf(
        "medical",
        "patient",
        "provider",
        "service",
        "visit",
        "clinic",
        "hospital",
        "pharmacy",
        "physician",
        "doctor",
        "copay",
        "eob"
    )

    data class Result(
        val passed: Boolean,
        val matchedKeywords: List<String>,
        val preview: String
    )

    fun validate(ocrText: String): Result = validateForScanType(ocrText, CameraScanDocumentType.Eob)

    fun validateForScanType(ocrText: String, scanType: CameraScanDocumentType): Result {
        val normalized = ocrText.lowercase()
        val matched = when (scanType) {
            CameraScanDocumentType.Eob -> {
                EOB_KEYWORDS.filter { keyword -> normalized.contains(keyword) }
            }
            CameraScanDocumentType.Receipt -> {
                val eobMatches = EOB_KEYWORDS.filter { keyword -> normalized.contains(keyword) }
                if (eobMatches.isNotEmpty()) {
                    eobMatches
                } else {
                    val receiptMatches = RECEIPT_KEYWORDS.filter { keyword -> normalized.contains(keyword) }
                    val medicalMatches = MEDICAL_INDICATORS.filter { keyword -> normalized.contains(keyword) }
                    if (receiptMatches.isNotEmpty() && medicalMatches.isNotEmpty()) {
                        receiptMatches + medicalMatches
                    } else {
                        emptyList()
                    }
                }
            }
        }.distinct()
        return Result(
            passed = matched.isNotEmpty(),
            matchedKeywords = matched,
            preview = ocrText.trim().take(500)
        )
    }
}
