package app.eob.me.data

import app.eob.me.network.VeryfiOcrFieldExtractor
import app.eob.me.network.VeryfiInsuranceEobPayloadParser
import app.eob.me.network.toNormalizedInsuranceEob
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirebaseEobMapper {
    fun profileToMap(profile: UserProfile): Map<String, Any?> {
        val planLimits = profile.sanitizedPlanLimits()
        return mapOf(
            "firstName" to profile.firstName,
            "lastName" to profile.lastName,
            "email" to profile.email,
            "city" to profile.city,
            "state" to profile.state,
            "insuranceName" to profile.insuranceName,
            "insuranceId" to profile.insuranceId,
            "groupName" to profile.groupName,
            "pcpCopay" to profile.pcpCopay,
            "specialistCopay" to profile.specialistCopay,
            "insuranceCardDownloadUrl" to profile.insuranceCardDownloadUrl,
            "annualDeductibleLimit" to planLimits.annualDeductibleLimit,
            "annualOutOfPocketMax" to planLimits.annualOutOfPocketMax,
            "hsaAllocation" to planLimits.hsaAllocation,
            "fsaAllocation" to planLimits.fsaAllocation,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    fun profileFromMap(data: Map<String, Any?>): UserProfile {
        return UserProfile(
            firstName = data.stringValue("firstName", "first_name"),
            lastName = data.stringValue("lastName", "last_name"),
            email = data.stringValue("email"),
            city = data.stringValue("city"),
            state = data.stringValue("state"),
            insuranceName = data.stringValue("insuranceName", "insurance_name", "insuranceCardSummary", "insurance_card_summary"),
            insuranceId = data.stringValue("insuranceId", "insurance_id", "subscriberId", "subscriber_id", "memberId", "member_id", "policyId"),
            groupName = data.stringValue("groupName", "group_name", "groupNumber", "group_number"),
            pcpCopay = data.stringValue("pcpCopay", "pcp_copay"),
            specialistCopay = data.stringValue("specialistCopay", "specialist_copay"),
            insuranceCardDownloadUrl = data.stringValue("insuranceCardDownloadUrl", "insurance_card_download_url", "insurance_card_url"),
            annualDeductibleLimit = data.doubleValue("annualDeductibleLimit", "annual_deductible_limit"),
            annualOutOfPocketMax = data.doubleValue("annualOutOfPocketMax", "annual_out_of_pocket_max"),
            hsaAllocation = data.doubleValue("hsaAllocation", "hsa_allocation"),
            fsaAllocation = data.doubleValue("fsaAllocation", "fsa_allocation")
        ).sanitizedPlanLimits()
    }

    fun eobToMap(record: EobRecord): Map<String, Any?> {
        return mapOf(
            "id" to record.id,
            "sourceName" to record.sourceName,
            "providerName" to record.providerName,
            "insuranceName" to record.insuranceName,
            "serviceDate" to record.serviceDate,
            "serviceDateSortKey" to record.serviceDateSortKey,
            "duplicateChargeWarnings" to record.duplicateChargeWarnings,
            "rawText" to record.rawText,
            "totalBilledAmount" to record.totalBilledAmount,
            "totalInsurancePaidAmount" to record.totalInsurancePaidAmount,
            "totalContractualAdjustmentAmount" to record.totalContractualAdjustmentAmount,
            "totalCopayAmount" to record.totalCopayAmount,
            "totalDeductibleAmount" to record.totalDeductibleAmount,
            "totalCoinsuranceAmount" to record.totalCoinsuranceAmount,
            "isHsaEligible" to record.isHsaEligible,
            "isFsaEligible" to record.isFsaEligible,
            "charges" to record.charges.map(::chargeToMap),
            "provider_name" to record.providerName,
            "insurance_name" to record.insuranceName,
            "date_of_service" to record.serviceDate,
            "billed_amount" to record.totalBilledAmount,
            "insurance_paid" to record.totalInsurancePaidAmount,
            "contractual_adj" to record.totalContractualAdjustmentAmount,
            "copay" to record.totalCopayAmount,
            "deductible" to record.totalDeductibleAmount,
            "coinsurance" to record.totalCoinsuranceAmount,
            "total_amount_billed" to record.totalBilledAmount,
            "patient_responsibility" to (record.totalCopayAmount + record.totalDeductibleAmount + record.totalCoinsuranceAmount),
            "cptCodes" to record.charges.joinToString(",") { it.cptCode },
            "updatedAt" to System.currentTimeMillis()
        )
    }

    fun eobFromMap(data: Map<String, Any?>, documentId: String = ""): EobRecord {
        val enrichedData = enrichFromVeryfiClientStream(data)
        resolveNestedInsuranceEobPayload(enrichedData)?.let { nestedPayload ->
            nestedPayload.toNormalizedInsuranceEob().getOrNull()?.let { normalized ->
                return InsuranceEobRecordBridge.toEobRecord(
                    document = normalized.document,
                    documentRefId = documentId.ifBlank { enrichedData.stringValue("id") },
                    sourceName = enrichedData.stringValue("sourceName", "source_name").ifBlank { "Firebase" },
                    rawText = enrichedData.stringValue("rawText", "raw_text", "ocr_text", "ocrText")
                )
            }
        }
        val serviceDate = enrichedData.dateValue("serviceDate", "dateOfService", "date_of_service")
        val rawText = enrichedData.stringValue(
            "rawText",
            "raw_text",
            "raw_analysis_text",
            "full_ocr_data",
            "ocrText",
            "ocr_text"
        )
        val charges = enrichedData.listValue("charges")
            .mapNotNull { it as? Map<*, *> }
            .map { chargeFromMap(it.entries.associate { entry -> entry.key.toString() to entry.value }) }
            .ifEmpty { synthesizeCharges(enrichedData, rawText, serviceDate) }
        val storedHsaEligible = enrichedData.booleanValue("isHsaEligible", "is_hsa_eligible")
        val storedFsaEligible = enrichedData.booleanValue("isFsaEligible", "is_fsa_eligible")
        val detectedEligibility = EobAnalyzer.detectTaxVaultEligibility(rawText, charges)
        val taxVaultEligibility = EobAnalyzer.TaxVaultEligibility(
            isHsaEligible = storedHsaEligible ?: detectedEligibility.isHsaEligible,
            isFsaEligible = storedFsaEligible ?: detectedEligibility.isFsaEligible
        )

        val record = EobRecord(
            id = enrichedData.longValue("id").toInt().takeUnless { it == 0 } ?: stableId(documentId, enrichedData, serviceDate),
            firestoreId = documentId,
            sourceName = enrichedData.stringValue("sourceName", "source_name", "source").ifBlank { "Firebase" },
            providerName = resolveProviderName(enrichedData, rawText),
            insuranceName = resolveInsuranceName(enrichedData, rawText),
            serviceDate = serviceDate,
            serviceDateSortKey = enrichedData.longValue("serviceDateSortKey", "service_date_sort_key").toInt().takeUnless { it == 0 }
                ?: EobAnalyzer.serviceDateSortKey(serviceDate),
            charges = charges,
            duplicateChargeWarnings = enrichedData.stringListValue("duplicateChargeWarnings", "duplicate_charge_warnings"),
            rawText = rawText,
            totalBilledAmount = enrichedData.doubleValue("totalBilledAmount", "billed_amount", "total_amount_billed"),
            totalInsurancePaidAmount = enrichedData.doubleValue("totalInsurancePaidAmount", "insurance_paid"),
            totalContractualAdjustmentAmount = enrichedData.doubleValue("totalContractualAdjustmentAmount", "contractual_adj"),
            totalCopayAmount = enrichedData.doubleValue("totalCopayAmount", "copay"),
            totalDeductibleAmount = enrichedData.doubleValue("totalDeductibleAmount", "deductible"),
            totalCoinsuranceAmount = enrichedData.doubleValue("totalCoinsuranceAmount", "coinsurance"),
            isHsaEligible = taxVaultEligibility.isHsaEligible,
            isFsaEligible = taxVaultEligibility.isFsaEligible
        )
        return reconcileNormalizedEobRecord(record, enrichedData)
    }

    /**
     * Re-hydrates zeroed Firestore EOB rows from the embedded [veryfiClientStream] payload using
     * the same OCR/custom-field rules as the on-device hybrid extraction path.
     */
    internal fun enrichFromVeryfiClientStream(data: Map<String, Any?>): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        val stream = data["veryfiClientStream"] as? Map<String, Any?> ?: return data
        val enrichedPayload = VeryfiOcrFieldExtractor.enrichPayload(stream)
        val merged = data.toMutableMap()
        fun mergeIfMissing(vararg keys: String, value: Any?) {
            if (value == null) return
            val hasValue = keys.any { key ->
                when (val existing = merged[key]) {
                    null -> false
                    is Number -> existing.toDouble() > 0.0
                    is String -> existing.trim().isNotBlank()
                    else -> true
                }
            }
            if (!hasValue) {
                keys.forEach { key -> merged[key] = value }
            }
        }
        mergeIfMissing("billed_amount", "totalBilledAmount", "total_amount_billed", value = enrichedPayload["billed_amount"])
        mergeIfMissing("insurance_paid", "totalInsurancePaidAmount", value = enrichedPayload["insurance_paid"])
        mergeIfMissing("contractual_adj", "totalContractualAdjustmentAmount", value = enrichedPayload["contractual_adj"])
        mergeIfMissing("copay", "totalCopayAmount", value = enrichedPayload["copay"])
        mergeIfMissing("deductible", "totalDeductibleAmount", value = enrichedPayload["deductible"])
        mergeIfMissing("coinsurance", "totalCoinsuranceAmount", value = enrichedPayload["coinsurance"])
        mergeIfMissing("patient_responsibility", "patientResponsibility", value = enrichedPayload["patient_responsibility"])
        mergeIfMissing("provider_name", "providerName", value = enrichedPayload["provider_name"])
        mergeIfMissing("insurance_name", "insuranceName", value = enrichedPayload["insurance_name"])
        mergeIfMissing("date_of_service", "serviceDate", "dateOfService", value = enrichedPayload["date_of_service"])
        mergeIfMissing("cptCodes", "cpt_codes", "cpt_code", value = enrichedPayload["cpt_codes"] ?: enrichedPayload["cpt"])
        val ocrText = VeryfiOcrFieldExtractor.extractOcrText(enrichedPayload)
        if (ocrText.isNotBlank()) {
            mergeIfMissing("ocr_text", "rawText", "raw_text", value = ocrText)
        }
        return merged
    }

    /**
     * Locates nested `claims[]` on the Firestore row or inside embedded [veryfiClientStream].
     */
    internal fun resolveNestedInsuranceEobPayload(data: Map<String, Any?>): Map<String, Any?>? {
        if (VeryfiInsuranceEobPayloadParser.isNestedClaimsPayload(data)) {
            return data
        }
        @Suppress("UNCHECKED_CAST")
        val stream = data["veryfiClientStream"] as? Map<String, Any?>
        return stream?.takeIf { VeryfiInsuranceEobPayloadParser.isNestedClaimsPayload(it) }
    }

    /**
     * Mirrors Cloud Functions [normalizeEobDocument]: charge-line totals win when present,
     * and [patient_responsibility] backfills patient-share when copay/deductible/coinsurance are absent.
     */
    internal fun reconcileNormalizedEobRecord(record: EobRecord, data: Map<String, Any?>): EobRecord {
        val chargeTotals = totalCharges(record.charges)
        val reconciled = if (record.charges.isNotEmpty()) {
            val hasChargeLineAmounts = chargeTotals.billedAmount > 0.0 ||
                chargeTotals.insurancePaidAmount > 0.0 ||
                chargeTotals.contractualAdjustmentAmount > 0.0 ||
                chargeTotals.copayAmount > 0.0 ||
                chargeTotals.deductibleAmount > 0.0 ||
                chargeTotals.coinsuranceAmount > 0.0
            if (hasChargeLineAmounts) {
                record.copy(
                    totalBilledAmount = chargeTotals.billedAmount,
                    totalInsurancePaidAmount = chargeTotals.insurancePaidAmount,
                    totalContractualAdjustmentAmount = chargeTotals.contractualAdjustmentAmount,
                    totalCopayAmount = chargeTotals.copayAmount,
                    totalDeductibleAmount = chargeTotals.deductibleAmount,
                    totalCoinsuranceAmount = chargeTotals.coinsuranceAmount
                )
            } else {
                record
            }
        } else {
            record
        }
        return applyPatientResponsibilityFallback(reconciled, data)
    }

    private data class ChargeTotals(
        val billedAmount: Double,
        val insurancePaidAmount: Double,
        val contractualAdjustmentAmount: Double,
        val copayAmount: Double,
        val deductibleAmount: Double,
        val coinsuranceAmount: Double
    )

    private fun totalCharges(charges: List<EobCharge>): ChargeTotals {
        return charges.fold(
            ChargeTotals(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        ) { totals, charge ->
            ChargeTotals(
                billedAmount = totals.billedAmount + charge.billedAmount,
                insurancePaidAmount = totals.insurancePaidAmount + charge.insurancePaidAmount,
                contractualAdjustmentAmount = totals.contractualAdjustmentAmount + charge.contractualAdjustmentAmount,
                copayAmount = totals.copayAmount + charge.copayAmount,
                deductibleAmount = totals.deductibleAmount + charge.deductibleAmount,
                coinsuranceAmount = totals.coinsuranceAmount + charge.coinsuranceAmount
            )
        }
    }

    private fun applyPatientResponsibilityFallback(record: EobRecord, data: Map<String, Any?>): EobRecord {
        val extracted = record.totalCopayAmount + record.totalDeductibleAmount + record.totalCoinsuranceAmount
        val stored = data.doubleValue("patient_responsibility", "patientResponsibility")
        if (extracted <= 0.0 && stored > 0.0) {
            return record.copy(totalCopayAmount = stored)
        }
        return record
    }

    private fun resolveProviderName(data: Map<String, Any?>, rawText: String): String {
        val labeled = data.stringValue("providerName", "provider_name", "provider", "vendor_name")
        if (labeled.isNotBlank()) return labeled
        val vendorName = (data["vendor"] as? Map<*, *>)?.get("name")?.toString()?.trim().orEmpty()
        if (vendorName.isNotBlank()) return vendorName
        return EobAnalyzer.findProviderName(rawText)
    }

    private fun resolveInsuranceName(data: Map<String, Any?>, rawText: String): String {
        return data.stringValue(
            "insuranceName",
            "insurance_name",
            "insurance",
            "insurance_company_name",
            "insurance_company",
            "payerName",
            "payer_name"
        ).ifBlank { EobAnalyzer.findInsuranceName(rawText) }
    }

    fun newsFromMap(data: Map<String, Any?>): NewsRelease {
        val parsedBaseRelevance = data.longValue("baseRelevance", "base_relevance").toInt()
        return NewsRelease(
            company = data.stringValue("company"),
            headline = data.stringValue("headline"),
            summary = data.stringValue("summary"),
            date = data.stringValue("date"),
            targetTags = data.stringListValue("targetTags", "target_tags"),
            baseRelevance = parsedBaseRelevance.takeIf { it > 0 } ?: 1,
            articleUrl = data.stringValue("articleUrl", "article_url")
        )
    }

    private fun chargeToMap(charge: EobCharge): Map<String, Any?> {
        return mapOf(
            "cptCode" to charge.cptCode,
            "cptDescription" to charge.cptDescription,
            "category" to charge.category.name,
            "billedAmount" to charge.billedAmount,
            "insurancePaidAmount" to charge.insurancePaidAmount,
            "contractualAdjustmentAmount" to charge.contractualAdjustmentAmount,
            "copayAmount" to charge.copayAmount,
            "deductibleAmount" to charge.deductibleAmount,
            "coinsuranceAmount" to charge.coinsuranceAmount,
            "serviceDate" to charge.serviceDate
        )
    }

    private fun chargeFromMap(data: Map<String, Any?>): EobCharge {
        val code = data.stringValue("cptCode", "cpt_code", "code")
        val fallbackInfo = EobKnowledgeBase.cptInfoFor(code)
        return EobCharge(
            cptCode = code,
            cptDescription = data.stringValue("cptDescription", "cpt_description", "description").ifBlank { fallbackInfo.description },
            category = data.stringValue("category").toCptCategory() ?: fallbackInfo.category,
            billedAmount = data.doubleValue("billedAmount", "billed_amount", "charge"),
            insurancePaidAmount = data.doubleValue("insurancePaidAmount", "insurance_paid", "paid"),
            contractualAdjustmentAmount = data.doubleValue("contractualAdjustmentAmount", "contractual_adj", "contractual_adjustment", "adjustment"),
            copayAmount = data.doubleValue("copayAmount", "copay", "co_pay"),
            deductibleAmount = data.doubleValue("deductibleAmount"),
            coinsuranceAmount = data.doubleValue("coinsuranceAmount", "coinsurance"),
            serviceDate = data.dateValue("serviceDate", "dateOfService", "date_of_service")
        )
    }

    private fun synthesizeCharges(data: Map<String, Any?>, rawText: String, serviceDate: String): List<EobCharge> {
        val cptCodes = data.cptCodes()
            .ifEmpty { EobAnalyzer.validCptCodes(data.lineItemsText()) }
            .ifEmpty { EobAnalyzer.validCptCodes(rawText) }
        if (cptCodes.isEmpty()) return emptyList()
        val firstCode = cptCodes.first()
        return cptCodes.map { code ->
            val fallbackInfo = EobKnowledgeBase.cptInfoFor(code)
            EobCharge(
                cptCode = code,
                cptDescription = fallbackInfo.description,
                category = fallbackInfo.category,
                billedAmount = if (code == firstCode) data.doubleValue("totalBilledAmount", "billedAmount", "totalAmountBilled", "total_amount_billed", "billed_amount") else 0.0,
                insurancePaidAmount = if (code == firstCode) data.doubleValue("totalInsurancePaidAmount", "insurancePaid", "insurance_paid") else 0.0,
                contractualAdjustmentAmount = if (code == firstCode) data.doubleValue("totalContractualAdjustmentAmount", "contractualAdjustment", "contractual_adj", "contractual_adjustment") else 0.0,
                copayAmount = if (code == firstCode) data.doubleValue("totalCopayAmount", "copayAmount", "copay") else 0.0,
                deductibleAmount = if (code == firstCode) data.doubleValue("totalDeductibleAmount", "deductibleAmount", "deductible") else 0.0,
                coinsuranceAmount = if (code == firstCode) data.doubleValue("totalCoinsuranceAmount", "coinsuranceAmount", "coinsurance") else 0.0,
                serviceDate = serviceDate
            )
        }
    }

    private fun Map<String, Any?>.cptCodes(): List<String> {
        val rawCodes = this["cptCodes"] ?: this["cpt_codes"] ?: this["cptCode"] ?: this["cpt_code"] ?: this["cpt"]
        return when (rawCodes) {
            is String -> rawCodes.split(",", " ", ";", "|")
            is List<*> -> rawCodes.mapNotNull { it?.toString() }
            else -> emptyList()
        }.map { it.trim().uppercase(Locale.US) }
            .filter { Regex("^[1-9][0-9]{4}$|^[A-J][0-9]{4}$").matches(it) }
            .distinct()
    }

    private fun Map<String, Any?>.lineItemsText(): String {
        val rawLineItems = this["line_items"] ?: this["lineItems"] ?: return ""
        return when (rawLineItems) {
            is String -> rawLineItems
            is List<*> -> rawLineItems.joinToString(" ") { item ->
                when (item) {
                    is Map<*, *> -> listOfNotNull(
                        item["description"]?.toString(),
                        item["cpt_code"]?.toString(),
                        item["cptCode"]?.toString(),
                        item["code"]?.toString()
                    ).joinToString(" ")
                    else -> item?.toString().orEmpty()
                }
            }
            else -> rawLineItems.toString()
        }
    }

    private fun Map<String, Any?>.stringValue(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key -> this[key]?.toString()?.takeIf { it.isNotBlank() } }.orEmpty()
    }

    private fun Map<String, Any?>.longValue(vararg keys: String): Long {
        return when (val value = keys.firstNotNullOfOrNull { key -> this[key] }) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    private fun Map<String, Any?>.doubleValue(vararg keys: String): Double {
        return when (val value = keys.firstNotNullOfOrNull { key -> this[key] }) {
            is Number -> value.toDouble()
            is String -> value.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun Map<String, Any?>.booleanValue(vararg keys: String): Boolean? {
        return when (val value = keys.firstNotNullOfOrNull { key -> this[key] }) {
            is Boolean -> value
            is String -> when (value.trim().lowercase(Locale.US)) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> null
            }
            is Number -> value.toInt() != 0
            else -> null
        }
    }

    private fun Map<String, Any?>.dateValue(vararg keys: String): String {
        val rawDate = keys.firstNotNullOfOrNull { key -> this[key] } ?: return "Date not recognized"
        return when (rawDate) {
            is Number -> SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(rawDate.toLong()))
            is Date -> SimpleDateFormat("MM/dd/yyyy", Locale.US).format(rawDate)
            else -> normalizeDateString(rawDate.toString())
        }
    }

    private fun Map<String, Any?>.listValue(key: String): List<Any?> {
        return this[key] as? List<Any?> ?: emptyList()
    }

    private fun Map<String, Any?>.stringListValue(vararg keys: String): List<String> {
        return keys.firstNotNullOfOrNull { key -> listValue(key).takeIf { it.isNotEmpty() } }
            ?.mapNotNull { it?.toString() }
            ?: emptyList()
    }

    private fun stableId(documentId: String, data: Map<String, Any?>, serviceDate: String): Int {
        return (documentId.ifBlank {
            listOf(
                data.stringValue("providerName", "provider_name", "provider"),
                data.stringValue("insuranceName", "insurance_name", "insurance"),
                serviceDate,
                data.cptCodes().joinToString(",")
            ).joinToString("|")
        }.hashCode() and Int.MAX_VALUE).takeUnless { it == 0 } ?: 1
    }

    private fun normalizeDateString(rawDate: String): String {
        val trimmed = rawDate.trim()
        if (trimmed.isBlank()) return "Date not recognized"
        val isoMatch = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""").find(trimmed)
        if (isoMatch != null) {
            val year = isoMatch.groupValues[1]
            val month = isoMatch.groupValues[2].padStart(2, '0')
            val day = isoMatch.groupValues[3].padStart(2, '0')
            return "$month/$day/$year"
        }
        return trimmed
    }

    private fun String.toCptCategory(): CptCategory? {
        return CptCategory.entries.firstOrNull { it.name == this }
    }
}
