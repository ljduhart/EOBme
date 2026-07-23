package app.eob.me.data

sealed interface ClaimStatus {
    data object AuditedCorrect : ClaimStatus
    data class PotentialError(val message: String) : ClaimStatus
    data object Appealed : ClaimStatus
}

data class MedicalClaim(
    val id: String,
    val claimNumber: String,
    val dateOfService: String,
    val totalBilled: Double,
    val carrierCovered: Double,
    val status: ClaimStatus,
    val sourceName: String,
    val storageDownloadUrl: String
)

data class FacilitySpending(
    val id: String,
    val providerName: String,
    val totalSpent: Double,
    val outOfPocketShare: Double,
    val carrierShare: Double,
    val claims: List<MedicalClaim>,
    val isExpanded: Boolean = false
)

data class ExpenseAnalyticsAllocation(
    val networkSavings: Double,
    val carrierCovered: Double,
    val patientResponsibility: Double
) {
    val totalBilled: Double
        get() = networkSavings + carrierCovered + patientResponsibility

    fun networkSavingsFraction(): Float =
        fraction(networkSavings)

    fun carrierCoveredFraction(): Float =
        fraction(carrierCovered)

    fun patientResponsibilityFraction(): Float =
        fraction(patientResponsibility)

    private fun fraction(amount: Double): Float {
        val total = totalBilled
        if (total <= 0.0) return 0f
        return (amount / total).toFloat().coerceIn(0f, 1f)
    }
}

enum class ExpenseAnalyticsSort {
    HighestPatientShare,
    NewestActivity,
    HighestBilledTotal,
    FacilityAlphabetical
}

data class ExpenseAnalyticsState(
    val isLoading: Boolean = false,
    val allocation: ExpenseAnalyticsAllocation? = null,
    val totalPatientOutOfPocket: Double = 0.0,
    val totalCarrierContribution: Double = 0.0,
    val totalNetworkSavings: Double = 0.0,
    val totalBilled: Double = 0.0,
    val facilities: List<FacilitySpending> = emptyList(),
    val selectedSort: ExpenseAnalyticsSort = ExpenseAnalyticsSort.HighestPatientShare
)
