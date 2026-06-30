package app.eob.me.data

import android.content.Context

/**
 * Persists monthly subscription usage counters locally.
 * [app.eob.me.viewmodel.EobViewModel] is the runtime source of truth.
 */
class SubscriptionUsageStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readMonthlyEobScanCount(timeKey: Int): Int {
        if (prefs.getInt(KEY_EOB_SCAN_MONTH, Int.MIN_VALUE) != timeKey) return 0
        return prefs.getInt(KEY_EOB_SCAN_COUNT, 0)
    }

    fun incrementMonthlyEobScanCount(timeKey: Int): Int {
        val next = readMonthlyEobScanCount(timeKey) + 1
        prefs.edit()
            .putInt(KEY_EOB_SCAN_MONTH, timeKey)
            .putInt(KEY_EOB_SCAN_COUNT, next)
            .apply()
        return next
    }

    fun readMonthlyAppealLetterCount(timeKey: Int): Int {
        if (prefs.getInt(KEY_APPEAL_MONTH, Int.MIN_VALUE) != timeKey) return 0
        return prefs.getInt(KEY_APPEAL_COUNT, 0)
    }

    fun incrementMonthlyAppealLetterCount(timeKey: Int): Int {
        val next = readMonthlyAppealLetterCount(timeKey) + 1
        prefs.edit()
            .putInt(KEY_APPEAL_MONTH, timeKey)
            .putInt(KEY_APPEAL_COUNT, next)
            .apply()
        return next
    }

    private companion object {
        const val PREFS_NAME = "eobme_subscription_usage"
        const val KEY_EOB_SCAN_MONTH = "eob_scan_month_key"
        const val KEY_EOB_SCAN_COUNT = "eob_scan_month_count"
        const val KEY_APPEAL_MONTH = "appeal_letter_month_key"
        const val KEY_APPEAL_COUNT = "appeal_letter_month_count"
    }
}
