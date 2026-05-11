package com.eobme.app.data

import java.util.Locale
import kotlin.math.max

object EobAnalyzer {
    private val cptRegex = Regex("\\b([1-9][0-9]{4}|[A-Ja-j][0-9]{4})\\b")
    private val dateRegexes = listOf(
        Regex("\\b(0?[1-9]|1[0-2])[/-](0?[1-9]|[12][0-9]|3[01])[/-]((?:20)?[0-9]{2})\\b"),
        Regex("\\b((?:20)[0-9]{2})[/-](0?[1-9]|1[0-2])[/-](0?[1-9]|[12][0-9]|3[01])\\b")
    )

    fun analyze(rawText: String, sourceName: String, nextId: Int): EobRecord {
        val cleanedText = normalizeScan(rawText)
        val insuranceName = findInsuranceName(cleanedText)
        val providerName = findProviderName(cleanedText)
        val serviceDate = findServiceDate(cleanedText)
        val lines = cleanedText.lines().filter { it.isNotBlank() }
        val lineCharges = lines.mapNotNull { parseChargeLine(it, serviceDate) }
        val charges = if (lineCharges.isNotEmpty()) lineCharges else parseDocumentLevelCharge(cleanedText, serviceDate)
        val warnings = duplicateWarnings(charges)

        return EobRecord(
            id = nextId,
            sourceName = sourceName,
            providerName = providerName,
            insuranceName = insuranceName,
            serviceDate = serviceDate,
            serviceDateSortKey = serviceDateSortKey(serviceDate),
            charges = charges,
            duplicateChargeWarnings = warnings,
            rawText = cleanedText
        )
    }

