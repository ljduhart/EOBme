package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiPr167Test {
    @Test
    fun taxVaultFilterMatchesMockupHeaderAndBinaryBeam() {
        val filterSource = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        val scannerSource = readSource("ui/components/home/TitaniumVaultBiometricScanner.kt")
        assertTrue(filterSource.contains("TaxVaultBrandingHeader"))
        assertTrue(filterSource.contains("TaxVaultVaultIconCluster"))
        assertTrue(filterSource.contains("drawTaxVaultCentralLightRay"))
        assertTrue(filterSource.contains("VaultKeyCardsAndBinaryGlyph"))
        assertTrue(filterSource.contains("scannerSize = 48.dp"))
        assertTrue(scannerSource.contains("scannerSize: Dp = 72.dp"))
        assertFalse(filterSource.contains("V2: Secure Lock Sequence"))
    }

    @Test
    fun biometricScannerPlacedInHeaderTopRightWhenEnabled() {
        val source = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        val headerBlock = source.substringAfter("private fun TaxVaultBrandingHeader")
            .substringBefore("@Composable\nprivate fun TaxVaultVaultIconCluster")
        assertTrue(headerBlock.contains("if (showTitaniumDoor)"))
        assertTrue(headerBlock.contains("TitaniumVaultBiometricScanner"))
        assertTrue(headerBlock.contains("Modifier.align(Alignment.TopEnd)"))
        assertTrue(headerBlock.contains("Tax Vault Filter") || headerBlock.contains("taxVaultFilterTitle"))
    }

    @Test
    fun vaultLogoAndTitleFollowMockupStack() {
        val source = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        val headerBlock = source.substringAfter("private fun TaxVaultBrandingHeader")
            .substringBefore("@Composable\nprivate fun TaxVaultVaultIconCluster")
        assertTrue(headerBlock.indexOf("TaxVaultVaultIconCluster") < headerBlock.indexOf("\"taxVaultFilterTitle\""))
        assertTrue(headerBlock.contains("TaxVaultVaultIconCluster"))
        assertTrue(headerBlock.contains("text = "))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
