package app.eob.me.network

import app.eob.me.data.NewsRelease

object InsuranceNewsRotation {
    const val BECKERS_VISIBLE_COUNT = 3
    const val HEALTHCARE_DIVE_VISIBLE_COUNT = 4
    const val ROTATION_PERIOD_MS = 2 * 60 * 60 * 1000L

    fun rotationSlot(nowMs: Long = System.currentTimeMillis()): Long {
        return nowMs / ROTATION_PERIOD_MS
    }

    fun millisUntilNextRotation(nowMs: Long = System.currentTimeMillis()): Long {
        val nextBoundary = (rotationSlot(nowMs) + 1) * ROTATION_PERIOD_MS
        return (nextBoundary - nowMs).coerceAtLeast(0L)
    }

    fun rotatedWindow(
        pool: List<NewsRelease>,
        visibleCount: Int,
        slot: Long
    ): List<NewsRelease> {
        if (pool.isEmpty() || visibleCount <= 0) return emptyList()
        val sorted = pool.sortedByDescending { RssNewsMapper.sortKey(it.date) }
        if (sorted.size <= visibleCount) return sorted
        val startIndex = ((slot.toInt() * visibleCount) % sorted.size).coerceAtLeast(0)
        return buildList(visibleCount) {
            var index = startIndex
            repeat(visibleCount) {
                add(sorted[index % sorted.size])
                index++
            }
        }
    }

    fun combineRotatedIntelligence(
        beckersPool: List<NewsRelease>,
        healthcareDivePool: List<NewsRelease>,
        slot: Long
    ): List<NewsRelease> {
        val beckers = rotatedWindow(
            pool = beckersPool,
            visibleCount = BECKERS_VISIBLE_COUNT,
            slot = slot
        )
        val dive = rotatedWindow(
            pool = healthcareDivePool,
            visibleCount = HEALTHCARE_DIVE_VISIBLE_COUNT,
            slot = slot
        )
        return (beckers + dive).sortedByDescending { RssNewsMapper.sortKey(it.date) }
    }
}
