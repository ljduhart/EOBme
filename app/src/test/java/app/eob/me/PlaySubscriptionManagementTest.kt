package app.eob.me

import app.eob.me.billing.PlaySubscriptionManagement
import app.eob.me.data.SubscriptionCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaySubscriptionManagementTest {
    @Test
    fun playSubscriptionManagementBuildsGooglePlayManagementLinks() {
        val source = readSource("billing/PlaySubscriptionManagement.kt")
        assertTrue(source.contains("store/account/subscriptions"))
        assertTrue(source.contains("appendQueryParameter(\"package\""))
        assertTrue(source.contains("appendQueryParameter(\"sku\""))
        assertTrue(source.contains("Intent.ACTION_VIEW"))
        assertEquals(
            SubscriptionCatalog.GOLD_SUBSCRIPTION_ID,
            SubscriptionCatalog.subscriptionProductId(app.eob.me.data.SubscriptionTier.Gold)
        )
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
