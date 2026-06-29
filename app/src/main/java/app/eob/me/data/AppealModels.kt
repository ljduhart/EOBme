package app.eob.me.data

enum class AppealTarget {
    INSURANCE,
    DOCTOR;

    fun labelKey(): String = when (this) {
        INSURANCE -> "appealTargetInsurance"
        DOCTOR -> "appealTargetDoctor"
    }
}

enum class InsuranceAppealStrategy {
    PROCESSED_INCORRECTLY,
    DENIED_INCORRECTLY,
    PATIENT_RESPONSIBILITY_INCORRECT;

    fun labelKey(): String = when (this) {
        PROCESSED_INCORRECTLY -> "appealStrategyInsuranceProcessedIncorrectly"
        DENIED_INCORRECTLY -> "appealStrategyInsuranceDeniedIncorrectly"
        PATIENT_RESPONSIBILITY_INCORRECT -> "appealStrategyInsurancePatientResponsibilityIncorrect"
    }

    fun insightKey(): String = when (this) {
        PROCESSED_INCORRECTLY -> "appealInsightInsuranceProcessedIncorrectly"
        DENIED_INCORRECTLY -> "appealInsightInsuranceDeniedIncorrectly"
        PATIENT_RESPONSIBILITY_INCORRECT -> "appealInsightInsurancePatientResponsibilityIncorrect"
    }
}

enum class DoctorDisputeStrategy {
    IMPROPER_BALANCE_BILLING,
    CODING_UPCODING_ERROR,
    PRIOR_AUTHORIZATION_FAILURE,
    NO_SURPRISES_ACT;

    fun labelKey(): String = when (this) {
        IMPROPER_BALANCE_BILLING -> "appealStrategyImproperBalanceBilling"
        CODING_UPCODING_ERROR -> "appealStrategyCodingUpcodingError"
        PRIOR_AUTHORIZATION_FAILURE -> "appealStrategyPriorAuthorizationFailure"
        NO_SURPRISES_ACT -> "appealStrategyNoSurprisesAct"
    }

    fun insightKey(): String = when (this) {
        IMPROPER_BALANCE_BILLING -> "appealInsightDoctorBalanceBilling"
        CODING_UPCODING_ERROR -> "appealInsightDoctorCodingUpcoding"
        PRIOR_AUTHORIZATION_FAILURE -> "appealInsightDoctorPriorAuth"
        NO_SURPRISES_ACT -> "appealInsightDoctorNoSurprisesAct"
    }
}
