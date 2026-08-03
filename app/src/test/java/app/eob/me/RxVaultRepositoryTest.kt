package app.eob.me

import app.eob.me.data.local.entity.MedicationRecord
import app.eob.me.data.repository.RxVaultRepository
import app.eob.me.work.RefillAlertWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RxVaultRepositoryTest {
    @Test
    fun refillAlertDelayIsPositive() {
        val now = System.currentTimeMillis()
        val record = MedicationRecord(
            id = 1L,
            medicationName = "Test",
            dosage = "5mg",
            quantity = 30,
            refillDate = now + 30L * 86_400_000L,
            copayAmount = 0.0,
            isFsaEligible = false
        )
        val delay = RxVaultRepository.refillAlertDelayMillis(record, now)
        assertTrue(delay >= 60_000L)
    }

    @Test
    fun refillNotificationIdIsStableAndPositive() {
        val id = RefillAlertWorker.notificationId(9_223_372_036_854_775L)
        assertTrue(id > 0)
        assertEquals(RefillAlertWorker.notificationId(42L), RefillAlertWorker.notificationId(42L))
    }
}
