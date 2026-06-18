package app.eob.me

import app.eob.me.data.EobRecord
import app.eob.me.data.ProviderSummary
import app.eob.me.ui.screens.NetworkStatus
import app.eob.me.ui.screens.toPremiumProviderSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedProviderDirectoryTest {
    @Test
    fun providerSummaryMapsToPremiumProviderWithNetworkStatus() {
        val inNetworkRecord = sampleRecord(
            id = 1,
            providerName = "Downtown Medical",
            rawText = "In-network preventive visit"
        )
        val outOfNetworkRecord = sampleRecord(
            id = 2,
            providerName = "Regional Clinic",
            rawText = "Out-of-network specialist visit"
        )
        val summary = ProviderSummary(
            providerName = "Regional Clinic",
            eobCount = 1,
            totalBilled = 250.0,
            totalInsurancePaid = 100.0,
            totalPatientResponsibility = 150.0,
            lastServiceDate = "01/15/2026"
        )

        val premium = summary.toPremiumProviderSummary(listOf(inNetworkRecord, outOfNetworkRecord))

        assertEquals("Regional Clinic", premium.id)
        assertEquals("Regional Clinic", premium.name)
        assertEquals(1, premium.eobCount)
        assertEquals(250.0, premium.totalBilled, 0.0)
        assertEquals(100.0, premium.totalPaid, 0.0)
        assertEquals(150.0, premium.totalResponsibility, 0.0)
        assertEquals(NetworkStatus.OUT_OF_NETWORK, premium.networkStatus)
    }

    @Test
    fun animatedProviderDirectoryUsesKeyedLazyColumnAndAssuranceBadge() {
        val source = readSource("ui/screens/AnimatedProviderDirectory.kt")
        assertTrue(source.contains("itemsIndexed(items = providers, key = { _, item -> item.id })"))
        assertTrue(source.contains("enum class NetworkStatus"))
        assertTrue(source.contains("HorizontalDivider"))
        assertTrue(source.contains("AssuranceBadge"))
        assertTrue(source.contains("MutableTransitionState"))
    }

    @Test
    fun providerDirectoryScreenDelegatesToAnimatedDirectory() {
        val screenSource = readSource("ui/screens/ProviderDirectoryScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(screenSource.contains("AnimatedProviderDirectoryScreen"))
        assertTrue(screenSource.contains("toPremiumProviderSummary"))
        assertTrue(navSource.contains("openProviderRecordHistory"))
        assertTrue(navSource.contains("onViewProviderRecords"))
    }

    private fun sampleRecord(id: Int, providerName: String, rawText: String): EobRecord {
        return EobRecord(
            id = id,
            sourceName = "Test",
            providerName = providerName,
            insuranceName = "Test Insurance",
            serviceDate = "01/15/2026",
            serviceDateSortKey = 20260115,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = rawText
        )
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        val file = candidates.first { it.isFile }
        return file.readText()
    }
}
