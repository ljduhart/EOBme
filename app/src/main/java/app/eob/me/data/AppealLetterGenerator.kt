package app.eob.me.data

object AppealLetterGenerator {
    fun generate(profile: UserProfile, eob: EobRecord?): String {
        val selectedEob = eob ?: return emptyDraft(profile)
        val memberName = profile.fullName.ifBlank { "[Member name]" }
        val cityState = listOf(profile.city, profile.state).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "[City, State]" }
        val groupNumber = profile.insuranceGroupNumber.ifBlank { "[Insurance group number]" }
        val issues = EobAnalyzer.detectBillingIssues(selectedEob)
        val issueSection = issueAppealSection(issues)

        return """
            To: ${selectedEob.insuranceName}

            Re: Appeal of EOB determination

            Member: $memberName
            Insurance group number: $groupNumber
            Location: $cityState
            Provider: ${selectedEob.providerName}
            Date of Service: ${selectedEob.serviceDate}

            I am requesting a review of the Explanation of Benefits for the services listed above. The EOB shows a billed amount of ${selectedEob.totalBilledAmount.asCurrency()}, insurance paid of ${selectedEob.totalInsurancePaidAmount.asCurrency()}, and contractual adjustment of ${selectedEob.totalContractualAdjustmentAmount.asCurrency()}.

            $issueSection

            Please reprocess this claim, confirm the provider contract rate, and provide a written explanation for any copay, deductible, coinsurance, denial, or patient responsibility amount that remains.

            Thank you,
            $memberName
        """.trimIndent()
    }

    private fun issueAppealSection(issues: List<BillingIssue>): String {
        if (issues.isEmpty()) {
            return "I am requesting confirmation that this claim was processed according to my plan benefits and the provider contract."
        }
        return buildString {
            appendLine("Issues identified for review:")
            issues.forEach { issue ->
                appendLine("- ${issue.title}: ${issue.explanation}")
                appendLine("  Requested action: ${issue.recommendedAction}")
            }
            append("Please treat this appeal as a request for a full claim review and written explanation for each issue above.")
        }
    }

    private fun emptyDraft(profile: UserProfile): String {
        val memberName = profile.fullName.ifBlank { "[Member name]" }
        return """
            To: [Insurance company]

            Re: Appeal of EOB determination

            Member: $memberName
            Insurance group number: ${profile.insuranceGroupNumber.ifBlank { "[Insurance group number]" }}

            Please review the attached Explanation of Benefits and reprocess the claim according to the plan benefits and provider contract.

            Thank you,
            $memberName
        """.trimIndent()
    }
}
