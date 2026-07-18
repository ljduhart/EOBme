package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiPr172Test {
    @Test
    fun dashboardUsesPieChartAndFacilityBarGraph() {
        val source = readSource("ui/screens/DashboardScreen.kt")
        assertTrue(source.contains("ClaimAllocationPieChart"))
        assertTrue(source.contains("drawArc"))
        assertTrue(source.contains("useCenter = true"))
        assertTrue(source.contains("FacilitySpendingBarChart"))
        assertTrue(source.contains("facilitySpendingTotal"))
        assertTrue(source.contains("asCurrency()"))
        assertFalse(source.contains("LinearProgressIndicator"))
    }

    @Test
    fun providerTypeChipsShowLabelAndDotOnly() {
        val source = readSource("ui/components/home/CareTeamUi.kt")
        assertTrue(source.contains("displayOrder.chunked(2)"))
        assertTrue(source.contains(".size(10.dp)"))
        assertTrue(source.contains("careTeamLabel(language, type)"))
        assertFalse(source.contains("providerTypeDetailLine"))
        assertFalse(source.contains("careTeamUnassignedHint"))
        assertFalse(source.contains("preferredDoctors"))
    }

    @Test
    fun profileValidationRunsOnlyOnSave() {
        val profileSource = readSource("ui/screens/ProfileScreen.kt")
        val validationSource = readSource("data/ProfileFormValidation.kt")
        assertTrue(profileSource.contains("ProfileFormValidator.validate"))
        assertTrue(profileSource.contains("showValidationErrors"))
        assertFalse(profileSource.contains("canSave"))
        assertFalse(profileSource.contains("isEditing && draftCredentials.password.isNotBlank()"))
        assertTrue(validationSource.contains("ProfileFieldErrors"))
    }

    @Test
    fun profileFieldsExposeSaveTriggeredErrors() {
        val source = readSource("ui/screens/RegistrationScreen.kt")
        assertTrue(source.contains("fieldErrors: ProfileFieldErrors"))
        assertTrue(source.contains("showFieldErrors: Boolean"))
        assertTrue(source.contains("fieldErrorText"))
    }

    @Test
    fun profileSaveTrimsValidatedTextFields() {
        val source = readSource("ui/screens/ProfileScreen.kt")
        assertTrue(source.contains("firstName = mergedProfile.firstName.trim()"))
        assertTrue(source.contains("email = mergedProfile.email.trim()"))
        assertTrue(source.contains("state = mergedProfile.state.trim()"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
