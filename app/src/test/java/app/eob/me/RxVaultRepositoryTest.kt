package app.eob.me

import app.eob.me.data.local.entity.MedicationRecord
import app.eob.me.data.repository.RxVaultRepository
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
}
