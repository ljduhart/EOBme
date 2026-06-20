package app.eob.me.data

enum class AppealTarget {
    INSURANCE,
    DOCTOR;

    fun labelKey(): String = when (this) {
        INSURANCE -> "appealTargetInsurance"
        DOCTOR -> "appealTargetDoctor"
    }
}

enum class DoctorDisputeStrategy {
    ITEMIZED_AUDIT,
    UNAPPLIED_COPAY,
    FINANCIAL_HARDSHIP;

    fun labelKey(): String = when (this) {
        ITEMIZED_AUDIT -> "appealStrategyItemizedAudit"
        UNAPPLIED_COPAY -> "appealStrategyUnappliedCopay"
        FINANCIAL_HARDSHIP -> "appealStrategyFinancialHardship"
    }
}
