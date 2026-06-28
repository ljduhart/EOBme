package app.eob.me.data

import java.util.Calendar

object BentoSnapshotExtractor {
    private const val DEFAULT_DEDUCTIBLE = 2_000.0
    private const val DEFAULT_OOP_MAX = 8_550.0
    private const val ANNUAL_CPT_VISIT_CAP = 24f

    fun buildCptBentoSnapshot(
        language: AppLanguage,
        records: List<EobRecord>,
        selectedCategory: CptCategory
    ): CptBentoSnapshot {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val yearCharges = records
            .filter { EobAnalyzer.serviceYear(it.serviceDate) == year }
            .flatMap { it.charges }
            .filter { it.category == selectedCategory }

        val dominantCode = yearCharges
            .groupingBy { it.cptCode }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            .orEmpty()
            .ifBlank { "99213" }

        val info = EobKnowledgeBase.cptInfoFor(dominantCode)
        val shortLabel = cptShortLabel(language, dominantCode, info)
        val translatorLine = EobStrings.tf(language, "cptTranslatorFormat", dominantCode, shortLabel)

        val usageCount = yearCharges.count { it.cptCode == dominantCode }.toFloat()
        val ringProgress = (usageCount / ANNUAL_CPT_VISIT_CAP).coerceIn(0f, 1f)

        val recentCharges = yearCharges.takeLast(6)
        val trendPoints = if (recentCharges.isEmpty()) {
            listOf(0.5f, 0.5f, 0.5f)
        } else {
            recentCharges.map { charge ->
                val ratio = FairHealthPricingIndex.priceRatio(charge.billedAmount, charge.cptCode)
                ((ratio - 0.5) / 1.5 + 0.5).toFloat().coerceIn(0.05f, 0.95f)
            }
        }

        val avgRatio = if (yearCharges.isEmpty()) {
            1.0
        } else {
            yearCharges.map { FairHealthPricingIndex.priceRatio(it.billedAmount, it.cptCode) }.average()
        }
        val gaugePosition = ((avgRatio - 0.5) / 1.5 + 0.5).toFloat().coerceIn(0.05f, 0.95f)
        val direction = when {
            avgRatio < 0.92 -> PriceTrendDirection.BelowFair
            avgRatio > 1.12 -> PriceTrendDirection.AboveFair
            else -> PriceTrendDirection.NearFair
        }

        return CptBentoSnapshot(
            dominantCode = dominantCode,
            translatorLine = translatorLine,
            ringProgress = ringProgress,
            priceGaugePosition = gaugePosition,
            priceTrendPoints = trendPoints,
            trendDirection = direction
        )
    }

    fun buildCptFlashcardEntries(
        language: AppLanguage,
        records: List<EobRecord>,
        category: CptCategory
    ): List<CptCodeEntry> {
        return records
            .flatMap { it.charges }
            .filter { it.category == category }
            .groupBy { it.cptCode }
            .map { (code, charges) ->
                val info = EobKnowledgeBase.cptInfoFor(code)
                val chargeDescription = charges.firstOrNull()?.cptDescription.orEmpty()
                val definition = resolveCptFlashcardDefinition(
                    language = language,
                    code = code,
                    info = info,
                    chargeDescription = chargeDescription
                )
                val shortName = resolveCptFlashcardShortName(
                    language = language,
                    code = code,
                    definition = definition
                )
                val totalBilled = charges.sumOf { it.billedAmount }
                CptCodeEntry(
                    code = code,
                    category = EobStrings.cptCategoryLabel(language, category),
                    shortName = shortName,
                    definition = definition,
                    totalBilled = java.util.Locale.US.let { locale ->
                        String.format(locale, "$%.2f", totalBilled)
                    }
                )
            }
            .sortedBy { it.code }
    }

