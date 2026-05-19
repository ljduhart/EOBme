package app.eob.me.data

enum class AppLanguage(val displayName: String) {
    English("English"),
    Spanish("Español"),
    French("Français"),
    Chinese("中文")
}

enum class CptCategory(val displayName: String) {
    OfficeVisit("OVs"),
    Lab("Labs"),
    Hospital("Hospital"),
    Dme("DME"),
    Injection("Injections"),
    Other("Other")
}

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val city: String = "",
    val state: String = "",
    val subscriberId: String = "",
    val insuranceCardSummary: String = "",
    val insuranceCardDownloadUrl: String = ""
) {
    val isComplete: Boolean
        get() = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            isPasswordValid &&
            city.isNotBlank() &&
            state.isNotBlank()

    val isPasswordValid: Boolean
        get() = password.length >= 8 && password.any { it.isDigit() }

    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

data class CptInfo(
    val code: String,
    val description: String,
    val category: CptCategory
)

data class EobCharge(
    val cptCode: String,
    val cptDescription: String,
    val category: CptCategory,
    val billedAmount: Double,
    val insurancePaidAmount: Double,
    val contractualAdjustmentAmount: Double,
    val copayAmount: Double,
    val deductibleAmount: Double,
    val coinsuranceAmount: Double,
    val serviceDate: String
)

data class EobRecord(
    val id: Int,
    val sourceName: String,
    val providerName: String,
    val insuranceName: String,
    val serviceDate: String,
    val serviceDateSortKey: Int,
    val charges: List<EobCharge>,
    val duplicateChargeWarnings: List<String>,
    val rawText: String,
    val totalBilledAmount: Double = 0.0,
    val totalInsurancePaidAmount: Double = 0.0,
    val totalContractualAdjustmentAmount: Double = 0.0,
    val totalCopayAmount: Double = 0.0,
    val totalDeductibleAmount: Double = 0.0,
    val totalCoinsuranceAmount: Double = 0.0
) {
    val totalPatientResponsibility: Double
        get() = totalCopayAmount + totalDeductibleAmount + totalCoinsuranceAmount
}

data class NewsRelease(
    val company: String,
    val headline: String,
    val summary: String,
    val date: String
)

data class CptUsage(
    val info: CptInfo,
    val year: Int,
    val count: Int
)

data class DoctorAppointment(
    val id: Int,
    val date: String,
    val providerName: String,
    val notes: String
)

data class EobFieldConfidence(
    val fieldName: String,
    val value: String,
    val confidencePercent: Int,
    val needsReview: Boolean
)

data class EobMathValidation(
    val expectedPatientResponsibility: Double,
    val extractedPatientResponsibility: Double,
    val difference: Double,
    val isBalanced: Boolean
)

data class EobAccuracyReview(
    val overallConfidencePercent: Int,
    val fields: List<EobFieldConfidence>,
    val mathValidation: EobMathValidation,
    val warnings: List<String>
)

data class YearlyHealthCostSummary(
    val year: Int,
    val eobCount: Int,
    val totalBilled: Double,
    val totalInsurancePaid: Double,
    val totalContractualAdjustment: Double,
    val totalCopay: Double,
    val totalDeductible: Double,
    val totalCoinsurance: Double
) {
    val totalPatientResponsibility: Double
        get() = totalCopay + totalDeductible + totalCoinsurance
}

enum class BillingIssueType {
    DuplicateCharge,
    MathMismatch,
    MissingInsurancePayment,
    HighPatientResponsibility,
    MissingProvider,
    MissingInsurance,
    MissingDateOfService,
    MissingCptCode,
    PossibleDenial
}

enum class BillingIssueSeverity {
    Info,
    Warning,
    Critical
}

data class BillingIssue(
    val type: BillingIssueType,
    val severity: BillingIssueSeverity,
    val title: String,
    val explanation: String,
    val recommendedAction: String
)

data class ProviderSummary(
    val providerName: String,
    val eobCount: Int,
    val totalBilled: Double,
    val totalInsurancePaid: Double,
    val totalPatientResponsibility: Double,
    val lastServiceDate: String
)
