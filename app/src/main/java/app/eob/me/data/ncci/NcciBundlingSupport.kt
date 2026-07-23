package app.eob.me.data.ncci

/**
 * Shared helpers for parsing local NCCI bundling pair lines.
 */
internal object NcciBundlingSupport {
    fun rulesFromLines(lines: List<String>): Map<String, Set<String>> {
        val merged = linkedMapOf<String, MutableSet<String>>()
        lines.forEach { line ->
            val parts = line.split('|', limit = 2)
            if (parts.size != 2) return@forEach
            val column1 = parts[0].trim().uppercase()
            val column2 = parts[1].trim().uppercase()
            if (column1.isBlank() || column2.isBlank()) return@forEach
            merged.getOrPut(column1) { linkedSetOf() }.add(column2)
        }
        return merged.mapValues { entry -> entry.value.toSet() }
    }

    fun mergeBundlingRules(vararg maps: Map<String, Set<String>>): Map<String, Set<String>> {
        val merged = linkedMapOf<String, MutableSet<String>>()
        maps.forEach { map ->
            map.forEach { (column1, column2Codes) ->
                val bucket = merged.getOrPut(column1.trim().uppercase()) { linkedSetOf() }
                column2Codes.forEach { column2 ->
                    val bundled = column2.trim().uppercase()
                    if (bundled.isNotBlank()) {
                        bucket += bundled
                    }
                }
            }
        }
        return merged.mapValues { entry -> entry.value.toSet() }
    }
}