    fun buildYtdDeductibleBentoSnapshot(
        records: List<EobRecord>,
        profile: UserProfile
    ): YtdDeductibleBentoSnapshot {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val yearRecords = records.filter { EobAnalyzer.serviceYear(it.serviceDate) == year }
        val summary = EobAnalyzer.yearlyHealthCostSummary(records, preferredYear = year)

        val safeProfile = profile.sanitizedPlanLimits()
        val deductibleLimit = safeProfile.annualDeductibleLimit.takeIf { it > 0 } ?: DEFAULT_DEDUCTIBLE
        val oopMax = safeProfile.annualOutOfPocketMax.takeIf { it > 0 } ?: DEFAULT_OOP_MAX

        val deductiblePaid = summary.totalDeductible
        val copayPaid = summary.totalCopay
        val coinsurancePaid = summary.totalCoinsurance
        val patientResp = summary.totalPatientResponsibility

        val deductibleProgress = (deductiblePaid / deductibleLimit.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
        val oopProgress = (patientResp / oopMax.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

        val monthlyBilled = monthlyTotals(yearRecords) { it.totalBilledAmount }
        val monthlyPatient = monthlyTotals(yearRecords) { it.totalPatientResponsibility }
        val monthlyBilledNorm = normalizeSeries(monthlyBilled)
        val trajectory = buildTrajectory(monthlyPatient, deductibleLimit)

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val thisMonthBilled = monthlyBilled.getOrElse(currentMonth) { 0.0 }
        val avgBilled = monthlyBilled.filter { it > 0.0 }.average().takeIf { !it.isNaN() } ?: 0.0
        val velocity = if (avgBilled <= 0.0) {
            0.25f
        } else {
            (thisMonthBilled / avgBilled).toFloat().coerceIn(0.2f, 1f)
        }

        val monthsElapsed = (currentMonth + 1).coerceAtLeast(1)
        val projectedDeductible = (deductiblePaid / monthsElapsed) * 12
        val onTrackEarly = projectedDeductible >= deductibleLimit && monthsElapsed < 10

        return YtdDeductibleBentoSnapshot(
            year = year,
            totalBilled = summary.totalBilled,
            totalPatientResponsibility = patientResp,
            deductiblePaidYtd = deductiblePaid,
            copayPaidYtd = copayPaid,
            coinsurancePaidYtd = coinsurancePaid,
            deductibleLimit = deductibleLimit,
            outOfPocketMax = oopMax,
            deductibleProgress = deductibleProgress,
            outOfPocketProgress = oopProgress,
            monthlyBilledNormalized = monthlyBilledNorm,
            trajectoryNormalized = trajectory,
            spendingVelocity = velocity,
            onTrackEarlyDeductible = onTrackEarly
        )
    }

    fun buildInsuranceNewsBentoSnapshot(
        language: AppLanguage,
        releases: List<NewsRelease>,
        records: List<EobRecord>
    ): InsuranceNewsBentoSnapshot {
        val tickerHeadlines = releases.map { it.headline }.filter { it.isNotBlank() }
        val featured = releases.firstOrNull()
        val flaggedCount = records.sumOf { record ->
            EobAnalyzer.detectBillingIssues(record).count { it.severity != BillingIssueSeverity.Info }
        }
        val criticalRelease = releases.firstOrNull { isCriticalNewsRelease(it) }
        val criticalAlertActive = criticalRelease != null || flaggedCount > 0
        val actionSummary = when {
            criticalRelease != null -> criticalRelease.summary.trim()
            flaggedCount > 0 -> EobStrings.tf(language, "newsBentoCriticalBilling", flaggedCount)
            else -> EobStrings.t(language, "newsBentoTapToOpen")
        }
        return InsuranceNewsBentoSnapshot(
            tickerHeadlines = tickerHeadlines.ifEmpty {
                listOf(EobStrings.t(language, "insuranceNewsAllClear"))
            },
            previewHeadline = featured?.headline?.substringAfter(": ")?.trim()
                ?.ifBlank { featured.headline }
                ?: EobStrings.t(language, "newsBentoTickerFallback"),
            previewCompany = featured?.company.orEmpty().ifBlank {
                EobStrings.t(language, "insuranceNews")
            },
            criticalAlertActive = criticalAlertActive,
            actionSummary = actionSummary
        )
    }

    private val criticalNewsKeywords = listOf(
        "alert",
        "urgent",
        "critical",
        "denial",
        "denied",
        "out-of-network",
        "termination",
        "suspension",
        "warning",
        "fraud"
    )

    private fun isCriticalNewsRelease(release: NewsRelease): Boolean {
        val text = "${release.headline} ${release.summary}".lowercase()
        return criticalNewsKeywords.any { keyword -> text.contains(keyword) }
    }

    private fun cptShortLabel(language: AppLanguage, code: String, info: CptInfo): String {
        val key = "cptShort$code"
        val localized = EobStrings.t(language, key)
        if (localized != key) return localized
        return info.description
            .substringBefore(',')
            .take(28)
            .ifBlank { EobStrings.t(language, "unspecifiedProcedure") }
    }

    private fun resolveCptFlashcardDefinition(
        language: AppLanguage,
        code: String,
        info: CptInfo,
        chargeDescription: String
    ): String {
        val hasKnowledgeBaseEntry = EobKnowledgeBase.cptDescriptions.any {
            it.code.equals(code, ignoreCase = true)
        }
        if (hasKnowledgeBaseEntry && info.description.isNotBlank()) {
            return info.description
        }
        if (chargeDescription.isNotBlank()) {
            return chargeDescription
        }
        if (info.description.isNotBlank()) {
            return info.description
        }
        return EobStrings.t(language, "unspecifiedProcedure")
    }

    private fun resolveCptFlashcardShortName(
        language: AppLanguage,
        code: String,
        definition: String
    ): String {
        val localizedKey = "cptShort$code"
        val localized = EobStrings.t(language, localizedKey)
        if (localized != localizedKey) return localized
        return definition
            .substringBefore(',')
            .take(28)
            .trimEnd()
            .ifBlank { EobStrings.t(language, "unspecifiedProcedure") }
    }

    private fun monthlyTotals(
        records: List<EobRecord>,
        selector: (EobRecord) -> Double
    ): List<Double> {
        val buckets = DoubleArray(12)
        records.forEach { record ->
            val monthIndex = monthIndexFromRecord(record)
            if (monthIndex in 0..11) {
                buckets[monthIndex] += selector(record)
            }
        }
        return buckets.toList()
    }

    private fun monthIndexFromRecord(record: EobRecord): Int {
        val key = record.serviceDateSortKey
        if (key == Int.MAX_VALUE) return Calendar.getInstance().get(Calendar.MONTH)
        return ((key % 10000) / 100 - 1).coerceIn(0, 11)
    }

    private fun normalizeSeries(values: List<Double>): List<Float> {
        val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        return values.map { (it / max).toFloat().coerceIn(0f, 1f) }
    }

    private fun buildTrajectory(monthlyPatient: List<Double>, deductibleLimit: Double): List<Float> {
        if (deductibleLimit <= 0) return List(12) { 0f }
        var cumulative = 0.0
        return monthlyPatient.map { monthAmount ->
            cumulative += monthAmount
            (cumulative / deductibleLimit).toFloat().coerceIn(0f, 1.2f)
        }
    }
}
