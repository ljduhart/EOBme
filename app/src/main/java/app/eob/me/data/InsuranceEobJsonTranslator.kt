package app.eob.me.data

import app.eob.me.network.VeryfiAnyDocMapper
import java.util.Locale

/**
 * Translates nested health insurance EOB JSON (group → claims → service_lines with numbered
 * suffix columns) into flat [EobRecord] + [EobCharge] maps consumed by [FirebaseEobMapper].
 */
object InsuranceEobJsonTranslator {
    private val INDEX_SUFFIX = Regex("""_(\d+)$""")
    private val PROCEDURE_CODE = Regex("""^[A-J][0-9]{4}$""", RegexOption.IGNORE_CASE)

    data class TranslationResult(
        val mergedRecord: EobRecord,
        val claimRecords: List<EobRecord>,
        val flattenedPayload: Map<String, Any?>
    )

    fun isNestedInsuranceEobPayload(payload: Map<String, Any?>): Boolean {
        val claims = payload["claims"] ?: payload["Claims"]
        return claims is List<*> && claims.isNotEmpty()
    }

    fun translate(
        payload: Map<String, Any?>,
        documentRefId: String,
        sourceName: String
    ): TranslationResult? {
        if (!isNestedInsuranceEobPayload(payload)) return null
        val claims = payload.claimMaps("claims", "Claims")
        if (claims.isEmpty()) return null

        val documentId = HybridDocumentRef.stableDocumentId(documentRefId)
        val payerName = payload.stringField("payer_name", "payerName", "insurance_name", "insuranceName")
        val groupName = payload.stringField("group_name", "groupName")
        val groupNumber = payload.stringField("group_number", "groupNumber")
        val subscriberId = payload.stringField("subscriber_id", "subscriberId", "member_id", "memberId")
        val subscriberName = payload.stringField("subscriber_name", "subscriberName", "patient_name", "patientName")
        val benefitType = payload.stringField("benefit_type", "benefitType")

        val claimRecords = claims.mapIndexed { index, claim ->
            claimToRecord(
                claim = claim,
                claimIndex = index,
                documentId = documentId,
                sourceName = sourceName,
                payerName = payerName,
                groupName = groupName,
                groupNumber = groupNumber,
                subscriberId = subscriberId,
                subscriberName = subscriberName,
                benefitType = benefitType,
                firestoreIdSuffix = if (claims.size > 1) "_claim_$index" else ""
            )
        }

        val mergedCharges = claimRecords.flatMap { it.charges }
        val mergedRecord = mergeClaimRecords(
            claimRecords = claimRecords,
            documentId = documentId,
            sourceName = sourceName,
            payerName = payerName,
            rawText = payload.rawTextField()
        )

        val flattenedPayload = buildFlattenedPayload(
            original = payload,
            mergedRecord = mergedRecord,
            claimRecords = claimRecords
        )

        return TranslationResult(
            mergedRecord = mergedRecord.copy(firestoreId = documentId),
            claimRecords = claimRecords.mapIndexed { index, record ->
                record.copy(
                    firestoreId = if (claims.size > 1) "${documentId}_claim_$index" else documentId
                )
            },
            flattenedPayload = flattenedPayload
        )
    }

    /**
     * Merges nested insurance EOB JSON into a flat payload so existing Veryfi OCR merge + Firestore
     * normalization can consume financial fields when the structure is not detected early.
     */
    fun enrichPayloadWithNestedClaims(
        payload: Map<String, Any?>,
        documentRefId: String,
        sourceName: String
    ): Map<String, Any?>? {
        val translation = translate(payload, documentRefId, sourceName) ?: return null
        return payload + translation.flattenedPayload + mapOf(
            "insuranceEobTranslated" to true,
            "charges" to translation.mergedRecord.charges.map(::chargeToMap)
        )
    }

