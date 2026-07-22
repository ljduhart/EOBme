package app.eob.me

import android.content.Context
import app.eob.me.data.SubscriptionUsageStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SubscriptionUsageTest {
    private lateinit var store: SubscriptionUsageStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("eobme_subscription_usage", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        store = SubscriptionUsageStore(context)
    }

    @Test
    fun monthlyEobScanCountResetsOnNewMonthKey() {
        val januaryKey = 202_600
        val februaryKey = 202_601

        assertEquals(0, store.readMonthlyEobScanCount(januaryKey))
        assertEquals(1, store.incrementMonthlyEobScanCount(januaryKey))
        assertEquals(1, store.readMonthlyEobScanCount(januaryKey))
        assertEquals(0, store.readMonthlyEobScanCount(februaryKey))

        assertEquals(1, store.incrementMonthlyEobScanCount(februaryKey))
        assertEquals(1, store.readMonthlyEobScanCount(februaryKey))
        assertEquals(0, store.readMonthlyEobScanCount(januaryKey))
    }

    @Test
    fun monthlyAppealLetterCountTracksIndependentlyFromScans() {
        val monthKey = 202_605

        assertEquals(1, store.incrementMonthlyAppealLetterCount(monthKey))
        assertEquals(2, store.incrementMonthlyAppealLetterCount(monthKey))
        assertEquals(2, store.readMonthlyAppealLetterCount(monthKey))
        assertEquals(0, store.readMonthlyEobScanCount(monthKey))
    }
}
