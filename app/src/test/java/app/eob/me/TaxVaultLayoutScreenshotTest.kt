package app.eob.me

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.TaxVaultBudgetSummary
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.TaxVaultVisibilityMode
import app.eob.me.data.VaultEvidenceChargePreview
import app.eob.me.data.VaultEvidenceThumbnail
import app.eob.me.ui.components.home.TaxVaultVerticalFilterCard
import app.eob.me.ui.components.taxvault.VaultEvidenceCarousel
import app.eob.me.ui.theme.EOBmeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the real home Tax Vault section at phone and tablet widths, asserts key
 * controls are present, and writes PNG screenshots for visual audit.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28])
class TaxVaultLayoutScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleThumbnails = listOf(
        VaultEvidenceThumbnail(
            id = "eob-1",
            imageUrl = "https://example.com/eob.jpg",
            providerName = "Lakeside Clinic",
            rotationDegrees = -4f,
            isReceipt = false,
            serviceDate = "03/15/2026",
            amountDisplay = "$30.00",
            chargePreviewLines = listOf(
                VaultEvidenceChargePreview(code = "99213", amount = "$180.00")
            )
        ),
        VaultEvidenceThumbnail(
            id = "receipt-1",
            imageUrl = "https://example.com/receipt.jpg",
            providerName = "CVS Pharmacy",
            rotationDegrees = 3f,
            isReceipt = true,
            serviceDate = "03/10/2026",
            amountDisplay = "$18.50"
        ),
        VaultEvidenceThumbnail(
            id = "eob-2",
            imageUrl = "https://example.com/eob2.jpg",
            providerName = "Stanford Health",
            rotationDegrees = -2f,
            isReceipt = false,
            serviceDate = "02/28/2026",
            amountDisplay = "$125.00",
            chargePreviewLines = listOf(
                VaultEvidenceChargePreview(code = "80053", amount = "$48.00")
            )
        )
    )

    @Test
    @Config(qualifiers = "w360dp-h900dp")
    fun narrowPhoneTaxVaultSectionRendersEvidenceAndFullFilter() {
        renderTaxVaultHomeSection(darkModeEnabled = true)
        assertTaxVaultControlsVisible()
        saveScreenshot("narrow_phone_tax_vault.png")
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp")
    fun wideTabletTaxVaultSectionRendersEvidenceAndFullFilter() {
        renderTaxVaultHomeSection(darkModeEnabled = true)
        assertTaxVaultControlsVisible()
        saveScreenshot("wide_tablet_tax_vault.png")
    }

    private fun renderTaxVaultHomeSection(darkModeEnabled: Boolean) {
        val language = AppLanguage.English
        val homePrimaryText = if (darkModeEnabled) {
            Color(0xFFE8F4FF)
        } else {
            Color(0xFF0B1F45)
        }
        composeRule.setContent {
            EOBmeTheme(darkTheme = darkModeEnabled) {
                Surface(color = if (darkModeEnabled) Color(0xFF0A1628) else Color(0xFFF5F7FA)) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        val miniatureCardWidth = (maxWidth / 3.2f).coerceIn(88.dp, 108.dp)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VaultEvidenceCarousel(
                                language = language,
                                thumbnails = sampleThumbnails,
                                onEvidenceSelected = {},
                                titleColor = homePrimaryText,
                                miniatureCardWidth = miniatureCardWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TaxVaultVerticalFilterCard(
                                language = language,
                                darkModeEnabled = darkModeEnabled,
                                isGoldTier = true,
                                filterState = TaxVaultFilterState.HSA,
                                visibilityMode = TaxVaultVisibilityMode.GATED,
                                budgetSummary = TaxVaultBudgetSummary(
                                    eligibleAmount = 1250.0,
                                    allocationLimit = 3000.0
                                ),
                                onFilterSelected = {},
                                onVisibilityModeSelected = {},
                                onVaultDoorUnlocked = {},
                                enableShimmerOverlay = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertTaxVaultControlsVisible() {
        composeRule.onNodeWithText(
            EobStrings.t(AppLanguage.English, "taxVaultEvidenceGalleryTitle")
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            EobStrings.t(AppLanguage.English, "taxVaultFilterTitle")
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            EobStrings.t(AppLanguage.English, "taxVaultHsaFunds")
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            EobStrings.t(AppLanguage.English, "taxVaultFsaFunds")
        ).assertIsDisplayed()
        composeRule.onNodeWithText("LAKESIDE", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("CVS", substring = true).assertIsDisplayed()
    }

    private fun saveScreenshot(fileName: String) {
        val outputDir = File("/opt/cursor/artifacts/screenshots").also { it.mkdirs() }
        val outputFile = File(outputDir, fileName)
        val rootView = composeRule.activity.findViewById<ViewGroup>(android.R.id.content)
            ?.getChildAt(0)
            ?: error("Compose root view not found")
        val width = rootView.width.coerceAtLeast(1)
        val height = rootView.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        rootView.draw(canvas)
        FileOutputStream(outputFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assert(outputFile.exists() && outputFile.length() > 0L) {
            "Screenshot was not written: ${outputFile.absolutePath}"
        }
    }
}
