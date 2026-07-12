package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiPr166Test {
    @Test
    fun cleanInsuranceCardRemovesDuplicateLabelsAndResizes() {
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        assertFalse(cardSource.contains("cleanInsuranceMemberIdSectionLabel"))
        assertFalse(cardSource.contains("cleanInsuranceGroupSectionLabel"))
        assertTrue(cardSource.contains("InsuranceCardMinHeight = 176.dp"))
        assertTrue(cardSource.contains("cleanInsuranceGroupNumberLabel"))
        assertTrue(cardSource.contains("cleanInsuranceCopaySectionDetail"))
        assertTrue(homeSource.contains("fillMaxWidth(0.98f)"))
    }

    @Test
    fun cleanInsuranceCopayLabelNoLongerIncludesPcpSpecParenthetical() {
        val stringsSource = readSource("data/EobStrings.kt")
        assertTrue(stringsSource.contains("\"cleanInsuranceCopayLabel\" to \"Copay\""))
        assertFalse(stringsSource.contains("\"cleanInsuranceCopayLabel\" to \"Copay (PCP/Spec)\""))
    }

    @Test
    fun taxVaultFilterCardMatchesCenteredMockLayout() {
        val filterSource = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        assertTrue(filterSource.contains("TaxVaultFilterHeaderRow"))
        assertTrue(filterSource.contains("drawTaxVaultDataStreams"))
        assertTrue(filterSource.contains("VaultPrimaryText = Color.White"))
        assertTrue(filterSource.contains("activeTrackColor = EobBrandBlue"))
        assertFalse(filterSource.contains("V2: Secure Lock Sequence"))
        assertTrue(homeSource.contains("fillMaxWidth(0.88f)"))
        assertTrue(homeSource.contains("contentAlignment = Alignment.Center"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
