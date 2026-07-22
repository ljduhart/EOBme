package app.eob.me

import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.CptCategory
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.data.AppLanguage
import app.eob.me.viewmodel.EobViewModel
import app.eob.me.viewmodel.HubUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EobViewModelCareTeamTest {
    @Test
    fun hubUiStateDefaultsIncludeAllCareTeamSlots() {
        val state = HubUiState()
        assertEquals(4, state.preferredDoctors.size)
        CareTeamProviderType.displayOrder.forEach { type ->
            assertEquals(type, state.preferredDoctors[type]?.type)
        }
    }

    @Test
    fun updatePreferredDoctorStoresInUiState() {
        val viewModel = EobViewModel()
        val doctor = PreferredDoctor(
            type = CareTeamProviderType.Dentist,
            name = "Dr. Smith",
            specialty = "General dentistry",
            address = "123 Main St",
            phone = "5555555555"
        )
        viewModel.updatePreferredDoctor(doctor)
        val stored = viewModel.uiState.value.preferredDoctors[CareTeamProviderType.Dentist]
        assertEquals("(555) 555-5555", stored?.phone)
        assertEquals("Dr. Smith", stored?.name)
    }

    @Test
    fun sanitizeCareTeamPhoneStripsInvalidCharacters() {
        val viewModel = EobViewModel()
        assertEquals("(555) 555-5555", viewModel.sanitizeCareTeamPhone("abc5555555555xyz"))
    }

    @Test
    fun sanitizeCareTeamProviderNamePrefixesDrAndCapitalizes() {
        val viewModel = EobViewModel()
        assertEquals("Dr. John Smith", viewModel.sanitizeCareTeamProviderName("john smith"))
        assertEquals("Dr. Jane Doe", viewModel.sanitizeCareTeamProviderName("dr. jane doe"))
        assertEquals("Dr. Lee", viewModel.sanitizeCareTeamProviderName("DR LEE"))
        assertEquals("", viewModel.sanitizeCareTeamProviderName("   "))
        assertEquals("", viewModel.sanitizeCareTeamProviderName("Dr."))
        assertEquals("", viewModel.sanitizeCareTeamProviderName("dr"))
        assertEquals("", viewModel.sanitizeCareTeamProviderName("DR."))
        assertEquals("", viewModel.sanitizeCareTeamProviderName("Dr. "))
    }

    @Test
    fun updatePreferredDoctorDoesNotPrefixDrWhenNameIsBlank() {
        val viewModel = EobViewModel()
        viewModel.updatePreferredDoctor(
            PreferredDoctor(
                type = CareTeamProviderType.Pcp,
                name = "",
                phone = "5551234567"
            )
        )
        val stored = viewModel.uiState.value.preferredDoctors[CareTeamProviderType.Pcp]
        assertEquals("", stored?.name)
        assertEquals("(555) 123-4567", stored?.phone)
        assertFalse(stored?.isAssigned == true)
        val card = viewModel.careTeamCardStates(AppLanguage.English)
            .first { it.type == CareTeamProviderType.Pcp }
        assertEquals("Tap to add", card.primaryLine)
    }

    @Test
    fun updatePreferredDoctorClearsDrPrefixWhenOnlyTitleEntered() {
        val viewModel = EobViewModel()
        viewModel.updatePreferredDoctor(
            PreferredDoctor(
                type = CareTeamProviderType.Dentist,
                name = "Dr.",
                phone = "5551234567"
            )
        )
        val stored = viewModel.uiState.value.preferredDoctors[CareTeamProviderType.Dentist]
        assertEquals("", stored?.name)
        assertFalse(stored?.isAssigned == true)
    }

    @Test
    fun updatePreferredDoctorNormalizesNameAndPhoneForAllProviderTypes() {
        val viewModel = EobViewModel()
        CareTeamProviderType.displayOrder.forEach { type ->
            viewModel.updatePreferredDoctor(
                PreferredDoctor(
                    type = type,
                    name = "jane doe",
                    phone = "5551234567"
                )
            )
            val stored = viewModel.uiState.value.preferredDoctors[type]
            assertEquals("Dr. Jane Doe", stored?.name)
            assertEquals("(555) 123-4567", stored?.phone)
        }
    }

    @Test
    fun allCareTeamCardsStayUnassignedWithoutNameAcrossProviderTypes() {
        val viewModel = EobViewModel()
        CareTeamProviderType.displayOrder.forEach { type ->
            viewModel.updatePreferredDoctor(
                PreferredDoctor(
                    type = type,
                    name = "",
                    phone = "5551234567"
                )
            )
        }
        val cards = viewModel.careTeamCardStates(AppLanguage.English)
        CareTeamProviderType.displayOrder.forEach { type ->
            val card = cards.first { it.type == type }
            val stored = viewModel.uiState.value.preferredDoctors[type]
            assertEquals("", stored?.name)
            assertFalse(card.isAssigned)
            assertEquals("Tap to add", card.primaryLine)
            assertFalse(card.primaryLine.startsWith("Dr."))
        }
    }

    @Test
    fun clearingProviderNameOnResaveRemovesDrFromCard() {
        val viewModel = EobViewModel()
        viewModel.updatePreferredDoctor(
            PreferredDoctor(
                type = CareTeamProviderType.Therapist,
                name = "jane doe",
                phone = "5551234567"
            )
        )
        viewModel.updatePreferredDoctor(
            PreferredDoctor(
                type = CareTeamProviderType.Therapist,
                name = "",
                phone = "5551234567"
            )
        )
        val stored = viewModel.uiState.value.preferredDoctors[CareTeamProviderType.Therapist]
        val card = viewModel.careTeamCardStates(AppLanguage.English)
            .first { it.type == CareTeamProviderType.Therapist }
        assertEquals("", stored?.name)
        assertEquals("Tap to add", card.primaryLine)
        assertEquals("(555) 123-4567", stored?.phone)
    }

    @Test
    fun careTeamPhoneDialogUsesDigitsWithVisualTransformation() {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/ui/components/home/HomeCareTeamCards.kt"),
            java.io.File("app/src/main/java/app/eob/me/ui/components/home/HomeCareTeamCards.kt")
        )
        val source = candidates.first { it.isFile }.readText()
        assertTrue(source.contains("value = phoneDigits"))
        assertTrue(source.contains("careTeamPhoneVisualTransformation"))
    }

    @Test
    fun careTeamCardStatesShowSanitizedNameAndPhoneAfterSave() {
        val viewModel = EobViewModel()
        viewModel.updatePreferredDoctor(
            PreferredDoctor(
                type = CareTeamProviderType.Pcp,
                name = "john smith",
                phone = "5551234567"
            )
        )
        viewModel.updatePreferredDoctor(
            PreferredDoctor(
                type = CareTeamProviderType.Specialist,
                name = "jane doe",
                specialty = "Cardiology",
                phone = "5559876543"
            )
        )
        val cards = viewModel.careTeamCardStates(AppLanguage.English)
        val pcp = cards.first { it.type == CareTeamProviderType.Pcp }
        val specialist = cards.first { it.type == CareTeamProviderType.Specialist }
        assertEquals("Dr. John Smith", pcp.primaryLine)
        assertEquals("(555) 123-4567", pcp.secondaryLine)
        assertEquals("tel:5551234567", pcp.phoneDialUri)
        assertEquals("Dr. Jane Doe", specialist.primaryLine)
        assertEquals("tel:5559876543", specialist.phoneDialUri)
    }

    @Test
    fun careTeamShimmerSuppressedWhenAnyCardCompleteWithPhone() {
        val viewModel = EobViewModel()
        assertFalse(viewModel.careTeamShimmerSuppressed(AppLanguage.English))
        viewModel.updatePreferredDoctor(
            PreferredDoctor(
                type = CareTeamProviderType.Pcp,
                name = "john smith",
                phone = "5551234567"
            )
        )
        assertTrue(viewModel.careTeamShimmerSuppressed(AppLanguage.English))
        val cards = viewModel.careTeamCardStates(AppLanguage.English)
        val pcp = cards.first { it.type == CareTeamProviderType.Pcp }
        assertTrue(pcp.isCompleteWithPhone)
    }

    @Test
    fun addAppointmentStoresProviderType() {
        val viewModel = EobViewModel()
        viewModel.addAppointment(
            date = "01/15/2026",
            provider = "Dr. Lee",
            time = "10:00",
            notes = "Checkup",
            providerType = CareTeamProviderType.Specialist
        )
        val appointment = viewModel.uiState.value.appointments.single()
        assertEquals(CareTeamProviderType.Specialist, appointment.providerType)
        assertTrue(appointment.providerName.isNotBlank())
    }

    @Test
    fun setLoadingInvoiceSetsProcessingPhase() {
        val viewModel = EobViewModel()
        viewModel.setLoadingInvoice(true)
        assertEquals(InvoiceProcessingPhase.Processing, viewModel.uiState.value.invoiceProcessingPhase)
        assertTrue(viewModel.uiState.value.isLoadingInvoice)
    }

    @Test
    fun setLoadingInvoiceFalseClearsProcessingPhase() {
        val viewModel = EobViewModel()
        viewModel.setLoadingInvoice(true)
        viewModel.setLoadingInvoice(false)
        assertEquals(InvoiceProcessingPhase.Idle, viewModel.uiState.value.invoiceProcessingPhase)
        assertFalse(viewModel.uiState.value.isLoadingInvoice)
    }

    @Test
    fun setSelectedCptCategoryUpdatesHubUiState() {
        val viewModel = EobViewModel()
        viewModel.setSelectedCptCategory(CptCategory.Lab)
        assertEquals(CptCategory.Lab, viewModel.uiState.value.selectedCptCategory)
    }

    @Test
    fun resetHubStateClearsAppointmentsAndRestoresCareTeamDefaults() {
        val viewModel = EobViewModel()
        viewModel.updatePreferredDoctor(
            PreferredDoctor(
                type = CareTeamProviderType.Pcp,
                name = "Dr. Jones"
            )
        )
        viewModel.addAppointment("01/01/2026", "Dr. Jones", "9:00", "", CareTeamProviderType.Pcp)
        viewModel.resetHubState()
        assertTrue(viewModel.uiState.value.appointments.isEmpty())
        assertEquals("", viewModel.uiState.value.preferredDoctors[CareTeamProviderType.Pcp]?.name)
    }
}
