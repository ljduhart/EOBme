package com.eobme.app.ocr

import com.eobme.app.reference.CptCodes
import com.eobme.app.reference.InsuranceCompanies
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class CptEntry(
    val code: String,
    val description: String,
    val category: String
)

data class ParsedEobData(
    val providerName: String = "",
    val insuranceName: String = "",
    val billedAmount: Double = 0.0,
    val insurancePaid: Double = 0.0,
    val contractualAdjustment: Double = 0.0,
    val copay: Double = 0.0,
    val deductible: Double = 0.0,
    val coinsurance: Double = 0.0,
    val dateOfService: Long = System.currentTimeMillis(),
    val cptCodes: List<CptEntry> = emptyList(),
    val subscriberId: String = ""
)

object EobOcrParser {

    private val DATE_PATTERNS = listOf(
        Pattern.compile("\\b(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})\\b"),
        Pattern.compile("\\b(\\d{4})[/\\-](\\d{1,2})[/\\-](\\d{1,2})\\b"),
        Pattern.compile("\\b(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{2})\\b")
    )

    private val MONEY_PATTERN = Pattern.compile("\\$?([\\d,]+\\.\\d{2})")

    private val CPT_PATTERN = Pattern.compile("\\b([1-9A-Ja-j]\\d{4})\\b")

    private val PROVIDER_KEYWORDS = listOf(
        "provider", "physician", "doctor", "dr.", "dr ", "clinic", "medical group",
        "health center", "hospital", "rendering", "servicing", "attending"
    )

    private val BILLED_KEYWORDS = listOf(
        "billed", "charges", "total charge", "amount billed", "submitted amount",
        "claim amount", "total amount", "gross charge"
    )

    private val PAID_KEYWORDS = listOf(
        "plan paid", "insurance paid", "amount paid", "benefit paid", "paid amount",
        "plan payment", "allowed paid", "insurance payment", "we paid", "paid by plan"
    )

    private val ADJUSTMENT_KEYWORDS = listOf(
        "contractual", "adjustment", "discount", "write-off", "write off",
        "not covered", "non-covered", "contract adjustment", "plan discount"
    )

    private val COPAY_KEYWORDS = listOf(
        "copay", "co-pay", "copayment", "co-payment", "office visit copay"
    )

    private val DEDUCTIBLE_KEYWORDS = listOf(
        "deductible", "deduct", "ded", "annual deductible", "remaining deductible"
    )

    private val COINSURANCE_KEYWORDS = listOf(
        "coinsurance", "co-insurance", "coins", "your share", "member responsibility",
        "patient responsibility", "you owe", "amount you owe", "your cost"
    )

    private val SUBSCRIBER_KEYWORDS = listOf(
        "subscriber", "member id", "member #", "policy", "id number",
        "identification", "member number", "subscriber id", "policy number"
    )

    fun parse(ocrText: String): ParsedEobData {
        val lines = ocrText.lines()
        val lowerText = ocrText.lowercase()

        val insuranceName = InsuranceCompanies.matchInsuranceName(ocrText)
        val providerName = extractProvider(lines)
        val dateOfService = extractDate(ocrText)
        val cptCodes = extractCptCodes(ocrText)
        val subscriberId = extractWithKeywords(lines, SUBSCRIBER_KEYWORDS, extractId = true)

        val billedAmount = extractAmount(lines, BILLED_KEYWORDS)
        val insurancePaid = extractAmount(lines, PAID_KEYWORDS)
        val contractualAdjustment = extractAmount(lines, ADJUSTMENT_KEYWORDS)
        val copay = extractAmount(lines, COPAY_KEYWORDS)
        val deductible = extractAmount(lines, DEDUCTIBLE_KEYWORDS)
        val coinsurance = extractAmount(lines, COINSURANCE_KEYWORDS)

        return ParsedEobData(
            providerName = providerName,
            insuranceName = insuranceName,
            billedAmount = billedAmount,
            insurancePaid = insurancePaid,
            contractualAdjustment = contractualAdjustment,
            copay = copay,
            deductible = deductible,
            coinsurance = coinsurance,
            dateOfService = dateOfService,
            cptCodes = cptCodes,
            subscriberId = subscriberId
        )
    }

