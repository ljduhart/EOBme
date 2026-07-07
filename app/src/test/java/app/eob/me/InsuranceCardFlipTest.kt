package app.eob.me

import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsuranceCardFlipTest {
    @Test
    fun insuranceCardNotesFieldsHaveNoPlaceholderPrefill() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertFalse(source.contains("insuranceCardPrescriptionsPlaceholder"))
        assertFalse(source.contains("insuranceCardDoctorNotesPlaceholder"))
        assertFalse(source.contains("placeholder = {"))
    }

    @Test
    fun cleanInsuranceCardUsesRotationFlipAnimation() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("graphicsLayer"))
        assertTrue(source.contains("rotationY"))
        assertTrue(source.contains("insurance_card_rotation"))
        assertTrue(source.contains("InsuranceCardNotesBackFace"))
        assertTrue(source.contains("InsuranceCardFlipButton"))
    }

    @Test
    fun insuranceCardNotesBackExposesPrescriptionAndDoctorFields() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("insuranceCardPrescriptionsLabel"))
        assertTrue(source.contains("insuranceCardDoctorNotesLabel"))
        assertTrue(source.contains("onCurrentPrescriptionsChange"))
        assertTrue(source.contains("onDoctorQuickNotesChange"))
    }

    @Test
    fun homeScreenWiresInsuranceCardNotesThroughViewModel() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(homeSource.contains("onInsurancePrescriptionsChange"))
        assertTrue(homeSource.contains("onInsuranceDoctorNotesChange"))
        assertTrue(homeSource.contains("profile.currentPrescriptions"))
        assertTrue(homeSource.contains("profile.doctorQuickNotes"))
        assertTrue(navSource.contains("updateInsuranceCardPrescriptions"))
        assertTrue(navSource.contains("updateInsuranceCardDoctorNotes"))
        assertTrue(viewModelSource.contains("observeInsuranceCardMetadata"))
        assertTrue(viewModelSource.contains("scheduleInsuranceCardNotesPersist"))
    }

    @Test
    fun cleanInsuranceCardBackHandlerFlipsToFrontBeforeHubExit() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("BackHandler(enabled = flipped)"))
    }

    @Test
    fun observeProfilePreservesInsuranceCardNotesInViewModel() {
        val source = readSource("viewmodel/EobViewModel.kt")
        val observeBlock = source.substringAfter("private fun observeProfile")
            .substringBefore("fun fetchHistoryFromFirestore")
        assertTrue(observeBlock.contains("currentPrescriptions = _syncProfile.value.currentPrescriptions"))
        assertTrue(observeBlock.contains("doctorQuickNotes = _syncProfile.value.doctorQuickNotes"))
    }

    @Test
    fun applyInsuranceCardNotesPreservesOtherProfileFields() {
        val viewModel = EobViewModel()
        val profile = app.eob.me.data.UserProfile(
            firstName = "Jane",
            insuranceName = "Aetna",
            insuranceId = "ABC123",
            currentPrescriptions = "Rx A",
            doctorQuickNotes = "Note A"
        )
        val updated = viewModel.applyInsuranceCardNotes(
            profile = profile,
            currentPrescriptions = "Rx B",
            doctorQuickNotes = "Note B"
        )
        assertEquals("Jane", updated.firstName)
        assertEquals("Aetna", updated.insuranceName)
        assertEquals("ABC123", updated.insuranceId)
        assertEquals("Rx B", updated.currentPrescriptions)
        assertEquals("Note B", updated.doctorQuickNotes)
    }

    @Test
    fun paywallAllowsFreeTierSelectionForComparison() {
        val source = readSource("ui/screens/PaywallDialog.kt")
        assertTrue(source.contains("selectedTier == SubscriptionTier.Free"))
        assertFalse(source.contains("enabled = false,\n                onClick = {}"))
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
