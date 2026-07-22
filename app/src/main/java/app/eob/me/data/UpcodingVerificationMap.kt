package app.eob.me.data

/**
 * Local E&M upcoding verification rules for high-complexity office visit CPT codes.
 */
object UpcodingVerificationMap {
    val upcodingVerificationRules = mapOf(
        "99204" to "45-59 minutes",
        "99205" to "60-74 minutes",
        "99214" to "30-39 minutes",
        "99215" to "40-54 minutes"
    )

    fun requiredTimeFor(cptCode: String): String? {
        val normalized = cptCode.trim()
        if (normalized.isBlank()) return null
        return upcodingVerificationRules[normalized]
    }
}