    fun isValidCptCode(code: String): Boolean = CptCodes.isValidCode(code)

    private fun extractProvider(lines: List<String>): String {
        for (line in lines) {
            val lower = line.lowercase()
            for (keyword in PROVIDER_KEYWORDS) {
                if (lower.contains(keyword)) {
                    val afterKeyword = line.substringAfter(keyword, "")
                        .substringAfter(":", "")
                        .trim()
                        .trimStart('-', ' ')
                    if (afterKeyword.length > 2) return afterKeyword.take(100)
                }
            }
        }
        for (i in lines.indices) {
            val lower = lines[i].lowercase().trim()
            if (lower.contains("provider") || lower.contains("physician") || lower.contains("doctor")) {
                if (i + 1 < lines.size) {
                    val next = lines[i + 1].trim()
                    if (next.isNotBlank() && next.length > 2) return next.take(100)
                }
            }
        }
        return ""
    }

    private fun extractDate(text: String): Long {
        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return try {
                    val matched = matcher.group()
                    val formats = listOf(
                        SimpleDateFormat("MM/dd/yyyy", Locale.US),
                        SimpleDateFormat("yyyy/MM/dd", Locale.US),
                        SimpleDateFormat("MM-dd-yyyy", Locale.US),
                        SimpleDateFormat("yyyy-MM-dd", Locale.US),
                        SimpleDateFormat("MM/dd/yy", Locale.US),
                        SimpleDateFormat("MM-dd-yy", Locale.US)
                    )
                    for (fmt in formats) {
                        try {
                            val date = fmt.parse(matched)
                            if (date != null) return date.time
                        } catch (_: Exception) { }
                    }
                    System.currentTimeMillis()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }
        }
        return System.currentTimeMillis()
    }

    private fun extractCptCodes(text: String): List<CptEntry> {
        val results = mutableListOf<CptEntry>()
        val seen = mutableSetOf<String>()
        val matcher = CPT_PATTERN.matcher(text)
        while (matcher.find()) {
            val code = matcher.group(1)?.uppercase() ?: continue
            if (code in seen) continue
            if (!isValidCptCode(code)) continue
            seen.add(code)
            val info = CptCodes.lookup(code)
            results.add(
                CptEntry(
                    code = code,
                    description = info?.description ?: "",
                    category = info?.category ?: CptCodes.categorize(code)
                )
            )
        }
        return results
    }

    private fun extractAmount(lines: List<String>, keywords: List<String>): Double {
        for (line in lines) {
            val lower = line.lowercase()
            for (keyword in keywords) {
                if (lower.contains(keyword)) {
                    val matcher = MONEY_PATTERN.matcher(line)
                    if (matcher.find()) {
                        return try {
                            matcher.group(1)?.replace(",", "")?.toDouble() ?: 0.0
                        } catch (_: Exception) { 0.0 }
                    }
                }
            }
        }
        return 0.0
    }

    private fun extractWithKeywords(lines: List<String>, keywords: List<String>, extractId: Boolean = false): String {
        for (line in lines) {
            val lower = line.lowercase()
            for (keyword in keywords) {
                if (lower.contains(keyword)) {
                    val afterKeyword = line.substringAfter(keyword, "")
                        .substringAfter(":", "")
                        .trim()
                        .trimStart('-', '#', ' ')
                    if (afterKeyword.isNotBlank()) {
                        return if (extractId) {
                            afterKeyword.split(" ", "\t").first().trim()
                        } else {
                            afterKeyword.take(100)
                        }
                    }
                }
            }
        }
        return ""
    }
}