    fun normalizeScan(rawText: String): String {
        return rawText
            .replace('\u00A0', ' ')
            .replace(Regex("[\\t ]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    fun findInsuranceName(text: String): String {
        return EobKnowledgeBase.insuranceNames
            .sortedByDescending { it.length }
            .firstOrNull { insurance ->
                val escapedName = insurance
                    .split(Regex("\\s+"))
                    .joinToString("\\s+") { Regex.escape(it) }
                Regex("(?i)(?<![A-Za-z0-9])$escapedName(?![A-Za-z0-9])").containsMatchIn(text)
            }
            ?: "Insurance not recognized"
    }

    fun findProviderName(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val labeledProvider = lines.firstNotNullOfOrNull { line ->
            val match = Regex("(?i)\\b(provider|rendering provider|facility|doctor|physician)\\b\\s*[:#-]?\\s*(.+)").find(line)
            match?.groupValues?.getOrNull(2)?.takeIf { it.length > 2 }
        }
        if (labeledProvider != null) return labeledProvider.cleanLabelValue()

        return lines.firstOrNull { line ->
            line.contains("clinic", ignoreCase = true) ||
                line.contains("hospital", ignoreCase = true) ||
                line.contains("medical", ignoreCase = true) ||
                line.contains("health", ignoreCase = true)
        }?.cleanLabelValue() ?: "Provider not recognized"
    }

    fun validCptCodes(text: String): List<String> {
        return cptRegex.findAll(text)
            .map { it.groupValues[1].uppercase(Locale.US) }
            .distinct()
            .toList()
    }

    fun cptUsage(records: List<EobRecord>, year: Int): List<CptUsage> {
        return records
            .flatMap { record ->
                record.charges.filter { charge -> serviceYear(charge.serviceDate) == year }
            }
            .groupingBy { it.cptCode }
            .eachCount()
            .map { (code, count) -> CptUsage(EobKnowledgeBase.cptInfoFor(code), year, count) }
            .sortedWith(compareBy<CptUsage> { it.info.category.ordinal }.thenBy { it.info.code })
    }

    fun isSameEob(first: EobRecord, second: EobRecord): Boolean {
        return first.insuranceName.equals(second.insuranceName, ignoreCase = true) &&
            first.providerName.equals(second.providerName, ignoreCase = true) &&
            first.serviceDate == second.serviceDate &&
            first.charges.map { it.cptCode }.sorted() == second.charges.map { it.cptCode }.sorted()
    }

    fun compactDuplicateEobs(records: List<EobRecord>): List<EobRecord> {
        return records.sortedBy { it.serviceDateSortKey }.fold(mutableListOf()) { compacted, record ->
            val duplicateIndex = compacted.indexOfFirst { existing -> isSameEob(existing, record) }
            if (duplicateIndex >= 0) {
                compacted[duplicateIndex] = record.copy(id = compacted[duplicateIndex].id)
            } else {
                compacted.add(record)
            }
            compacted
        }
    }

    private fun parseChargeLine(line: String, fallbackDate: String): EobCharge? {
        val cpt = cptRegex.find(line)?.groupValues?.getOrNull(1)?.uppercase(Locale.US) ?: return null
        val localDate = findServiceDate(line).takeUnless { it == "Date not recognized" } ?: fallbackDate
        val billed = amountAfterLabels(line, listOf("billed", "charge", "charged", "submitted", "amount billed"))
        val paid = amountAfterLabels(line, listOf("paid", "insurance paid", "plan paid", "payer paid"))
        val adjustment = amountAfterLabels(line, listOf("adjustment", "contractual", "allowed discount", "discount"))
        val copay = amountAfterLabels(line, listOf("copay", "co-pay"))
        val deductible = amountAfterLabels(line, listOf("deductible"))
        val coinsurance = amountAfterLabels(line, listOf("coinsurance", "co-insurance"))
        val amounts = moneyValues(line)

        return EobCharge(
            cptCode = cpt,
            cptDescription = EobKnowledgeBase.cptInfoFor(cpt).description,
            category = EobKnowledgeBase.cptInfoFor(cpt).category,
            billedAmount = billed ?: amounts.getOrNull(0) ?: 0.0,
            insurancePaidAmount = paid ?: amounts.getOrNull(1) ?: 0.0,
            contractualAdjustmentAmount = adjustment ?: amounts.getOrNull(2) ?: 0.0,
            copayAmount = copay ?: 0.0,
            deductibleAmount = deductible ?: 0.0,
            coinsuranceAmount = coinsurance ?: 0.0,
            serviceDate = localDate
        )
    }

    private fun parseDocumentLevelCharge(text: String, serviceDate: String): List<EobCharge> {
        return validCptCodes(text).map { cpt ->
            val info = EobKnowledgeBase.cptInfoFor(cpt)
            EobCharge(
                cptCode = cpt,
                cptDescription = info.description,
                category = info.category,
                billedAmount = amountAfterLabels(text, listOf("total billed", "billed", "charges")) ?: 0.0,
                insurancePaidAmount = amountAfterLabels(text, listOf("insurance paid", "plan paid", "paid")) ?: 0.0,
                contractualAdjustmentAmount = amountAfterLabels(text, listOf("contractual adjustment", "adjustment")) ?: 0.0,
                copayAmount = amountAfterLabels(text, listOf("copay", "co-pay")) ?: 0.0,
                deductibleAmount = amountAfterLabels(text, listOf("deductible")) ?: 0.0,
                coinsuranceAmount = amountAfterLabels(text, listOf("coinsurance", "co-insurance")) ?: 0.0,
                serviceDate = serviceDate
            )
        }
    }

    private fun duplicateWarnings(charges: List<EobCharge>): List<String> {
        return charges
            .groupBy { "${it.serviceDate}|${it.cptCode}|${it.billedAmount}" }
            .filterValues { it.size > 1 }
            .map { (_, duplicates) ->
                val charge = duplicates.first()
                "Possible duplicate: ${charge.cptCode} on ${charge.serviceDate} for ${charge.billedAmount.asCurrency()}"
            }
    }

    private fun amountAfterLabels(text: String, labels: List<String>): Double? {
        labels.forEach { label ->
            val regex = Regex("""(?i)\b${Regex.escape(label)}\b\s*[:#-]?\s*\${'$'}?\(?([0-9,]+(?:\.[0-9]{2})?)\)?""")
            val value = regex.find(text)?.groupValues?.getOrNull(1)?.toMoney()
            if (value != null) return value
        }
        return null
    }

    private fun moneyValues(text: String): List<Double> {
        return Regex("""\${'$'}\s*([0-9,]+(?:\.[0-9]{2})?)""")
            .findAll(text)
            .mapNotNull { it.groupValues.getOrNull(1)?.toMoney() }
            .toList()
    }

    private fun findServiceDate(text: String): String {
        dateRegexes.forEachIndexed { index, regex ->
            val match = regex.find(text) ?: return@forEachIndexed
            return if (index == 0) {
                val month = match.groupValues[1].padStart(2, '0')
                val day = match.groupValues[2].padStart(2, '0')
                val year = normalizeYear(match.groupValues[3])
                "$month/$day/$year"
            } else {
                val year = match.groupValues[1]
                val month = match.groupValues[2].padStart(2, '0')
                val day = match.groupValues[3].padStart(2, '0')
                "$month/$day/$year"
            }
        }
        return "Date not recognized"
    }

    fun serviceDateSortKey(date: String): Int {
        val parts = date.split("/")
        if (parts.size != 3) return Int.MAX_VALUE
        val month = parts[0].toIntOrNull() ?: return Int.MAX_VALUE
        val day = parts[1].toIntOrNull() ?: return Int.MAX_VALUE
        val year = parts[2].toIntOrNull() ?: return Int.MAX_VALUE
        return year * 10000 + month * 100 + day
    }

    fun serviceYear(date: String): Int {
        return max(0, date.split("/").getOrNull(2)?.toIntOrNull() ?: 0)
    }

    private fun normalizeYear(year: String): String {
        return if (year.length == 2) "20$year" else year
    }

    private fun String.toMoney(): Double? = replace(",", "").toDoubleOrNull()

    private fun String.cleanLabelValue(): String {
        return replace(Regex("\\s{2,}"), " ")
            .trim(' ', '-', ':')
            .take(64)
    }
}

fun Double.asCurrency(): String = "$" + String.format(Locale.US, "%,.2f", this)
