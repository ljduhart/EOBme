package app.eob.me.data

/**
 * Normalized insurance EOB domain models. These types are free of Veryfi's flat indexed schema
 * (`cpt_code_1`, `amount_billed_2`, …) and are safe for repositories and UI consumption.
 */

/**
 * A single procedure / service line on an insurance EOB after Veryfi payload normalization.
 *
 * @property lineIndex 1-based index from the source Veryfi row (preserved for traceability).
 * @property procedureCode CPT, HCPCS, or CDT code (trimmed, uppercased).
 * @property description Human-readable service description when present.
 * @property serviceDateIso ISO-8601 calendar date (`yyyy-MM-dd`) for the line.
 */
data class ServiceLine(
    val lineIndex: Int,
    val procedureCode: String,
    val description: String,
    val serviceDateIso: String,
    val billedAmount: Double,
    val allowedAmount: Double,
    val insurancePaidAmount: Double,
    val contractualAdjustmentAmount: Double,
    val copayAmount: Double,
    val deductibleAmount: Double,
    val coinsuranceAmount: Double,
    val patientResponsibilityAmount: Double
)

/**
 * Financial roll-up for a single claim when Veryfi provides [claim_totals].
 */
data class InsuranceClaimTotals(
    val totalBilled: Double,
    val totalInsurancePaid: Double,
    val totalContractualAdjustment: Double,
    val totalPatientResponsibility: Double,
    val totalCopay: Double,
    val totalDeductible: Double,
    val totalCoinsurance: Double
)

/**
 * One claim block under the payer/group header with normalized [serviceLines].
 */
data class InsuranceClaim(
    val claimIndex: Int,
    val providerName: String,
    val claimNumber: String,
    val processedDateIso: String,
    val serviceLines: List<ServiceLine>,
    val claimTotals: InsuranceClaimTotals?
)

/**
 * Fully normalized nested insurance EOB document produced by [app.eob.me.network.toNormalizedInsuranceEob].
 */
data class NormalizedInsuranceEob(
    val groupName: String,
    val payerName: String,
    val benefitType: String,
    val groupNumber: String,
    val subscriberId: String,
    val subscriberName: String,
    val benefitYearMaxRemaining: Double,
    val orthodontiaMaxRemaining: Double,
    val claims: List<InsuranceClaim>
) {
    val allServiceLines: List<ServiceLine>
        get() = claims.flatMap { it.serviceLines }
}

/**
 * Mapping output that preserves non-fatal parse warnings while returning normalized domain data.
 */
data class NormalizedInsuranceEobResult(
    val document: NormalizedInsuranceEob,
    val warnings: List<String> = emptyList()
)
