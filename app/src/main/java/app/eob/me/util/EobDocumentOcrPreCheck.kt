package app.eob.me.util

/**
 * On-device keyword gate for scanned EOB documents before cloud upload.
 */
object EobDocumentOcrPreCheck {
    private val KEYWORDS = listOf(
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

    data class Result(
        val passed: Boolean,
        val matchedKeywords: List<String>,
        val preview: String
    )

    fun validate(ocrText: String): Result {
        val normalized = ocrText.lowercase()
        val matched = KEYWORDS.filter { keyword -> normalized.contains(keyword) }
        return Result(
            passed = matched.isNotEmpty(),
            matchedKeywords = matched,
            preview = ocrText.trim().take(500)
        )
    }
}
