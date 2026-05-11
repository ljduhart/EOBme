package com.eobme.app.data

object FirebaseEobMapper {
    fun profileToMap(profile: UserProfile): Map<String, Any?> {
        return mapOf(
            "firstName" to profile.firstName,
            "lastName" to profile.lastName,
            "email" to profile.email,
            "city" to profile.city,
            "state" to profile.state,
            "subscriberId" to profile.subscriberId,
            "insuranceCardSummary" to profile.insuranceCardSummary,
            "insuranceCardDownloadUrl" to profile.insuranceCardDownloadUrl,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    fun profileFromMap(data: Map<String, Any?>, currentPassword: String = ""): UserProfile {
        return UserProfile(
            firstName = data.stringValue("firstName"),
            lastName = data.stringValue("lastName"),
            email = data.stringValue("email"),
            password = currentPassword,
            city = data.stringValue("city"),
            state = data.stringValue("state"),
            subscriberId = data.stringValue("subscriberId"),
            insuranceCardSummary = data.stringValue("insuranceCardSummary"),
            insuranceCardDownloadUrl = data.stringValue("insuranceCardDownloadUrl")
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
            "updatedAt" to System.currentTimeMillis()
        )
    }

    fun eobFromMap(data: Map<String, Any?>): EobRecord {
        val charges = data.listValue("charges")
            .mapNotNull { it as? Map<*, *> }
            .map { chargeFromMap(it.entries.associate { entry -> entry.key.toString() to entry.value }) }

        return EobRecord(
            id = data.longValue("id").toInt(),
            sourceName = data.stringValue("sourceName"),
            providerName = data.stringValue("providerName").ifBlank { "Provider not recognized" },
            insuranceName = data.stringValue("insuranceName").ifBlank { "Insurance not recognized" },
            serviceDate = data.stringValue("serviceDate").ifBlank { "Date not recognized" },
            serviceDateSortKey = data.longValue("serviceDateSortKey").toInt().takeUnless { it == 0 }
                ?: EobAnalyzer.serviceDateSortKey(data.stringValue("serviceDate")),
            charges = charges,
            duplicateChargeWarnings = data.stringListValue("duplicateChargeWarnings"),
            rawText = data.stringValue("rawText")
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
        val code = data.stringValue("cptCode")
        val fallbackInfo = EobKnowledgeBase.cptInfoFor(code)
        return EobCharge(
            cptCode = code,
            cptDescription = data.stringValue("cptDescription").ifBlank { fallbackInfo.description },
            category = data.stringValue("category").toCptCategory() ?: fallbackInfo.category,
            billedAmount = data.doubleValue("billedAmount"),
            insurancePaidAmount = data.doubleValue("insurancePaidAmount"),
            contractualAdjustmentAmount = data.doubleValue("contractualAdjustmentAmount"),
            copayAmount = data.doubleValue("copayAmount"),
            deductibleAmount = data.doubleValue("deductibleAmount"),
            coinsuranceAmount = data.doubleValue("coinsuranceAmount"),
            serviceDate = data.stringValue("serviceDate")
        )
    }

    private fun Map<String, Any?>.stringValue(key: String): String = this[key]?.toString().orEmpty()

    private fun Map<String, Any?>.longValue(key: String): Long {
        return when (val value = this[key]) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    private fun Map<String, Any?>.doubleValue(key: String): Double {
        return when (val value = this[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun Map<String, Any?>.listValue(key: String): List<Any?> {
        return this[key] as? List<Any?> ?: emptyList()
    }

    private fun Map<String, Any?>.stringListValue(key: String): List<String> {
        return listValue(key).mapNotNull { it?.toString() }
    }

    private fun String.toCptCategory(): CptCategory? {
        return CptCategory.entries.firstOrNull { it.name == this }
    }
}