    internal fun parseMoney(value: Any?): Double {
        return when (value) {
            null -> 0.0
            is Number -> value.toDouble()
            is String -> parseMoneyString(value)
            else -> parseMoneyString(value.toString())
        }
    }

    internal fun parseMoneyString(raw: String): Double {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return 0.0
        val normalized = trimmed
            .replace("$", "")
            .replace("USD", "", ignoreCase = true)
            .trim()
        if (normalized.isBlank()) return 0.0

        val commaCount = normalized.count { it == ',' }
        val dotCount = normalized.count { it == '.' }
        val cleaned = when {
            commaCount == 1 && dotCount == 0 -> {
                // European style: 511,42
                normalized.replace(",", ".")
            }
            commaCount >= 1 && dotCount == 1 -> {
                // US style: 1,578.00
                normalized.replace(",", "")
            }
            else -> normalized.replace(",", "")
        }
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    internal fun normalizeServiceDate(raw: Any?): String {
        val trimmed = raw?.toString()?.trim().orEmpty()
        if (trimmed.isBlank()) return "Date not recognized"
        val shortYear = Regex("""^(\d{1,2})/(\d{1,2})/(\d{2})$""").find(trimmed)
        if (shortYear != null) {
            val month = shortYear.groupValues[1].padStart(2, '0')
            val day = shortYear.groupValues[2].padStart(2, '0')
            val yearSuffix = shortYear.groupValues[3]
            val year = if (yearSuffix.toInt() >= 70) "19$yearSuffix" else "20$yearSuffix"
            return "$month/$day/$year"
        }
        return trimmed
    }

    private fun claimToRecord(
        claim: Map<String, Any?>,
        claimIndex: Int,
        documentId: String,
        sourceName: String,
        payerName: String,
        groupName: String,
        groupNumber: String,
        subscriberId: String,
        subscriberName: String,
        benefitType: String,
        firestoreIdSuffix: String
    ): EobRecord {
        val providerName = claim.stringField("provider_name", "providerName")
        val claimId = claim.stringField(
            "claim_number_1",
            "claim_number_2",
            "claim_number",
            "claim_id",
            "claimId"
        )
        val processedDate = normalizeServiceDate(
            claim.stringField("processed_date", "processedDate", "date_of_service", "service_date")
        )
        val totals = claim.mapField("claim_totals", "claimTotals")

        val charges = claim.serviceLineMaps("service_lines", "serviceLines")
            .flatMap { unpivotServiceLine(it) }
            .ifEmpty {
                unpivotServiceLine(claim)
            }

        val resolvedCharges = charges.ifEmpty {
            listOf(syntheticChargeFromTotals(totals, processedDate))
        }

        val totalBilled = firstPositive(
            totals.parseMoney("total_billed_1", "totalBilled1", "total_amount_billed_1"),
            resolvedCharges.sumOf { it.billedAmount }
        )
        val totalInsurancePaid = firstPositive(
            totals.parseMoney(
                "total_health_plan_responsibility_1",
                "health_plan_responsibility",
                "total_health_plan_responsibility"
            ),
            resolvedCharges.sumOf { it.insurancePaidAmount }
        )
        val totalContractualAdj = firstPositive(
            totals.parseMoney("total_contractual_adjustment_1", "totalContractualAdjustment1"),
            resolvedCharges.sumOf { it.contractualAdjustmentAmount }
        )
        val totalCopay = resolvedCharges.sumOf { it.copayAmount }
        val totalDeductible = resolvedCharges.sumOf { it.deductibleAmount }
        val totalCoinsurance = resolvedCharges.sumOf { it.coinsuranceAmount }
        val patientResponsibility = firstPositive(
            totals.parseMoney("total_patient_responsibility_1", "patient_responsibility_1"),
            totalCopay + totalDeductible + totalCoinsurance
        )

        val primaryServiceDate = resolvedCharges
            .map { it.serviceDate }
            .firstOrNull { it.isNotBlank() && it != "Date not recognized" }
            ?: processedDate

        val normalizedFields = mapOf<String, Any?>(
            "id" to documentId,
            "sourceName" to sourceName.ifBlank { "Veryfi" },
            "provider_name" to providerName,
            "insurance_name" to payerName,
            "payer_name" to payerName,
            "group_name" to groupName.ifBlank { groupNumber },
            "group_number" to groupNumber,
            "member_id" to subscriberId,
            "member_name" to subscriberName,
            "patient_name" to subscriberName,
            "claim_id" to claimId,
            "benefit_type" to benefitType,
            "date_of_service" to primaryServiceDate,
            "processed_date" to processedDate,
            "billed_amount" to totalBilled,
            "insurance_paid" to totalInsurancePaid,
            "contractual_adj" to totalContractualAdj,
            "copay" to totalCopay,
            "deductible" to totalDeductible,
            "coinsurance" to totalCoinsurance,
            "patient_responsibility" to patientResponsibility,
            "claim_index" to claimIndex,
            "charges" to resolvedCharges.map(::chargeToMap),
            "cptCodes" to resolvedCharges.joinToString(",") { it.cptCode },
            "rawText" to claim.toString()
        )

        return FirebaseEobMapper.eobFromMap(normalizedFields, documentId + firestoreIdSuffix)
    }

    private fun mergeClaimRecords(
        claimRecords: List<EobRecord>,
        documentId: String,
        sourceName: String,
        payerName: String,
        rawText: String
    ): EobRecord {
        if (claimRecords.size == 1) {
            return claimRecords.first()
        }
        val allCharges = claimRecords.flatMap { it.charges }
        val earliestSortKey = claimRecords.minOfOrNull { it.serviceDateSortKey } ?: 0
        val primaryDate = allCharges
            .map { it.serviceDate }
            .firstOrNull { it.isNotBlank() && it != "Date not recognized" }
            ?: claimRecords.first().serviceDate
        val providers = claimRecords.map { it.providerName }.filter { it.isNotBlank() }.distinct()
        val mergedFields = mapOf<String, Any?>(
            "id" to documentId,
            "sourceName" to sourceName.ifBlank { "Veryfi" },
            "provider_name" to providers.joinToString(" / "),
            "insurance_name" to payerName.ifBlank { claimRecords.first().insuranceName },
            "date_of_service" to primaryDate,
            "billed_amount" to claimRecords.sumOf { it.totalBilledAmount },
            "insurance_paid" to claimRecords.sumOf { it.totalInsurancePaidAmount },
            "contractual_adj" to claimRecords.sumOf { it.totalContractualAdjustmentAmount },
            "copay" to claimRecords.sumOf { it.totalCopayAmount },
            "deductible" to claimRecords.sumOf { it.totalDeductibleAmount },
            "coinsurance" to claimRecords.sumOf { it.totalCoinsuranceAmount },
            "patient_responsibility" to claimRecords.sumOf { it.totalPatientResponsibility },
            "charges" to allCharges.map(::chargeToMap),
            "cptCodes" to allCharges.joinToString(",") { it.cptCode },
            "rawText" to rawText,
            "serviceDateSortKey" to earliestSortKey
        )
        return FirebaseEobMapper.eobFromMap(mergedFields, documentId)
    }

    private fun unpivotServiceLine(serviceLine: Map<String, Any?>): List<EobCharge> {
        val indices = linkedSetOf<Int>()
        serviceLine.keys.forEach { key ->
            val match = INDEX_SUFFIX.find(key) ?: return@forEach
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let(indices::add)
        }
        if (indices.isEmpty()) {
            val singleCode = serviceLine.stringField("cpt_code", "cptCode", "code")
            if (singleCode.isBlank()) return emptyList()
            return listOf(chargeFromIndexedFields(serviceLine, suffix = ""))
        }
        return indices.sorted().mapNotNull { index ->
            val code = serviceLine.stringField("cpt_code_$index", "cptCode$index", "code_$index")
                .ifBlank {
                    serviceLine.stringField("cpt_code$index")
                }
            if (code.isBlank()) return@mapNotNull null
            chargeFromIndexedFields(serviceLine, suffix = "_$index")
        }
    }

    private fun chargeFromIndexedFields(line: Map<String, Any?>, suffix: String): EobCharge {
        val code = line.stringField("cpt_code$suffix", "cptCode$suffix", "code$suffix")
            .trim()
            .uppercase(Locale.US)
        val description = line.stringField(
            "service_description$suffix",
            "description$suffix",
            "cpt_description$suffix"
        )
        val fallback = EobKnowledgeBase.cptInfoFor(code)
        val serviceDate = normalizeServiceDate(
            line.stringField("service_date$suffix", "date_of_service$suffix", "serviceDate$suffix")
        )
        val billedAmount = firstPositive(
            line.parseMoney("total_amount_billed$suffix"),
            line.parseMoney("amount_billed$suffix", "billed_amount$suffix"),
            line.parseMoney("allowed_amount$suffix")
        )
        val copayAmount = line.parseMoney("copay_amount$suffix", "copay$suffix")
        val deductibleAmount = line.parseMoney("deductible_amount$suffix", "deductible$suffix")
        var coinsuranceAmount = line.parseMoney("coinsurance_amount$suffix", "coinsurance$suffix")
        val patientResponsibility = line.parseMoney("patient_responsibility$suffix")
        if (patientResponsibility > 0.0 && copayAmount + deductibleAmount + coinsuranceAmount <= 0.0) {
            coinsuranceAmount = patientResponsibility
        }
        return EobCharge(
            cptCode = code,
            cptDescription = description.ifBlank { fallback.description },
            category = fallback.category,
            billedAmount = billedAmount,
            insurancePaidAmount = line.parseMoney("health_plan_responsibility$suffix"),
            contractualAdjustmentAmount = line.parseMoney("contractual_adjustment$suffix"),
            copayAmount = copayAmount,
            deductibleAmount = deductibleAmount,
            coinsuranceAmount = coinsuranceAmount,
            serviceDate = serviceDate
        )
    }

    private fun syntheticChargeFromTotals(totals: Map<String, Any?>, serviceDate: String): EobCharge {
        val fallback = EobKnowledgeBase.cptInfoFor("")
        return EobCharge(
            cptCode = "",
            cptDescription = "Claim total",
            category = fallback.category,
            billedAmount = totals.parseMoney("total_billed_1", "total_amount_billed_1"),
            insurancePaidAmount = totals.parseMoney("total_health_plan_responsibility_1", "health_plan_responsibility"),
            contractualAdjustmentAmount = totals.parseMoney("total_contractual_adjustment_1"),
            copayAmount = 0.0,
            deductibleAmount = 0.0,
            coinsuranceAmount = 0.0,
            serviceDate = serviceDate
        )
    }

    private fun buildFlattenedPayload(
        original: Map<String, Any?>,
        mergedRecord: EobRecord,
        claimRecords: List<EobRecord>
    ): Map<String, Any?> {
        return mapOf(
            "provider_name" to mergedRecord.providerName,
            "insurance_name" to mergedRecord.insuranceName,
            "payer_name" to mergedRecord.insuranceName,
            "date_of_service" to mergedRecord.serviceDate,
            "billed_amount" to mergedRecord.totalBilledAmount,
            "insurance_paid" to mergedRecord.totalInsurancePaidAmount,
            "contractual_adj" to mergedRecord.totalContractualAdjustmentAmount,
            "copay" to mergedRecord.totalCopayAmount,
            "deductible" to mergedRecord.totalDeductibleAmount,
            "coinsurance" to mergedRecord.totalCoinsuranceAmount,
            "patient_responsibility" to mergedRecord.totalPatientResponsibility,
            "cptCodes" to mergedRecord.charges.joinToString(",") { it.cptCode },
            "charges" to mergedRecord.charges.map(::chargeToMap),
            "group_name" to original.stringField("group_name", "groupName"),
            "group_number" to original.stringField("group_number", "groupNumber"),
            "subscriber_id" to original.stringField("subscriber_id", "subscriberId"),
            "subscriber_name" to original.stringField("subscriber_name", "subscriberName"),
            "benefit_type" to original.stringField("benefit_type", "benefitType"),
            "benefit_year_max_remaining" to original.parseMoney("benefit_year_max_remaining"),
            "orthodontia_max_remaining" to original.parseMoney("orthodontia_max_remaining"),
            "claim_count" to claimRecords.size,
            "claims" to original["claims"]
        )
    }

    private fun chargeToMap(charge: EobCharge): Map<String, Any?> {
        return mapOf(
            "cptCode" to charge.cptCode,
            "cpt_code" to charge.cptCode,
            "cptDescription" to charge.cptDescription,
            "description" to charge.cptDescription,
            "category" to charge.category.name,
            "billedAmount" to charge.billedAmount,
            "billed_amount" to charge.billedAmount,
            "insurancePaidAmount" to charge.insurancePaidAmount,
            "insurance_paid" to charge.insurancePaidAmount,
            "contractualAdjustmentAmount" to charge.contractualAdjustmentAmount,
            "contractual_adj" to charge.contractualAdjustmentAmount,
            "copayAmount" to charge.copayAmount,
            "copay" to charge.copayAmount,
            "deductibleAmount" to charge.deductibleAmount,
            "deductible" to charge.deductibleAmount,
            "coinsuranceAmount" to charge.coinsuranceAmount,
            "coinsurance" to charge.coinsuranceAmount,
            "serviceDate" to charge.serviceDate,
            "date_of_service" to charge.serviceDate
        )
    }

    private fun Map<String, Any?>.claimMaps(vararg keys: String): List<Map<String, Any?>> {
        val raw = keys.firstNotNullOfOrNull { key -> this[key] } ?: return emptyList()
        if (raw !is List<*>) return emptyList()
        return raw.mapNotNull { item ->
            when (item) {
                is Map<*, *> -> item.entries.associate { (key, value) -> key.toString() to value }
                else -> null
            }
        }
    }

    private fun Map<String, Any?>.serviceLineMaps(vararg keys: String): List<Map<String, Any?>> {
        val raw = keys.firstNotNullOfOrNull { key -> this[key] } ?: return emptyList()
        if (raw !is List<*>) return emptyList()
        return raw.mapNotNull { item ->
            when (item) {
                is Map<*, *> -> item.entries.associate { (key, value) -> key.toString() to value }
                else -> null
            }
        }
    }

    private fun Map<String, Any?>.mapField(vararg keys: String): Map<String, Any?> {
        val raw = keys.firstNotNullOfOrNull { key -> this[key] } ?: return emptyMap()
        return when (raw) {
            is Map<*, *> -> raw.entries.associate { (key, value) -> key.toString() to value }
            else -> emptyMap()
        }
    }

    private fun Map<String, Any?>.stringField(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key ->
            this[key]?.toString()?.trim()?.takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    private fun Map<String, Any?>.parseMoney(vararg keys: String): Double {
        val raw = keys.firstNotNullOfOrNull { key -> this[key] } ?: return 0.0
        return parseMoney(raw)
    }

    private fun Map<String, Any?>.rawTextField(): String {
        return stringField("rawText", "raw_text", "ocr_text", "ocrText")
            .ifBlank { VeryfiAnyDocMapper.parseResponse(this).ocrText.orEmpty() }
    }

    private fun firstPositive(vararg values: Double): Double {
        return values.firstOrNull { it > 0.0 } ?: 0.0
    }

    internal fun isProcedureCode(code: String): Boolean {
        return PROCEDURE_CODE.matches(code.trim())
    }
}
