package app.eob.me

import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.viewmodel.EobViewModel
import app.eob.me.viewmodel.HubUiState
import org.junit.Assert.assertEquals
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
            phone = "555-0100"
        )
        viewModel.updatePreferredDoctor(doctor)
        val stored = viewModel.uiState.value.preferredDoctors[CareTeamProviderType.Dentist]
        assertEquals(doctor, stored)
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
