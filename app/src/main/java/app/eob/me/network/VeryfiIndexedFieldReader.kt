package app.eob.me.network

import app.eob.me.network.dto.VeryfiIndexedServiceLineDto

/**
 * Thread-safe reader for Veryfi's flat indexed keys (`field_1` … `field_8`).
 * Uses precompiled regex — no reflection.
 */
object VeryfiIndexedFieldReader {
    const val MAX_SERVICE_LINE_INDEX = 8

    internal val CODE_BASE_KEYS = listOf("cpt_code", "cptCode", "code")
    internal val DESCRIPTION_BASE_KEYS = listOf("service_description", "description", "cpt_description")
    internal val DATE_BASE_KEYS = listOf("service_date", "date_of_service", "serviceDate")
    internal val TOTAL_BILLED_BASE_KEYS = listOf("total_amount_billed", "totalAmountBilled")
    internal val BILLED_BASE_KEYS = listOf("amount_billed", "billed_amount", "amountBilled")
    internal val ALLOWED_BASE_KEYS = listOf("allowed_amount", "allowedAmount")
    internal val INSURANCE_PAID_BASE_KEYS = listOf("health_plan_responsibility", "insurance_paid", "insurancePaid")
    internal val CONTRACTUAL_BASE_KEYS = listOf("contractual_adjustment", "contractualAdjustment", "contractual_adj")
    internal val COPAY_BASE_KEYS = listOf("copay_amount", "copay", "co_pay")
    internal val DEDUCTIBLE_BASE_KEYS = listOf("deductible_amount", "deductible")
    internal val COINSURANCE_BASE_KEYS = listOf("coinsurance_amount", "coinsurance", "co_insurance")
    internal val PATIENT_RESP_BASE_KEYS = listOf("patient_responsibility", "patientResponsibility")

    /**
     * Discovers 1-based indices that have a non-blank procedure code (`cpt_code_N`).
     * Financial-only keys such as `copay_amount_2` without a matching code are ignored.
     */
    fun discoverIndices(fieldMap: VeryfiIndexedServiceLineDto): List<Int> {
        return (1..MAX_SERVICE_LINE_INDEX).filter { index ->
            stringValue(fieldMap, CODE_BASE_KEYS, index).isNotBlank()
        }
    }

    /**
     * Resolves the service date for [index], falling back to the nearest prior indexed date in the
     * same Veryfi row when `service_date_N` is absent (common for columns 4–8 on multi-CPT EOBs).
     */
    fun resolveServiceDateIso(fieldMap: VeryfiIndexedServiceLineDto, index: Int): String {
        for (candidate in index downTo 1) {
            val iso = VeryfiDateNormalizer.toIsoDate(stringValue(fieldMap, DATE_BASE_KEYS, candidate))
            if (iso.isNotBlank()) return iso
        }
        return ""
    }

    fun stringValue(fieldMap: VeryfiIndexedServiceLineDto, baseKeys: List<String>, index: Int): String {
        val suffix = "_$index"
        baseKeys.forEach { baseKey ->
            val indexed = fieldMap["$baseKey$suffix"] ?: fieldMap["$baseKey$index"]
            val value = indexed?.toString()?.trim()
            if (!value.isNullOrBlank()) return value
        }
        if (index == 1) {
            baseKeys.forEach { baseKey ->
                val value = fieldMap[baseKey]?.toString()?.trim()
                if (!value.isNullOrBlank()) return value
            }
        }
        return ""
    }

    fun moneyValue(fieldMap: VeryfiIndexedServiceLineDto, baseKeys: List<String>, index: Int): Double {
        val suffix = "_$index"
        baseKeys.forEach { baseKey ->
            val indexed = fieldMap["$baseKey$suffix"] ?: fieldMap["$baseKey$index"]
            if (indexed != null) return VeryfiCurrencyParser.parse(indexed)
        }
        if (index == 1) {
            baseKeys.forEach { baseKey ->
                if (fieldMap[baseKey] != null) return VeryfiCurrencyParser.parse(fieldMap[baseKey])
            }
        }
        return 0.0
    }

    fun moneyValue(fieldMap: Map<String, Any?>, keys: List<String>): Double {
        keys.forEach { key ->
            if (fieldMap[key] != null) return VeryfiCurrencyParser.parse(fieldMap[key])
        }
        return 0.0
    }

    fun stringValue(fieldMap: Map<String, Any?>, keys: List<String>): String {
        keys.forEach { key ->
            val value = fieldMap[key]?.toString()?.trim()
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }
}
