package app.eob.me

import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.CptCategory
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.InvoiceProcessingPhase
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
