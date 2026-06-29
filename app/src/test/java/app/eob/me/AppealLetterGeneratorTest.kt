package app.eob.me

import app.eob.me.data.AppealLetterGenerator
import app.eob.me.data.AppealTarget
import app.eob.me.data.DoctorDisputeStrategy
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppealLetterGeneratorTest {
    private val profile = UserProfile(
        firstName = "Jane",
        lastName = "Doe",
        email = "jane@example.com",
        insuranceId = "MEM123"
    )

    private fun sampleRecord() = EobAnalyzer.analyze(
        rawText = """
            Provider: City Medical Group
            Aetna
            03/15/2026
            99213 office visit copay ${'$'}35.00 billed ${'$'}180.00
        """.trimIndent(),
        sourceName = "appeal",
        nextId = 1
    )

    @Test
    fun insuranceTargetUsesStandardInsurerAppealCopy() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.INSURANCE,
            strategy = DoctorDisputeStrategy.IMPROPER_BALANCE_BILLING
        )
        assertTrue(letter.contains("Re: Appeal of EOB determination"))
        assertTrue(letter.contains("Aetna"))
        assertFalse(letter.contains("Billing Department"))
    }

    @Test
    fun doctorTemplateUsesProviderBillingDepartmentHeader() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.IMPROPER_BALANCE_BILLING
        )
        assertTrue(letter.contains("City Medical Group - Billing Department"))
        assertTrue(letter.contains("Patient Name: Jane Doe"))
        assertTrue(letter.contains("Date of Service: 03/15/2026"))
        assertTrue(letter.contains("Disputed Amount:"))
        assertTrue(letter.contains("[Your Signature]"))
    }

    @Test
    fun doctorImproperBalanceBillingInjectsNetworkDiscountReason() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.IMPROPER_BALANCE_BILLING
        )
        assertTrue(letter.contains("contractually obligated to write off the network discount"))
    }

    @Test
    fun doctorCodingUpcodingInjectsCodingErrorReason() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.CODING_UPCODING_ERROR
        )
        assertTrue(letter.contains("services billed do not accurately reflect the level of care"))
        assertTrue(letter.contains("standard routine check-up"))
    }

    @Test
    fun doctorPriorAuthorizationInjectsAuthorizationReason() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.PRIOR_AUTHORIZATION_FAILURE
        )
        assertTrue(letter.contains("lack of prior authorization"))
        assertTrue(letter.contains("administrative oversight"))
    }

    @Test
    fun doctorNoSurprisesActInjectsFederalProtectionReason() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.NO_SURPRISES_ACT
        )
        assertTrue(letter.contains("No Surprises Act"))
        assertTrue(letter.contains("out-of-network provider"))
    }
}
