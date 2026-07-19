package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiPr173Test {
    @Test
    fun insuranceCardBackShowsPillBottleAndNotepadLaunchers() {
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val iconSource = readSource("ui/components/InsuranceCardBackIcons.kt")
        assertTrue(cardSource.contains("InsuranceCardBackHub"))
        assertTrue(cardSource.contains("InsuranceCardPillBottleIcon"))
        assertTrue(cardSource.contains("InsuranceCardNotepadIcon"))
        assertTrue(cardSource.contains("InsuranceCardMedicationsPanel"))
        assertTrue(cardSource.contains("InsuranceCardDigitalNotepadPanel"))
        assertFalse(cardSource.contains("insuranceCardPrescriptionsPlaceholder"))
        assertTrue(iconSource.contains("PillCapsuleRed"))
        assertTrue(iconSource.contains("NotepadPaper"))
    }

    @Test
    fun medicationsPanelIncludesDosageScheduleAndAllergies() {
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val modelSource = readSource("data/EobModels.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(cardSource.contains("insuranceCardActiveMedicationsLabel"))
        assertTrue(cardSource.contains("insuranceCardDosageScheduleLabel"))
        assertTrue(cardSource.contains("insuranceCardAllergiesLabel"))
        assertTrue(modelSource.contains("medicationDosageSchedule"))
        assertTrue(modelSource.contains("medicationAllergies"))
        assertTrue(viewModelSource.contains("updateInsuranceCardDosageSchedule"))
        assertTrue(viewModelSource.contains("updateInsuranceCardAllergies"))
    }

    @Test
    fun homeAndNavHostWireInsuranceCardMetadataThroughEobViewModel() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(homeSource.contains("medicationDosageSchedule = profile.medicationDosageSchedule"))
        assertTrue(homeSource.contains("onMedicationDosageScheduleChange"))
        assertTrue(navSource.contains("updateInsuranceCardDosageSchedule"))
        assertTrue(navSource.contains("updateInsuranceCardAllergies"))
    }

    @Test
    fun appointmentSectionRemainsUnchangedOnHomeScreen() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val appointmentsSource = readSource("ui/components/home/HomeAppointmentsSection.kt")
        assertTrue(homeSource.contains("HomeAppointmentsSection"))
        assertTrue(appointmentsSource.contains("ProviderTypeChipBar"))
        assertTrue(appointmentsSource.contains("addAppointment"))
        assertTrue(appointmentsSource.contains("saveAppointment"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
