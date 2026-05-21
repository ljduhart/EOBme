package app.eob.me.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirebaseEobMapper {
    fun profileToMap(profile: UserProfile): Map<String, Any?> {
        return mapOf(
            "firstName" to profile.firstName,
            "lastName" to profile.lastName,
            "email" to profile.email,
            "city" to profile.city,
            "state" to profile.state,
            "subscriberId" to profile.subscriberId.ifBlank { profile.insuranceGroupNumber },
            "insuranceCardSummary" to profile.insuranceCardSummary,
            "insuranceCardDownloadUrl" to profile.insuranceCardDownloadUrl,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    fun profileFromMap(data: Map<String, Any?>, currentPassword: String = ""): UserProfile {
        return UserProfile(
            firstName = data.stringValue("firstName", "first_name"),
            lastName = data.stringValue("lastName", "last_name"),
            email = data.stringValue("email"),
            password = currentPassword,
            city = data.stringValue("city"),
            state = data.stringValue("state"),
            subscriberId = data.stringValue("subscriberId", "subscriber_id", "memberId", "member_id", "policyId"),
            insuranceCardSummary = data.stringValue("insuranceCardSummary", "insurance_card_summary"),
            insuranceCardDownloadUrl = data.stringValue("insuranceCardDownloadUrl", "insurance_card_download_url", "insurance_card_url")
        )
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
        val serviceDate = data.dateValue("serviceDate", "dateOfService", "date_of_service")
        val rawText = data.stringValue(
            "rawText",
            "raw_text",
            "raw_analysis_text",
            "full_ocr_data",
            "ocrText",
            "ocr_text"
        )
        val charges = data.listValue("charges")
            .mapNotNull { it as? Map<*, *> }
            .map { chargeFromMap(it.entries.associate { entry -> entry.key.toString() to entry.value }) }
            .ifEmpty { synthesizeCharges(data, rawText, serviceDate) }

        return EobRecord(
            id = data.longValue("id").toInt().takeUnless { it == 0 } ?: stableId(documentId, data, serviceDate),
            sourceName = data.stringValue("sourceName", "source_name", "source").ifBlank { "Firebase" },
            providerName = data.stringValue("providerName", "provider_name", "provider").ifBlank { EobAnalyzer.findProviderName(rawText) },
            insuranceName = data.stringValue("insuranceName", "insurance_name", "insurance", "payerName", "payer_name").ifBlank { EobAnalyzer.findInsuranceName(rawText) },
            serviceDate = serviceDate,
            serviceDateSortKey = data.longValue("serviceDateSortKey", "service_date_sort_key").toInt().takeUnless { it == 0 }
                ?: EobAnalyzer.serviceDateSortKey(serviceDate),
            charges = charges,
            duplicateChargeWarnings = data.stringListValue("duplicateChargeWarnings", "duplicate_charge_warnings"),
            rawText = rawText,
            totalBilledAmount = data.doubleValue("totalBilledAmount", "billed_amount", "total_amount_billed"),
            totalInsurancePaidAmount = data.doubleValue("totalInsurancePaidAmount", "insurance_paid"),
            totalContractualAdjustmentAmount = data.doubleValue("totalContractualAdjustmentAmount", "contractual_adj"),
            totalCopayAmount = data.doubleValue("totalCopayAmount", "copay"),
            totalDeductibleAmount = data.doubleValue("totalDeductibleAmount", "deductible"),
            totalCoinsuranceAmount = data.doubleValue("totalCoinsuranceAmount", "coinsurance")
        )
    }

    fun newsFromMap(data: Map<String, Any?>): NewsRelease {
        return NewsRelease(
            company = data.stringValue("company"),
            headline = data.stringValue("headline"),
            summary = data.stringValue("summary"),
            date = data.stringValue("date")
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
        val cptCodes = data.cptCodes().ifEmpty { EobAnalyzer.validCptCodes(rawText) }
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
        val rawCodes = this["cptCodes"] ?: this["cpt_codes"] ?: this["cptCode"] ?: this["cpt_code"]
        return when (rawCodes) {
            is String -> rawCodes.split(",", " ", ";", "|")
            is List<*> -> rawCodes.mapNotNull { it?.toString() }
            else -> emptyList()
        }.map { it.trim().uppercase() }
            .filter { Regex("^[1-9][0-9]{4}$|^[A-J][0-9]{4}$").matches(it) }
            .distinct()
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
