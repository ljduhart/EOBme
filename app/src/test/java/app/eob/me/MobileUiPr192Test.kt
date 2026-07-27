package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr192Test {
    @Test
    fun smartCardsDoubleTapOpensDialerWhenPhoneDialUriPresent() {
        val careTeamSource = readSource("ui/components/home/HomeCareTeamCards.kt")
        assertTrue(careTeamSource.contains("onDoubleTap = {"))
        assertTrue(careTeamSource.contains("cardState.phoneDialUri != null"))
        assertTrue(careTeamSource.contains("DeviceCallingUtils.hasDialablePhone(doctor.phone)"))
        assertTrue(careTeamSource.contains("if (!DeviceCallingUtils.hasDialablePhone(phoneNumber)) return"))
        assertTrue(careTeamSource.contains("dialCareTeamPhone(context, language, doctor.phone)"))
        assertTrue(careTeamSource.contains("DeviceCallingUtils.safelyDialNumber"))
        assertFalse(careTeamSource.contains("awaitEachGesture"))
    }

    @Test
    fun eobHistoryShowsCptBilledAmountsAndClaimTotal() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val stringsSource = readSource("data/EobStrings.kt")
        assertTrue(stringsSource.contains("\"historyClaimTotal\""))
        assertTrue(historySource.contains("HistoryClaimTotalRow"))
        assertTrue(historySource.contains("charge.billedAmount.asCurrency()"))
        assertTrue(historySource.contains("record.totalBilledAmount.asCurrency()"))
        assertTrue(historySource.indexOf("HistoryClaimTotalRow") > historySource.indexOf("ReceiptCptLine"))
    }

    @Test
    fun eobHistoryBreakdownAndClaimTotalRemainUnchangedAsideFromCptSection() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(historySource.contains("HistoryPatientResponsibilityHeader"))
        assertTrue(historySource.contains("HistoryReceiptAmountBreakdown"))
        assertTrue(historySource.contains("HistoryAppealPillButtons"))
        assertFalse(historySource.contains("HistoryExteriorBilledAmount"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr192() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("HistoryClaimTotalRow"))
        assertFalse(splashSource.contains("phoneDialUri"))
        assertFalse(introSource.contains("historyClaimTotal"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
