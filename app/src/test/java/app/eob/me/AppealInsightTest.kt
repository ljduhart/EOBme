package app.eob.me

import app.eob.me.data.AppealTarget
import app.eob.me.data.AppLanguage
import app.eob.me.data.DoctorDisputeStrategy
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobRecord
import app.eob.me.data.InsuranceAppealStrategy
import app.eob.me.ui.screens.resolveAppealInsight
import org.junit.Assert.assertTrue
import org.junit.Test

class AppealInsightTest {
    @Test
    fun insuranceInsightReflectsProcessedIncorrectlyStrategy() {
        val record = recordWithTotals(
            billed = 500.0,
            insurancePaid = 0.0,
            copay = 25.0
        )

        val insight = resolveAppealInsight(
            language = AppLanguage.English,
            record = record,
            target = AppealTarget.INSURANCE,
            strategy = DoctorDisputeStrategy.IMPROPER_BALANCE_BILLING,
            insuranceStrategy = InsuranceAppealStrategy.PROCESSED_INCORRECTLY
        )

        assertTrue(insight.contains("administrative calculation errors"))
    }

    @Test
    fun insuranceInsightReflectsDeniedIncorrectlyStrategy() {
        val record = recordWithTotals(
            billed = 200.0,
            insurancePaid = 40.0,
            copay = 50.0,
            deductible = 30.0
        )

        val insight = resolveAppealInsight(
            language = AppLanguage.English,
            record = record,
            target = AppealTarget.INSURANCE,
            strategy = DoctorDisputeStrategy.IMPROPER_BALANCE_BILLING,
            insuranceStrategy = InsuranceAppealStrategy.DENIED_INCORRECTLY
        )

        assertTrue(insight.contains("incorrect denial"))
    }

    @Test
    fun doctorInsightReflectsSelectedStrategy() {
        val record = recordWithTotals(billed = 120.0, insurancePaid = 80.0, copay = 10.0)

        val noSurprisesInsight = resolveAppealInsight(
            language = AppLanguage.English,
            record = record,
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.NO_SURPRISES_ACT,
            insuranceStrategy = InsuranceAppealStrategy.PROCESSED_INCORRECTLY
        )
        val balanceBillingInsight = resolveAppealInsight(
            language = AppLanguage.English,
            record = record,
            target = AppealTarget.DOCTOR,
            strategy = DoctorDisputeStrategy.IMPROPER_BALANCE_BILLING,
            insuranceStrategy = InsuranceAppealStrategy.PROCESSED_INCORRECTLY
        )

        assertTrue(noSurprisesInsight.contains("No Surprises Act"))
        assertTrue(balanceBillingInsight.contains("balance billing"))
    }

    private fun recordWithTotals(
        billed: Double,
        insurancePaid: Double,
        copay: Double,
        deductible: Double = 0.0,
        contractualAdj: Double = 0.0
    ): EobRecord {
        return EobAnalyzer.analyze(
            rawText = """
                Provider: Audit Clinic
                Aetna
                01/15/2026
                99213 billed ${'$'}$billed insurance paid ${'$'}$insurancePaid contractual adjustment ${'$'}$contractualAdj
                copay ${'$'}$copay deductible ${'$'}$deductible
            """.trimIndent(),
            sourceName = "test",
            nextId = 1
        )
    }
}
