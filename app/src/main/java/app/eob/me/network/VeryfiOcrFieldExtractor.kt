package app.eob.me.network

import java.util.Locale

/**
 * Mirrors Veryfi dashboard Rules that run PCRE-style regex against a document's [ocr_text] and
 * populate custom fields (e.g. billed_amount, cpt, patient_responsibility). When blueprint fields
 * are absent from the AnyDocs JSON, this extractor backfills them from [custom_fields] and OCR.
 */
object VeryfiOcrFieldExtractor {
    private data class OcrFieldRule(
        val key: String,
        val aliases: Set<String> = emptySet(),
        val patterns: List<Regex>
    )

    private val amountCapture = """(\d+(?:,\d{3})*(?:\.\d{2})?)"""

    private val fieldRules = listOf(
        OcrFieldRule(
            key = "billed_amount",
            aliases = setOf("total_amount_billed", "total_billed"),
            patterns = listOf(
                Regex("""Billed Amount:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Total Billed:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Amount Billed:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Charges:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "insurance_paid",
            aliases = setOf("amount_paid", "plan_paid"),
            patterns = listOf(
                Regex("""Insurance Paid:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Plan Paid:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Payer Paid:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "contractual_adj",
            aliases = setOf("contractual_adjustment", "adjustment"),
            patterns = listOf(
                Regex("""Contractual Adjustment:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Contractual Adj(?:ustment)?\.?:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Adjustment:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "copay",
            aliases = setOf("co_pay"),
            patterns = listOf(
                Regex("""Copay:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Co-?Pay:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "deductible",
            patterns = listOf(
                Regex("""Deductible:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "coinsurance",
            patterns = listOf(
                Regex("""Coinsurance:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Co-?Insurance:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "patient_responsibility",
            aliases = setOf("patientResponsibility", "your_responsibility"),
            patterns = listOf(
                Regex("""Patient Responsibility:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Your Responsibility:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE),
                Regex("""Amount You Owe:\s*\$?$amountCapture""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "cpt",
            aliases = setOf("cpt_code", "cpt_codes", "cptCodes"),
            patterns = listOf(
                Regex("""CPT\s*-\s*(\d{5})""", RegexOption.IGNORE_CASE),
                Regex("""CPT(?:\s*Code)?\s*[:#-]?\s*(\d{5})""", RegexOption.IGNORE_CASE),
                Regex("""HCPCS\s*[:#-]?\s*([A-J]\d{4})""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "date_of_service",
            aliases = setOf("service_date", "dateOfService"),
            patterns = listOf(
                Regex("""Date of Service:\s*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""", RegexOption.IGNORE_CASE),
                Regex("""Service Date:\s*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "provider_name",
            aliases = setOf("provider", "rendering_provider"),
            patterns = listOf(
                Regex("""(?:Provider|Rendering Provider|Facility):\s*([^\n\r]{2,80})""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "insurance_name",
            aliases = setOf("insurance_company_name", "payer_name", "insurance_company"),
            patterns = listOf(
                Regex("""(?:Insurance|Payer|Plan)(?:\s+Company)?:\s*([^\n\r]{2,80})""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "claim_id",
            aliases = setOf("claim_number"),
            patterns = listOf(
                Regex("""Claim(?:\s*(?:ID|Number|#))?:\s*([A-Za-z0-9-]{4,})""", RegexOption.IGNORE_CASE)
            )
        ),
        OcrFieldRule(
            key = "member_id",
            aliases = setOf("member_number", "subscriber_id"),
            patterns = listOf(
                Regex("""Member(?:\s*(?:ID|Number|#))?:\s*([A-Za-z0-9-]{4,})""", RegexOption.IGNORE_CASE)
            )
        )
    )

    fun enrichPayload(payload: Map<String, Any?>): Map<String, Any?> {
        val ocrText = extractOcrText(payload)
        val customFields = extractCustomFields(payload)
        val regexFields = extractFromOcrText(ocrText)
        val enriched = payload.toMutableMap()

        customFields.forEach { (key, value) ->
            mergeField(enriched, key, value)
        }
        regexFields.forEach { (key, value) ->
            mergeField(enriched, key, value)
        }
        if (ocrText.isNotBlank()) {
            if (!enriched.hasMeaningfulString("ocr_text", "ocrText", "text")) {
                enriched["ocr_text"] = ocrText
            }
        }
        propagateCptAliases(enriched)
        return enriched
    }

    fun extractOcrText(payload: Map<String, Any?>): String {
        return payload.stringValue("ocr_text", "ocrText", "text", "raw_text")
    }

    fun extractCustomFields(payload: Map<String, Any?>): Map<String, Any?> {
        val raw = payload["custom_fields"] ?: payload["customFields"] ?: return emptyMap()
        return when (raw) {
            is Map<*, *> -> raw.entries.mapNotNull { (key, value) ->
                val fieldKey = key?.toString()?.trim().orEmpty()
                if (fieldKey.isBlank()) return@mapNotNull null
                fieldKey to unwrapCustomFieldValue(value)
            }.toMap()
            is List<*> -> raw.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val fieldKey = listOf("name", "key", "field", "id")
                    .firstNotNullOfOrNull { candidate ->
                        map[candidate]?.toString()?.trim()?.takeIf { it.isNotBlank() }
                    }.orEmpty()
                if (fieldKey.isBlank()) return@mapNotNull null
                fieldKey to unwrapCustomFieldValue(map["value"] ?: map["text"] ?: map["content"])
            }.toMap()
            else -> emptyMap()
        }.filterValues { value -> value != null }
    }

    fun extractFromOcrText(ocrText: String): Map<String, Any?> {
        if (ocrText.isBlank()) return emptyMap()
        val extracted = linkedMapOf<String, Any?>()
        fieldRules.forEach { rule ->
            val match = rule.patterns.firstNotNullOfOrNull { pattern ->
                pattern.find(ocrText)?.groupValues?.getOrNull(1)?.trim()
            } ?: return@forEach
            if (rule.key == "cpt") {
                extracted["cpt"] = match.uppercase(Locale.US)
            } else if (isAmountRule(rule.key)) {
                parseAmount(match)?.let { extracted[rule.key] = it }
            } else {
                extracted[rule.key] = match
            }
        }
        return extracted
    }

    private fun mergeField(target: MutableMap<String, Any?>, key: String, value: Any?) {
        if (value == null) return
        val rule = fieldRules.firstOrNull { it.key == key || key in it.aliases }
        val canonicalKey = rule?.key ?: key
        if (target.hasMeaningfulValue(canonicalKey, rule?.aliases.orEmpty())) return
        when {
            isAmountRule(canonicalKey) -> {
                val amount = parseAmount(value) ?: return
                if (amount > 0.0) target[canonicalKey] = amount
            }
            canonicalKey == "cpt" -> {
                val code = value.toString().trim().uppercase(Locale.US)
                if (code.isNotBlank()) target["cpt"] = code
            }
            else -> {
                val text = value.toString().trim()
                if (text.isNotBlank()) target[canonicalKey] = text
            }
        }
    }

    private fun propagateCptAliases(target: MutableMap<String, Any?>) {
        val cpt = target.stringValue("cpt", "cpt_code", "cpt_codes", "cptCodes")
        if (cpt.isBlank()) return
        if (!target.hasMeaningfulString("cpt_codes", "cptCodes", "cpt_code", "cptCode")) {
            target["cpt_codes"] = cpt
            target["cptCodes"] = cpt
        }
    }

    private fun unwrapCustomFieldValue(value: Any?): Any? {
        return when (value) {
            null -> null
            is Map<*, *> -> value["value"] ?: value["text"] ?: value["content"]
            is List<*> -> value.firstOrNull()?.let { unwrapCustomFieldValue(it) }
            else -> value
        }
    }

    private fun isAmountRule(key: String): Boolean {
        return key in setOf(
            "billed_amount",
            "insurance_paid",
            "contractual_adj",
            "copay",
            "deductible",
            "coinsurance",
            "patient_responsibility"
        )
    }

    private fun parseAmount(value: Any?): Double? {
        return when (value) {
            is Number -> value.toDouble().takeIf { it.isFinite() }
            is String -> value.replace("$", "").replace(",", "").trim().toDoubleOrNull()
            else -> null
        }
    }

    private fun Map<String, Any?>.hasMeaningfulValue(key: String, aliases: Set<String>): Boolean {
        val keys = (aliases + key).toSet()
        return keys.any { candidate ->
            when (val value = this[candidate]) {
                null -> false
                is Number -> value.toDouble() > 0.0
                is String -> value.trim().isNotBlank()
                else -> true
            }
        }
    }

    private fun Map<String, Any?>.hasMeaningfulString(vararg keys: String): Boolean {
        return keys.any { key ->
            this[key]?.toString()?.trim()?.isNotBlank() == true
        }
    }

    private fun Map<String, Any?>.stringValue(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key ->
            this[key]?.toString()?.trim()?.takeIf { it.isNotBlank() }
        }.orEmpty()
    }
}
