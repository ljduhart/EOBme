package app.eob.me.data

import java.util.Locale
import kotlin.math.abs
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

    fun accuracyReview(record: EobRecord): EobAccuracyReview {
        val fieldConfidences = listOf(
            confidence("Insurance", record.insuranceName, !record.insuranceName.contains("not recognized", ignoreCase = true)),
            confidence("Provider", record.providerName, !record.providerName.contains("not recognized", ignoreCase = true)),
            confidence("Date of Service", record.serviceDate, record.serviceDate != "Date not recognized"),
            confidence("Billed Amount", record.totalBilledAmount.asCurrency(), record.totalBilledAmount > 0.0),
            confidence("Insurance Paid", record.totalInsurancePaidAmount.asCurrency(), record.totalInsurancePaidAmount > 0.0),
            confidence("Contractual Adjustment", record.totalContractualAdjustmentAmount.asCurrency(), record.totalContractualAdjustmentAmount >= 0.0),
            confidence("CPT Codes", record.charges.joinToString { it.cptCode }, record.charges.isNotEmpty() && record.charges.all { it.cptCode.isNotBlank() })
        )
        val expectedResponsibility = (record.totalBilledAmount - record.totalInsurancePaidAmount - record.totalContractualAdjustmentAmount)
            .coerceAtLeast(0.0)
        val extractedResponsibility = record.totalCopayAmount + record.totalDeductibleAmount + record.totalCoinsuranceAmount
        val difference = abs(expectedResponsibility - extractedResponsibility)
        val mathValidation = EobMathValidation(
            expectedPatientResponsibility = expectedResponsibility,
            extractedPatientResponsibility = extractedResponsibility,
            difference = difference,
            isBalanced = difference <= 0.05
        )
        val warnings = buildList {
            addAll(record.duplicateChargeWarnings)
            fieldConfidences.filter { it.needsReview }.forEach { add("${it.fieldName} needs review") }
            if (!mathValidation.isBalanced) {
                add("Billing math differs by ${difference.asCurrency()}")
            }
            if (record.rawText.length < 80) {
                add("OCR text is short; consider rescanning for better accuracy")
            }
        }
        val overall = fieldConfidences.map { it.confidencePercent }.average().toInt()
            .let { if (mathValidation.isBalanced) it else (it - 10).coerceAtLeast(0) }

        return EobAccuracyReview(
            overallConfidencePercent = overall,
            fields = fieldConfidences,
            mathValidation = mathValidation,
            warnings = warnings.distinct()
        )
    }

    fun yearlyHealthCostSummary(records: List<EobRecord>, preferredYear: Int? = null): YearlyHealthCostSummary {
        val year = preferredYear
            ?: records.map { serviceYear(it.serviceDate) }.filter { it > 0 }.maxOrNull()
            ?: 0
        val yearRecords = records.filter { serviceYear(it.serviceDate) == year }
        return YearlyHealthCostSummary(
            year = year,
            eobCount = yearRecords.size,
            totalBilled = yearRecords.sumOf { it.totalBilledAmount },
            totalInsurancePaid = yearRecords.sumOf { it.totalInsurancePaidAmount },
            totalContractualAdjustment = yearRecords.sumOf { it.totalContractualAdjustmentAmount },
            totalCopay = yearRecords.sumOf { it.totalCopayAmount },
            totalDeductible = yearRecords.sumOf { it.totalDeductibleAmount },
            totalCoinsurance = yearRecords.sumOf { it.totalCoinsuranceAmount }
        )
    }

    fun detectBillingIssues(record: EobRecord): List<BillingIssue> {
        val issues = mutableListOf<BillingIssue>()
        val review = accuracyReview(record)

        if (record.duplicateChargeWarnings.isNotEmpty()) {
            issues += BillingIssue(
                type = BillingIssueType.DuplicateCharge,
                severity = BillingIssueSeverity.Critical,
                title = "Possible duplicate charge",
                explanation = record.duplicateChargeWarnings.joinToString("; "),
                recommendedAction = "Ask the insurer and provider to verify whether this line was billed more than once."
            )
        }
        if (!review.mathValidation.isBalanced) {
            issues += BillingIssue(
                type = BillingIssueType.MathMismatch,
                severity = BillingIssueSeverity.Warning,
                title = "Billing math mismatch",
                explanation = "Expected patient responsibility is ${review.mathValidation.expectedPatientResponsibility.asCurrency()}, but extracted responsibility is ${review.mathValidation.extractedPatientResponsibility.asCurrency()}.",
                recommendedAction = "Request a written explanation of the remaining patient responsibility."
            )
        }
        if (record.totalInsurancePaidAmount == 0.0 && record.totalBilledAmount > 0.0) {
            issues += BillingIssue(
                type = BillingIssueType.MissingInsurancePayment,
                severity = BillingIssueSeverity.Critical,
                title = "No insurance payment detected",
                explanation = "The EOB has billed charges but no insurance payment was extracted.",
                recommendedAction = "Confirm whether the claim was denied, still processing, or applied fully to patient responsibility."
            )
        }
        if (record.totalBilledAmount > 0.0 && review.mathValidation.extractedPatientResponsibility / record.totalBilledAmount >= 0.5) {
            issues += BillingIssue(
                type = BillingIssueType.HighPatientResponsibility,
                severity = BillingIssueSeverity.Warning,
                title = "High patient responsibility",
                explanation = "The patient responsibility appears to be at least 50% of the billed amount.",
                recommendedAction = "Review deductible, copay, coinsurance, network status, and plan benefits."
            )
        }
        if (record.providerName.contains("not recognized", ignoreCase = true)) {
            issues += BillingIssue(
                type = BillingIssueType.MissingProvider,
                severity = BillingIssueSeverity.Warning,
                title = "Provider missing",
                explanation = "The provider name could not be confidently extracted.",
                recommendedAction = "Review the scan and add or correct the provider before sending an appeal."
            )
        }
        if (record.insuranceName.contains("not recognized", ignoreCase = true)) {
            issues += BillingIssue(
                type = BillingIssueType.MissingInsurance,
                severity = BillingIssueSeverity.Warning,
                title = "Insurance missing",
                explanation = "The insurance company could not be confidently extracted.",
                recommendedAction = "Review the scan and select the correct insurance company."
            )
        }
        if (record.serviceDate == "Date not recognized") {
            issues += BillingIssue(
                type = BillingIssueType.MissingDateOfService,
                severity = BillingIssueSeverity.Warning,
                title = "Date of Service missing",
                explanation = "The service date could not be confidently extracted.",
                recommendedAction = "Review the EOB and confirm the date of service."
            )
        }
        if (record.charges.isEmpty()) {
            issues += BillingIssue(
                type = BillingIssueType.MissingCptCode,
                severity = BillingIssueSeverity.Warning,
                title = "No CPT codes detected",
                explanation = "No valid CPT/HCPCS codes were extracted from the EOB.",
                recommendedAction = "Rescan or manually review the EOB line items."
            )
        }
        if (record.rawText.contains("denied", ignoreCase = true) || record.rawText.contains("not covered", ignoreCase = true)) {
            issues += BillingIssue(
                type = BillingIssueType.PossibleDenial,
                severity = BillingIssueSeverity.Critical,
                title = "Possible denial language",
                explanation = "The EOB text contains denial or not-covered language.",
                recommendedAction = "Use the denial appeal template and request the plan rule used for the denial."
            )
        }

        return issues.distinctBy { it.type }
    }

    fun providerDirectory(records: List<EobRecord>): List<ProviderSummary> {
        return records
            .filter { it.providerName.isNotBlank() && !it.providerName.contains("not recognized", ignoreCase = true) }
            .groupBy { it.providerName }
            .map { (provider, providerRecords) ->
                val latest = providerRecords.maxByOrNull { it.serviceDateSortKey }
                ProviderSummary(
                    providerName = provider,
                    eobCount = providerRecords.size,
                    totalBilled = providerRecords.sumOf { it.totalBilledAmount },
                    totalInsurancePaid = providerRecords.sumOf { it.totalInsurancePaidAmount },
                    totalPatientResponsibility = providerRecords.sumOf { it.totalCopayAmount + it.totalDeductibleAmount + it.totalCoinsuranceAmount },
                    lastServiceDate = latest?.serviceDate ?: "Date not recognized"
                )
            }
            .sortedByDescending { it.eobCount }
    }

    private fun confidence(fieldName: String, value: String, isReliable: Boolean): EobFieldConfidence {
        return EobFieldConfidence(
            fieldName = fieldName,
            value = value.ifBlank { "Missing" },
            confidencePercent = if (isReliable) 95 else 45,
            needsReview = !isReliable
        )
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
