package app.eob.me

import app.eob.me.data.InsuranceCardNotesMetadata
import app.eob.me.data.UserProfile
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsuranceCardMetadataTest {
    @Test
    fun insuranceCardNotesMetadataRoundTripsThroughProfile() {
        val profile = UserProfile(
            currentPrescriptions = "Lisinopril 10mg",
            medicationDosageSchedule = "Daily at 8 AM",
            medicationAllergies = "Penicillin",
            doctorQuickNotes = "Ask about knee pain."
        )
        val metadata = InsuranceCardNotesMetadata.fromProfile(profile)
        val merged = metadata.mergeInto(UserProfile(firstName = "Jane"))
        assertEquals("Lisinopril 10mg", merged.currentPrescriptions)
        assertEquals("Daily at 8 AM", merged.medicationDosageSchedule)
        assertEquals("Penicillin", merged.medicationAllergies)
        assertEquals("Ask about knee pain.", merged.doctorQuickNotes)
        assertEquals("Jane", merged.firstName)
    }

    @Test
    fun applyInsuranceCardNotesPreservesMedicationFields() {
        val viewModel = EobViewModel()
        val profile = UserProfile(
            firstName = "Jane",
            insuranceName = "Aetna",
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
        assertEquals("Rx B", updated.currentPrescriptions)
        assertEquals("Evening", updated.medicationDosageSchedule)
        assertEquals("Sulfa", updated.medicationAllergies)
        assertEquals("Note B", updated.doctorQuickNotes)
    }

    @Test
    fun cleanInsuranceCardUsesBackModeNavigation() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("InsuranceCardBackMode"))
        assertTrue(source.contains("AnimatedContent"))
        assertTrue(source.contains("BackHandler(enabled = flipped)"))
    }

    @Test
    fun cleanInsuranceCardRefreshesLocalNotesWhenFlippedToBack() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("LaunchedEffect(flipped)"))
        assertTrue(source.contains("if (flipped)"))
        assertTrue(source.contains("localDosageSchedule = medicationDosageSchedule"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
