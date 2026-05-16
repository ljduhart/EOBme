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
            password.isNotBlank()

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
    val rawText: String
) {
    val totalBilledAmount: Double = charges.sumOf { it.billedAmount }
    val totalInsurancePaidAmount: Double = charges.sumOf { it.insurancePaidAmount }
    val totalContractualAdjustmentAmount: Double = charges.sumOf { it.contractualAdjustmentAmount }
    val totalCopayAmount: Double = charges.sumOf { it.copayAmount }
    val totalDeductibleAmount: Double = charges.sumOf { it.deductibleAmount }
    val totalCoinsuranceAmount: Double = charges.sumOf { it.coinsuranceAmount }
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
