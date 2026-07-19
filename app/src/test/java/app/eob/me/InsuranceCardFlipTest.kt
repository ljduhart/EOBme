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
        assertTrue(source.contains("InsuranceCardBackFace"))
        assertTrue(source.contains("InsuranceCardFlipButton"))
    }

    @Test
    fun insuranceCardBackUsesPillBottleAndNotepadPanels() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("InsuranceCardBackHub"))
        assertTrue(source.contains("InsuranceCardMedicationsPanel"))
        assertTrue(source.contains("InsuranceCardDigitalNotepadPanel"))
        assertTrue(source.contains("onMedicationDosageScheduleChange"))
        assertTrue(source.contains("onMedicationAllergiesChange"))
        assertTrue(source.contains("onDoctorQuickNotesChange"))
    }

    @Test
    fun insuranceCardNotesAllowMultilineSpacingInput() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("singleLine = false"))
        assertTrue(source.contains("KeyboardCapitalization.Sentences"))
        assertTrue(source.contains("localPrescriptions"))
        assertFalse(source.contains("placeholder = {"))
    }

    @Test
    fun viewModelGuardsInsuranceMetadataDuringLocalEdits() {
        val source = readSource("viewmodel/EobViewModel.kt")
        assertTrue(source.contains("shouldIgnoreInsuranceMetadataSnapshot"))
        assertTrue(source.contains("insuranceNotesLocalEditAtMillis"))
        assertTrue(source.contains("INSURANCE_CARD_NOTES_REMOTE_GUARD_MS"))
    }

    @Test
    fun homeScreenWiresInsuranceCardNotesThroughViewModel() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(homeSource.contains("onInsurancePrescriptionsChange"))
        assertTrue(homeSource.contains("onInsuranceDoctorNotesChange"))
        assertTrue(homeSource.contains("profile.currentPrescriptions"))
        assertTrue(homeSource.contains("profile.medicationDosageSchedule"))
        assertTrue(homeSource.contains("profile.medicationAllergies"))
        assertTrue(homeSource.contains("profile.doctorQuickNotes"))
        assertTrue(navSource.contains("updateInsuranceCardPrescriptions"))
        assertTrue(navSource.contains("updateInsuranceCardDosageSchedule"))
        assertTrue(navSource.contains("updateInsuranceCardAllergies"))
        assertTrue(navSource.contains("updateInsuranceCardDoctorNotes"))
        assertTrue(viewModelSource.contains("observeInsuranceCardMetadata"))
        assertTrue(viewModelSource.contains("scheduleInsuranceCardNotesPersist"))
    }

    @Test
    fun cleanInsuranceCardBackHandlerFlipsToFrontBeforeHubExit() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("BackHandler(enabled = flipped)"))
        assertTrue(source.contains("InsuranceCardBackMode.Hub"))
    }

    @Test
    fun observeProfilePreservesInsuranceCardNotesInViewModel() {
        val source = readSource("viewmodel/EobViewModel.kt")
        val observeBlock = source.substringAfter("private fun observeProfile")
            .substringBefore("fun fetchHistoryFromFirestore")
        assertTrue(observeBlock.contains("currentPrescriptions = _syncProfile.value.currentPrescriptions"))
        assertTrue(observeBlock.contains("medicationDosageSchedule = _syncProfile.value.medicationDosageSchedule"))
        assertTrue(observeBlock.contains("medicationAllergies = _syncProfile.value.medicationAllergies"))
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
            medicationDosageSchedule = "Morning",
            medicationAllergies = "None",
            doctorQuickNotes = "Note A"
        )
        val updated = viewModel.applyInsuranceCardNotes(
            profile = profile,
            currentPrescriptions = "Rx B",
            medicationDosageSchedule = "Evening",
            medicationAllergies = "Sulfa",
            doctorQuickNotes = "Note B"
        )
        assertEquals("Jane", updated.firstName)
        assertEquals("Aetna", updated.insuranceName)
        assertEquals("ABC123", updated.insuranceId)
        assertEquals("Rx B", updated.currentPrescriptions)
        assertEquals("Evening", updated.medicationDosageSchedule)
        assertEquals("Sulfa", updated.medicationAllergies)
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
