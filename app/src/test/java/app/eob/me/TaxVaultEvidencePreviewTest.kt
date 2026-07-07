package app.eob.me

import android.os.Looper
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.UserProfile
import app.eob.me.data.VaultEvidencePreviewDetail
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TaxVaultEvidencePreviewTest {
    private val profile = UserProfile(
        firstName = "Jane",
        lastName = "Doe",
        email = "jane@example.com",
        city = "Austin",
        state = "TX"
    )

    @Test
    fun selectEvidencePreviewStoresIdAndResolvesEobDetail() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        val record = sampleEob(firestoreId = "vault-eob-1", provider = "Lakeside Clinic")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)

        viewModel.selectTaxVaultEvidencePreview(record.historyListKey())
        assertEquals(record.historyListKey(), viewModel.uiState.value.taxVaultEvidencePreviewId)

        val detail = viewModel.taxVaultEvidencePreviewDetail(record.historyListKey())
        assertTrue(detail is VaultEvidencePreviewDetail.Eob)
        assertEquals("Lakeside Clinic", (detail as VaultEvidencePreviewDetail.Eob).record.providerName)
    }

    @Test
    fun dismissEvidencePreviewClearsSelection() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        val record = sampleEob(firestoreId = "vault-eob-2", provider = "Metro Health")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)

        viewModel.selectTaxVaultEvidencePreview(record.historyListKey())
        viewModel.dismissTaxVaultEvidencePreview()

        assertNull(viewModel.uiState.value.taxVaultEvidencePreviewId)
    }

    @Test
    fun evidencePreviewUiWiresBlurOverlayAndTapSelection() {
        val screenSource = readSource("ui/screens/TaxVaultScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")

        assertTrue(screenSource.contains("VaultEvidencePreviewOverlay"))
        assertTrue(screenSource.contains("Modifier.blur(12.dp)"))
        assertTrue(screenSource.contains("onEvidenceSelected"))
        assertTrue(navSource.contains("selectTaxVaultEvidencePreview"))
        assertTrue(navSource.contains("dismissTaxVaultEvidencePreview"))
        assertTrue(navSource.contains("EobRoute.TaxVault.route"))
        assertTrue(viewModelSource.contains("taxVaultEvidencePreviewId"))
        assertTrue(viewModelSource.contains("VaultEvidencePreviewDetail"))
    }

    private fun waitForHubRecords(viewModel: EobViewModel) {
        val deadlineMs = System.currentTimeMillis() + 10_000
        while (viewModel.eobRecords.value.isEmpty() && System.currentTimeMillis() < deadlineMs) {
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertTrue("Timed out waiting for hub EOB records", viewModel.eobRecords.value.isNotEmpty())
    }

    private fun sampleEob(firestoreId: String, provider: String): EobRecord {
        return EobRecord(
            id = 1,
            firestoreId = firestoreId,
            sourceName = "Veryfi",
            providerName = provider,
            insuranceName = "Aetna",
            serviceDate = "03/15/2026",
            serviceDateSortKey = 20260315,
            charges = listOf(
                EobCharge(
                    cptCode = "99213",
                    cptDescription = "Office visit",
                    category = app.eob.me.data.CptCategory.OfficeVisit,
                    billedAmount = 180.0,
                    insurancePaidAmount = 120.0,
                    contractualAdjustmentAmount = 30.0,
                    copayAmount = 25.0,
                    deductibleAmount = 5.0,
                    coinsuranceAmount = 0.0,
                    serviceDate = "03/15/2026"
                )
            ),
            duplicateChargeWarnings = emptyList(),
            rawText = "{}",
            storageDownloadUrl = "https://example.com/eob.jpg",
            isHsaEligible = true
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
