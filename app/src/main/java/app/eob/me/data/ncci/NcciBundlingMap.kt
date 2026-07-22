package app.eob.me.data.ncci

/**
 * Master aggregation of local CMS NCCI PTP bundling rules.
 */
object NcciBundlingMap {
    val ncciBundlingRules: Map<String, Set<String>> by lazy {
        NcciBundlingSupport.mergeBundlingRules(
            NcciEmBundlingMap.rules,
            NcciLabBundlingMap.rules,
            NcciSurgeryBundlingMap.rules,
            NcciRadiologyBundlingMap.rules,
            NcciSpecialtyBundlingMap.rules,
        )
    }

    fun bundledCodesFor(cptCode: String): Set<String>? {
        val normalized = cptCode.trim().uppercase()
        if (normalized.isBlank()) return null
        return ncciBundlingRules[normalized]
    }

    val totalPairCount: Int
        get() = ncciBundlingRules.values.sumOf { it.size }
}
