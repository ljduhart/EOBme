package app.eob.me.network

object VeryfiAnyDocConstants {
    const val BASE_URL = "https://api.veryfi.com/api/v8/"
    /** Standard Veryfi documents endpoint; blueprint_name routes to health_insurance_eob. */
    const val ANY_DOCUMENTS_PATH = "partner/documents/"
    const val BLUEPRINT_HEALTH_INSURANCE_EOB = "health_insurance_eob"
    const val DOCUMENT_TYPE_EOB = "eob"
    const val CATEGORY_INSURANCE = "insurance"
    val CATEGORIES_INSURANCE: List<String> = listOf(CATEGORY_INSURANCE)
    const val EXTRACT_VERYFI_HYBRID_STREAM = "extractVeryfiHybridStream"
    const val HYBRID_STREAM_TIMEOUT_SECONDS = 120L
}
