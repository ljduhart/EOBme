package app.eob.me

import app.eob.me.data.AppealLetterGenerator
import app.eob.me.data.AppealTarget
import app.eob.me.data.DoctorDisputeStrategy
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.InsuranceAppealStrategy
import app.eob.me.data.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppealLetterGeneratorTest {
    private val profile = UserProfile(
        firstName = "Jane",
        lastName = "Doe",
        email = "jane@example.com",
        insuranceId = "MEM123",
        groupName = "GRP456",
        city = "Austin",
        state = "TX"
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
    fun insuranceTargetPrefersProfileInsuranceNameOverEobCarrier() {
        val profileWithCarrier = profile.copy(insuranceName = "UnitedHealthcare")
        val record = sampleRecord()
        val letter = AppealLetterGenerator.generate(
            profile = profileWithCarrier,
            eob = record,
            target = AppealTarget.INSURANCE,
            insuranceStrategy = InsuranceAppealStrategy.PROCESSED_INCORRECTLY
        )
        assertTrue(letter.contains("To: UnitedHealthcare – Member Appeals Department"))
        assertFalse(letter.contains("To: Aetna"))
    }

    @Test
    fun doctorTargetIncludesProfileInsuranceCarrier() {
        val profileWithCarrier = profile.copy(insuranceName = "UnitedHealthcare")
        val letter = AppealLetterGenerator.generate(
            profile = profileWithCarrier,
            eob = sampleRecord(),
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.IMPROPER_BALANCE_BILLING
        )
        assertTrue(letter.contains("Insurance Carrier: UnitedHealthcare"))
    }

    @Test
    fun insuranceTargetFallsBackToProfileInsuranceName() {
        val profileWithCarrier = profile.copy(
            insuranceName = "UnitedHealthcare",
            firstName = "Jane",
            lastName = "Doe"
        )
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: City Medical Group

                03/15/2026
                99213 office visit copay ${'$'}35.00 billed ${'$'}180.00
            """.trimIndent(),
            sourceName = "appeal",
            nextId = 2
        )
        val letter = AppealLetterGenerator.generate(
            profile = profileWithCarrier,
            eob = record,
            target = AppealTarget.INSURANCE,
            insuranceStrategy = InsuranceAppealStrategy.PROCESSED_INCORRECTLY
        )
        assertTrue(letter.contains("To: UnitedHealthcare – Member Appeals Department"))
        assertFalse(letter.contains("Insurance not recognized"))
    }

    @Test
    fun insuranceTargetUsesFormalMemberAppealsTemplate() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.INSURANCE,
            insuranceStrategy = InsuranceAppealStrategy.PROCESSED_INCORRECTLY
        )
        assertTrue(letter.contains("Member Appeals Department"))
        assertTrue(letter.contains("Re: Formal Claim Appeal for Member: Jane Doe"))
        assertTrue(letter.contains("Member ID: MEM123"))
        assertTrue(letter.contains("Group Number: GRP456"))
        assertTrue(letter.contains("Claim Number: [Claim Number listed on EOB]"))
        assertTrue(letter.contains("Provider Name: City Medical Group"))
        assertTrue(letter.contains("30-day review window"))
        assertTrue(letter.contains("[Your Signature]"))
        assertTrue(letter.contains("Austin, TX"))
        assertFalse(letter.contains("Billing Department"))
    }

    @Test
    fun insuranceProcessedIncorrectlyInjectsSystemGlitchReason() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.INSURANCE,
            insuranceStrategy = InsuranceAppealStrategy.PROCESSED_INCORRECTLY
        )
        assertTrue(letter.contains("your system appears to have processed this claim incorrectly"))
        assertTrue(letter.contains("contracted fee schedule"))
    }

    @Test
    fun insuranceDeniedIncorrectlyInjectsMedicalNecessityReason() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.INSURANCE,
            insuranceStrategy = InsuranceAppealStrategy.DENIED_INCORRECTLY
        )
        assertTrue(letter.contains("this claim was denied incorrectly"))
        assertTrue(letter.contains("medically necessary"))
    }

    @Test
    fun insurancePatientResponsibilityIncorrectInjectsDeductibleMathReason() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.INSURANCE,
            insuranceStrategy = InsuranceAppealStrategy.PATIENT_RESPONSIBILITY_INCORRECT
        )
        assertTrue(letter.contains("my patient responsibility was assigned incorrectly"))
        assertTrue(letter.contains("deductible / out-of-pocket maximum"))
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
