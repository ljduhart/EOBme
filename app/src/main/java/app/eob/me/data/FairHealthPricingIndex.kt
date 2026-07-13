package app.eob.me.data

import java.util.Locale

/**
 * Reference fair-health style pricing used for CPT bento trend gauges (not a live API).
 */
object FairHealthPricingIndex {
    private val fairPrices: Map<String, Double> = mapOf(
        "99202" to 95.0,
        "99203" to 125.0,
        "99204" to 185.0,
        "99205" to 245.0,
        "99211" to 45.0,
        "99212" to 75.0,
        "99213" to 115.0,
        "99214" to 165.0,
        "99215" to 225.0,
        "80053" to 42.0,
        "80061" to 38.0,
        "85025" to 28.0,
        "71046" to 120.0,
        "93000" to 55.0
    )

    fun fairPriceFor(code: String): Double {
        val normalized = code.uppercase(Locale.ROOT)
        return fairPrices[normalized] ?: estimateFromCategory(normalized)
    }

    private fun estimateFromCategory(code: String): Double {
        val category = EobKnowledgeBase.cptInfoFor(code).category
        return when (category) {
            CptCategory.OfficeVisit -> 130.0
            CptCategory.Lab -> 45.0
            CptCategory.Hospital -> 280.0
            CptCategory.XRay -> 220.0
            CptCategory.Dme -> 90.0
            CptCategory.Injection -> 75.0
            CptCategory.Other -> 100.0
        }
    }

    fun priceRatio(billed: Double, code: String): Double {
        val fair = fairPriceFor(code).coerceAtLeast(1.0)
        return (billed / fair).coerceIn(0.25, 2.5)
    }
}
