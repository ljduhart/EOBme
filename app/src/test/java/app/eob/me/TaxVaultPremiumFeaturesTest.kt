package app.eob.me

import app.eob.me.data.FsaDoomsdayEngine
import app.eob.me.data.FsaDoomsdayPhase
import app.eob.me.data.VaultReceiptMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TaxVaultPremiumFeaturesTest {
    @Test
    fun fsaDoomsdayGreenPhaseInJanuary() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.YEAR, 2026)
        }
        val snapshot = FsaDoomsdayEngine.snapshot(
            calendar = calendar,
            fsaAllocation = 1000.0,
            eligibleClaimAmount = 450.0,
            nowMillis = calendar.timeInMillis
        )
        assertEquals(FsaDoomsdayPhase.GREEN, snapshot.phase)
        assertEquals("taxVaultFsaDoomsdayGreen", snapshot.messageKey)
    }

    @Test
    fun fsaDoomsdayOrangePhaseInOctober() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.YEAR, 2026)
        }
        val snapshot = FsaDoomsdayEngine.snapshot(
            calendar = calendar,
            fsaAllocation = 1000.0,
            eligibleClaimAmount = 800.0,
            nowMillis = calendar.timeInMillis
        )
        assertEquals(FsaDoomsdayPhase.ORANGE, snapshot.phase)
        assertTrue(snapshot.unspentAmount > 0.0)
    }

    @Test
    fun fsaDoomsdayRedPhaseInDecember() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 17)
            set(Calendar.YEAR, 2026)
        }
        val snapshot = FsaDoomsdayEngine.snapshot(
            calendar = calendar,
            fsaAllocation = 1000.0,
            eligibleClaimAmount = 200.0,
            nowMillis = calendar.timeInMillis
        )
        assertEquals(FsaDoomsdayPhase.RED, snapshot.phase)
        assertTrue(snapshot.pulseAlert)
        assertTrue(snapshot.daysRemaining in 1..31)
    }

    @Test
    fun vaultReceiptOcrParserExtractsAmountAndDate() {
        val parsed = VaultReceiptMapper.parseReceiptFromOcr(
            """
                CVS Pharmacy
                03/15/2026
                Total $50.00
            """.trimIndent()
        )
        assertEquals(50.0, parsed.amount, 0.01)
        assertEquals("03/15/2026", parsed.serviceDate)
        assertTrue(parsed.providerName.contains("CVS"))
    }
}
