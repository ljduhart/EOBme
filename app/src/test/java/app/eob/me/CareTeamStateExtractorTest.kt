package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.CareTeamStateExtractor
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.NetworkAssuranceState
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.TherapistNetworkStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareTeamStateExtractorTest {
    @Test
    fun unassignedPcpShowsTapToEdit() {
        val cards = CareTeamStateExtractor.buildCareTeamCards(
            language = AppLanguage.English,
            preferredDoctors = emptyMap(),
            appointments = emptyList(),
            records = emptyList(),
            invoiceProcessing = false
        )
        val pcp = cards.first { it.type == CareTeamProviderType.Pcp }
        assertTrue(!pcp.isAssigned)
        assertEquals(NetworkAssuranceState.VerificationPending, pcp.assuranceState)
    }

    @Test
    fun pcpWithPhoneExposesTapToCall() {
        val doctor = PreferredDoctor(
            type = CareTeamProviderType.Pcp,
            name = "Dr. Smith",
            phone = "555-0100"
        )
        val cards = CareTeamStateExtractor.buildCareTeamCards(
            language = AppLanguage.English,
            preferredDoctors = mapOf(CareTeamProviderType.Pcp to doctor),
            appointments = emptyList(),
            records = emptyList(),
            invoiceProcessing = false
        )
        val pcp = cards.first { it.type == CareTeamProviderType.Pcp }
        assertEquals("Dr. Smith", pcp.primaryLine)
        assertTrue(pcp.phoneDialUri?.startsWith("tel:") == true)
    }

    @Test
    fun specialistShowsReferralAndSpecialty() {
        val doctor = PreferredDoctor(
            type = CareTeamProviderType.Specialist,
            name = "Dr. Lee",
            specialty = "Cardiologist"
        )
        val cards = CareTeamStateExtractor.buildCareTeamCards(
            language = AppLanguage.English,
            preferredDoctors = mapOf(CareTeamProviderType.Specialist to doctor),
            appointments = emptyList(),
            records = emptyList(),
            invoiceProcessing = false
        )
        val specialist = cards.first { it.type == CareTeamProviderType.Specialist }
        assertTrue(specialist.specialistReferralActive)
        assertEquals("Cardiologist", specialist.tertiaryLine)
    }

    @Test
    fun therapistCopayFromLatestEob() {
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Mind Wellness
                Aetna
                01/10/2026
                90834 therapy copay ${'$'}35.00 billed ${'$'}120.00
            """.trimIndent(),
            sourceName = "therapy",
            nextId = 1
        )
        val doctor = PreferredDoctor(
            type = CareTeamProviderType.Therapist,
            name = "Mind Wellness"
        )
        val cards = CareTeamStateExtractor.buildCareTeamCards(
            language = AppLanguage.English,
            preferredDoctors = mapOf(CareTeamProviderType.Therapist to doctor),
            appointments = emptyList(),
            records = listOf(record),
            invoiceProcessing = false
        )
        val therapist = cards.first { it.type == CareTeamProviderType.Therapist }
        assertTrue((therapist.therapistCopayAmount ?: 0.0) > 0.0)
        assertTrue(therapist.secondaryLine.contains("35"))
    }

    @Test
    fun therapistOutOfNetworkFromLatestEob() {
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Mind Wellness
                Aetna
                01/10/2026
                Out of network therapist visit billed ${'$'}200.00
            """.trimIndent(),
            sourceName = "oon",
            nextId = 1
        )
        val doctor = PreferredDoctor(
            type = CareTeamProviderType.Therapist,
            name = "Mind Wellness"
        )
        val cards = CareTeamStateExtractor.buildCareTeamCards(
            language = AppLanguage.English,
            preferredDoctors = mapOf(CareTeamProviderType.Therapist to doctor),
            appointments = emptyList(),
            records = listOf(record),
            invoiceProcessing = false
        )
        val therapist = cards.first { it.type == CareTeamProviderType.Therapist }
        assertEquals(TherapistNetworkStatus.OutOfNetwork, therapist.therapistNetworkStatus)
        assertEquals(NetworkAssuranceState.OutOfNetworkAlert, therapist.assuranceState)
    }

    @Test
    fun providerDirectoryShowsOutOfNetworkWhenDirectoryStale() {
        val record = EobAnalyzer.analyze(
            rawText = "Provider: Unknown Clinic\nout-of-network\n01/01/2026",
            sourceName = "x",
            nextId = 1
        )
        val assurance = CareTeamStateExtractor.buildProviderDirectoryAssurance(
            language = AppLanguage.English,
            preferredDoctors = mapOf(
                CareTeamProviderType.Pcp to PreferredDoctor(type = CareTeamProviderType.Pcp, name = "Dr. Other")
            ),
            records = listOf(record),
            invoiceProcessing = false
        )
        assertEquals(NetworkAssuranceState.OutOfNetworkAlert, assurance.state)
        assertTrue(assurance.showWarningDot)
    }
}
