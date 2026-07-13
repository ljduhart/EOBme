package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiPr171Test {
    @Test
    fun providerTypeChipBarUsesTwoByTwoGridWithDetailLines() {
        val source = readSource("ui/components/home/CareTeamUi.kt")
        assertTrue(source.contains("displayOrder.chunked(2)"))
        assertTrue(source.contains("ProviderTypeChipCell"))
        assertTrue(source.contains("providerTypeDetailLine"))
        assertTrue(source.contains("preferredDoctors"))
        assertTrue(source.contains(".size(10.dp)"))
        assertFalse(source.contains("CareTeamProviderType.displayOrder.forEach { type ->"))
    }

    @Test
    fun addAppointmentDialogPlacesProviderGridAfterNotes() {
        val source = readSource("ui/components/home/HomeAppointmentsSection.kt")
        val notesIndex = source.indexOf("appointmentNotes")
        val chipIndex = source.indexOf("ProviderTypeChipBar", notesIndex)
        val saveIndex = source.indexOf("saveAppointment")
        assertTrue(notesIndex >= 0)
        assertTrue(chipIndex > notesIndex)
        assertTrue(saveIndex > chipIndex)
        assertTrue(source.contains("preferredDoctors = preferredDoctors"))
    }

    @Test
    fun appointmentCalendarStillDelegatesThroughEobViewModel() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("eobViewModel.addAppointment"))
        assertTrue(navSource.contains("eobViewModel.updateAppointment"))
        assertTrue(navSource.contains("preferredDoctors = uiState.preferredDoctors"))
        assertTrue(viewModelSource.contains("fun addAppointment"))
        assertTrue(viewModelSource.contains("providerType: CareTeamProviderType"))
    }

    @Test
    fun cptTrackerIncludesXRayCategoryTabWithChromeSilver() {
        val modelsSource = readSource("data/EobModels.kt")
        val screenSource = readSource("ui/screens/CptTrackerScreen.kt")
        val stringsSource = readSource("data/EobStrings.kt")
        assertTrue(modelsSource.contains("XRay(\"X-Ray\")"))
        assertTrue(screenSource.contains("CptXRayChromeSilver"))
        assertTrue(screenSource.contains("CptCategory.XRay"))
        assertTrue(stringsSource.contains("categoryXRay"))
        assertTrue(stringsSource.contains("CptCategory.XRay"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
