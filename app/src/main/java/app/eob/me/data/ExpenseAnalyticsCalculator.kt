package app.eob.me.data

import java.util.Locale

object ExpenseAnalyticsCalculator {
    fun buildState(
        records: List<EobRecord>,
        sort: ExpenseAnalyticsSort,
        expandedFacilityIds: Set<String>,
        appealedClaimIds: Set<String>,
        issueDetector: (EobRecord) -> List<BillingIssue>,
        isLoading: Boolean
    ): ExpenseAnalyticsState {
        if (isLoading) {
            return ExpenseAnalyticsState(isLoading = true, selectedSort = sort)
        }
        if (records.isEmpty()) {
            return ExpenseAnalyticsState(isLoading = false, selectedSort = sort)
        }

        val allocation = buildAllocation(records)
        val facilities = buildFacilities(
            records = records,
            expandedFacilityIds = expandedFacilityIds,
            appealedClaimIds = appealedClaimIds,
            issueDetector = issueDetector
        )
        return ExpenseAnalyticsState(
            isLoading = false,
            allocation = allocation,
            totalPatientOutOfPocket = allocation.patientResponsibility,
            totalCarrierContribution = allocation.carrierCovered,
            totalNetworkSavings = allocation.networkSavings,
            totalBilled = allocation.totalBilled,
            facilities = sortFacilities(facilities, sort),
            selectedSort = sort
        )
    }

    fun facilityIdFor(providerName: String): String {
        return providerName.trim().lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "unknown-provider" }
    }

    fun titleCaseProviderName(providerName: String): String {
        val trimmed = providerName.trim()
        if (trimmed.isBlank()) return trimmed
        return trimmed.split(Regex("\\s+"))
            .joinToString(" ") { token ->
                token.lowercase(Locale.US).replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
                }
            }
    }

    private fun buildAllocation(records: List<EobRecord>): ExpenseAnalyticsAllocation {
        val grossBilled = records.sumOf { it.totalBilledAmount }
        val networkSavings = records.sumOf { it.totalContractualAdjustmentAmount }
        val patientResponsibility = records.sumOf { it.totalPatientResponsibility }
        val carrierCovered = records.sumOf { record ->
            record.totalInsurancePaidAmount.coerceAtLeast(0.0)
        }.let { paid ->
            if (paid > 0.0) {
                paid
            } else {
                (grossBilled - networkSavings - patientResponsibility).coerceAtLeast(0.0)
            }
        }
        return ExpenseAnalyticsAllocation(
            networkSavings = networkSavings,
            carrierCovered = carrierCovered,
            patientResponsibility = patientResponsibility
        )
    }

    private fun buildFacilities(
        records: List<EobRecord>,
        expandedFacilityIds: Set<String>,
        appealedClaimIds: Set<String>,
        issueDetector: (EobRecord) -> List<BillingIssue>
    ): List<FacilitySpending> {
        return records.groupBy { record -> record.providerName.ifBlank { "Unknown Provider" } }
            .map { (providerName, providerRecords) ->
                val totalSpent = providerRecords.sumOf { it.totalBilledAmount }
                val outOfPocketShare = providerRecords.sumOf { it.totalPatientResponsibility }
                val carrierShare = providerRecords.sumOf { record ->
                    val paid = record.totalInsurancePaidAmount
                    if (paid > 0.0) {
                        paid
                    } else {
                        (record.totalBilledAmount -
                            record.totalContractualAdjustmentAmount -
                            record.totalPatientResponsibility).coerceAtLeast(0.0)
                    }
                }
                val facilityId = facilityIdFor(providerName)
                FacilitySpending(
                    id = facilityId,
                    providerName = titleCaseProviderName(providerName),
                    totalSpent = totalSpent,
                    outOfPocketShare = outOfPocketShare,
                    carrierShare = carrierShare,
                    claims = providerRecords
                        .sortedByDescending { it.serviceDateSortKey }
                        .map { record -> toMedicalClaim(record, issueDetector, appealedClaimIds) },
                    isExpanded = facilityId in expandedFacilityIds
                )
            }
    }

    private fun toMedicalClaim(
        record: EobRecord,
        issueDetector: (EobRecord) -> List<BillingIssue>,
        appealedClaimIds: Set<String>
    ): MedicalClaim {
        val claimKey = record.historyListKey()
        val carrierCovered = record.totalInsurancePaidAmount.coerceAtLeast(0.0).let { paid ->
            if (paid > 0.0) {
                paid
            } else {
                (record.totalBilledAmount -
                    record.totalContractualAdjustmentAmount -
                    record.totalPatientResponsibility).coerceAtLeast(0.0)
            }
        }
        return MedicalClaim(
            id = claimKey,
            claimNumber = claimNumberFor(record),
            dateOfService = record.serviceDate.ifBlank { "—" },
            totalBilled = record.totalBilledAmount,
            carrierCovered = carrierCovered,
            status = resolveClaimStatus(record, issueDetector(record), appealedClaimIds),
            sourceName = record.sourceName,
            storageDownloadUrl = record.storageDownloadUrl
        )
    }

    private fun claimNumberFor(record: EobRecord): String {
        val trimmedFirestoreId = record.firestoreId.trim()
        if (trimmedFirestoreId.isNotEmpty()) {
            return trimmedFirestoreId.takeLast(8).uppercase(Locale.US)
        }
        return "EOB-${record.id.toString().padStart(4, '0')}"
    }

    private fun resolveClaimStatus(
        record: EobRecord,
        issues: List<BillingIssue>,
        appealedClaimIds: Set<String>
    ): ClaimStatus {
        if (record.historyListKey() in appealedClaimIds) {
            return ClaimStatus.Appealed
        }
        val flaggedIssues = issues.filter { issue -> issue.severity != BillingIssueSeverity.Info }
        if (flaggedIssues.isEmpty()) {
            return ClaimStatus.AuditedCorrect
        }
        val message = flaggedIssues.joinToString(separator = "; ") { issue -> issue.title }
        return ClaimStatus.PotentialError(message)
    }

    private fun sortFacilities(
        facilities: List<FacilitySpending>,
        sort: ExpenseAnalyticsSort
    ): List<FacilitySpending> {
        return when (sort) {
            ExpenseAnalyticsSort.HighestPatientShare ->
                facilities.sortedByDescending { facility ->
                    if (facility.totalSpent <= 0.0) 0.0 else facility.outOfPocketShare / facility.totalSpent
                }
            ExpenseAnalyticsSort.NewestActivity ->
                facilities.sortedByDescending { facility ->
                    facility.claims.maxOfOrNull { claim ->
                        claim.dateOfService
                    }.orEmpty()
                }
            ExpenseAnalyticsSort.HighestBilledTotal ->
                facilities.sortedByDescending { it.totalSpent }
            ExpenseAnalyticsSort.FacilityAlphabetical ->
                facilities.sortedBy { it.providerName.lowercase(Locale.US) }
        }
    }
}
