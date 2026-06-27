package app.eob.me.network

/**
 * Parses currency strings from Veryfi indexed EOB fields into [Double] values.
 *
 * Handles US (`$ 1,578.00`) and European (`$ 511,42`) formats plus raw numeric inputs.
 */
object VeryfiCurrencyParser {
    fun parse(value: Any?): Double {
        return when (value) {
            null -> 0.0
            is Number -> value.toDouble()
            is String -> parseString(value)
            else -> parseString(value.toString())
        }
    }

    fun parseString(raw: String): Double {
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
            commaCount == 1 && dotCount == 0 -> normalized.replace(",", ".")
            commaCount >= 1 && dotCount == 1 -> normalized.replace(",", "")
            else -> normalized.replace(",", "")
        }
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    fun firstPositive(vararg values: Double): Double =
        values.firstOrNull { it > 0.0 } ?: 0.0
}
