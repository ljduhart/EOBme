package app.eob.me

import android.os.Looper
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.UserProfile
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TaxVaultMiniatureEvidenceTest {
    private val profile = UserProfile(
        firstName = "Jane",
        lastName = "Doe",
        email = "jane@example.com"
    )

    @Test
    fun evidenceThumbnailsUseProviderDirectoryNameAndChargePreview() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        val record = sampleEob(
            firestoreId = "mini-eob-1",
            provider = "Lakeside Clinic",
            insurance = "Aetna"
        )
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)

        val thumbnail = viewModel.taxVaultEvidenceThumbnails().single()
        assertEquals("Lakeside Clinic", thumbnail.providerName)
        assertFalse(thumbnail.isReceipt)
        assertEquals("03/15/2026", thumbnail.serviceDate)
        assertTrue(thumbnail.amountDisplay.contains("30"))
        assertEquals("99213", thumbnail.chargePreviewLines.first().code)
        assertFalse(thumbnail.providerName.equals("Aetna", ignoreCase = true))
    }

    @Test
    fun miniatureEvidenceUiUsesPolaroidCardsAndSingleAddReceiptButton() {
        val screenSource = readSource("ui/screens/TaxVaultScreen.kt")
        val uiSource = readSource("ui/components/taxvault/TaxVaultEvidenceUi.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val modelsSource = readSource("data/TaxVaultModels.kt")
        val navSource = readSource("navigation/EobNavHost.kt")

        assertTrue(screenSource.contains("MiniaturePolaroidEvidenceCard"))
        assertTrue(screenSource.contains("VaultAddReceiptButton"))
        assertEquals(1, screenSource.split("VaultAddReceiptButton(").size - 1)
        assertFalse(screenSource.contains("floatingActionButton"))
        assertTrue(screenSource.contains("onEvidenceSelected"))
        assertTrue(navSource.contains("beginVaultReceiptScan"))
        assertTrue(navSource.contains("EobRoute.CameraCapture.route"))
        assertTrue(uiSource.contains("MiniatureEobPolaroidBody"))
        assertTrue(uiSource.contains("MiniatureReceiptPolaroidBody"))
        assertTrue(uiSource.contains("providerName.uppercase()"))
        assertTrue(uiSource.contains("VaultSparkleAccent"))
        assertTrue(viewModelSource.contains("resolveProviderDirectoryName"))
        assertTrue(viewModelSource.contains("VaultEvidenceChargePreview"))
        assertTrue(modelsSource.contains("data class VaultEvidenceChargePreview"))
    }

    @Test
    fun miniatureEvidenceTitleMatchesDesignCopy() {
        assertEquals(
            "Miniature Evidence",
            app.eob.me.data.EobStrings.t(app.eob.me.data.AppLanguage.English, "taxVaultEvidenceGalleryTitle")
        )
    }

    private fun waitForHubRecords(viewModel: EobViewModel) {
        var attempts = 0
        while (viewModel.eobRecords.value.isEmpty() && attempts < 1_000) {
            shadowOf(Looper.getMainLooper()).idle()
            attempts++
        }
        assertTrue(viewModel.eobRecords.value.isNotEmpty())
    }

    private fun sampleEob(
        firestoreId: String,
        provider: String,
        insurance: String
    ): EobRecord {
        return EobRecord(
            id = 1,
            firestoreId = firestoreId,
            sourceName = "Veryfi",
            providerName = provider,
            insuranceName = insurance,
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
            isHsaEligible = true,
            totalCopayAmount = 25.0,
            totalDeductibleAmount = 5.0
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
