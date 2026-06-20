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
            strategy = DoctorDisputeStrategy.ITEMIZED_AUDIT
        )
        assertTrue(letter.contains("Re: Appeal of EOB determination"))
        assertTrue(letter.contains("Aetna"))
        assertFalse(letter.contains("itemized billing statement"))
    }

    @Test
    fun doctorItemizedAuditInjectsTransparencyParagraphWithServiceDate() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.ITEMIZED_AUDIT
        )
        assertTrue(letter.contains("City Medical Group"))
        assertTrue(letter.contains("itemized billing statement"))
        assertTrue(letter.contains("03/15/2026"))
    }

    @Test
    fun doctorUnappliedCopayInjectsCopayCreditParagraph() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.UNAPPLIED_COPAY
        )
        assertTrue(letter.contains("copayment/coinsurance"))
        assertTrue(letter.contains("35"))
    }

    @Test
    fun doctorFinancialHardshipInjectsHardshipParagraph() {
        val letter = AppealLetterGenerator.generate(
            profile = profile,
            eob = sampleRecord(),
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.FINANCIAL_HARDSHIP
        )
        assertTrue(letter.contains("financial hardship adjustment"))
        assertTrue(letter.contains("Charity Care"))
    }
}
