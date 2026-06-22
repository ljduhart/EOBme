package app.eob.me.data

object AppealLetterGenerator {
    fun generate(
        profile: UserProfile,
        eob: EobRecord?,
        target: AppealTarget = AppealTarget.INSURANCE,
        strategy: DoctorDisputeStrategy = DoctorDisputeStrategy.ITEMIZED_AUDIT,
        veryfiData: VeryfiExtractedData? = null
    ): String {
        val selectedEob = eob ?: return emptyDraft(profile, target)
        return when (target) {
            AppealTarget.INSURANCE -> generateInsuranceAppeal(profile, selectedEob, veryfiData)
            AppealTarget.DOCTOR -> generateDoctorAppeal(profile, selectedEob, strategy, veryfiData)
        }
    }

    private fun generateInsuranceAppeal(
        profile: UserProfile,
        selectedEob: EobRecord,
        veryfiData: VeryfiExtractedData?
    ): String {
        val memberName = profile.fullName.ifBlank { "[Member name]" }
        val cityState = listOf(profile.city, profile.state).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "[City, State]" }
        val subscriber = profile.insuranceId.ifBlank { "[Insurance ID]" }
        val serviceDate = resolvedServiceDate(selectedEob, veryfiData)
        val issues = EobAnalyzer.detectBillingIssues(selectedEob)
        val issueSection = issueAppealSection(issues)

        return """
            To: ${veryfiData?.insuranceCompanyName?.takeIf { it.isNotBlank() } ?: selectedEob.insuranceName}

            Re: Appeal of EOB determination

            Member: $memberName
            Insurance ID: $subscriber
            Location: $cityState
            Provider: ${veryfiData?.providerName?.takeIf { it.isNotBlank() } ?: selectedEob.providerName}
            Date of Service: $serviceDate

            I am requesting a review of the Explanation of Benefits for the services listed above. The EOB shows a billed amount of ${selectedEob.totalBilledAmount.asCurrency()}, insurance paid of ${selectedEob.totalInsurancePaidAmount.asCurrency()}, and contractual adjustment of ${selectedEob.totalContractualAdjustmentAmount.asCurrency()}.

            $issueSection

            Please reprocess this claim, confirm the provider contract rate, and provide a written explanation for any copay, deductible, coinsurance, denial, or patient responsibility amount that remains.

            Thank you,
            $memberName
        """.trimIndent()
    }

    private fun generateDoctorAppeal(
        profile: UserProfile,
        selectedEob: EobRecord,
        strategy: DoctorDisputeStrategy,
        veryfiData: VeryfiExtractedData?
    ): String {
        val memberName = profile.fullName.ifBlank { "[Member name]" }
        val serviceDate = resolvedServiceDate(selectedEob, veryfiData)
        val providerName = veryfiData?.providerName?.takeIf { it.isNotBlank() } ?: selectedEob.providerName
        val strategySection = doctorDisputeStrategySection(selectedEob, strategy, veryfiData)

        return """
            To: $providerName

            Re: Patient billing dispute and account review

            Member: $memberName
            Provider: $providerName
            Date of Service: $serviceDate

            $strategySection

            Please respond in writing with your findings and an updated patient responsibility statement if adjustments are warranted.

            Thank you,
            $memberName
        """.trimIndent()
    }

    private fun doctorDisputeStrategySection(
        selectedEob: EobRecord,
        strategy: DoctorDisputeStrategy,
        veryfiData: VeryfiExtractedData?
    ): String {
        val serviceDate = resolvedServiceDate(selectedEob, veryfiData)
        val copayAmount = resolvedCopayAmount(selectedEob, veryfiData)
        val cptCodes = resolvedCptCodes(selectedEob, veryfiData)
        val cptSection = if (cptCodes.isNotEmpty()) {
            " including ${cptCodes.joinToString(", ")}"
        } else {
            ""
        }

        return when (strategy) {
            DoctorDisputeStrategy.ITEMIZED_AUDIT ->
                "I am formally requesting a complete, unbundled itemized billing statement for the services rendered on $serviceDate. Under federal billing transparency regulations, please provide a line-item audit reflecting all specific CPT and HCPCS procedural codes$cptSection, facility fees, and associated pharmacologic charges. Please place a temporary hold on this account pending the review of these line items."
            DoctorDisputeStrategy.UNAPPLIED_COPAY ->
                "I am writing to dispute the remaining balance on this account. My records indicate that the required point-of-service patient copayment/coinsurance of $copayAmount was fully tendered. It appears this payment has not been appropriately credited to the final patient responsibility ledger of ${resolvedPatientResponsibility(selectedEob, veryfiData)}. Please review your transactional accounting records, apply the missing credit, and issue a corrected statement."
            DoctorDisputeStrategy.FINANCIAL_HARDSHIP ->
                "I am requesting a formal review of this balance for a financial hardship adjustment or self-pay fee schedule write-off. As this current liability of ${resolvedPatientResponsibility(selectedEob, veryfiData)} exceeds my un-budgeted out-of-pocket medical capacity, I ask that you review this account under your facility's Charity Care or uninsured patient forgiveness policies, or offer a negotiated one-time settlement rate."
        }
    }

    private fun resolvedServiceDate(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        return veryfiData?.dateOfService?.takeIf { it.isNotBlank() }
            ?: selectedEob.serviceDate.ifBlank { "[Date]" }
    }

    private fun resolvedCopayAmount(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        val copay = veryfiData?.copay?.takeIf { it > 0.0 }
            ?: selectedEob.totalCopayAmount.takeIf { it > 0.0 }
            ?: selectedEob.charges.map { it.copayAmount }.filter { it > 0.0 }.maxOrNull()
        return copay?.asCurrency() ?: "[Amount]"
    }

    private fun resolvedPatientResponsibility(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        val amount = veryfiData?.patientResponsibility?.takeIf { it > 0.0 }
            ?: selectedEob.totalPatientResponsibility.takeIf { it > 0.0 }
        return amount?.asCurrency() ?: "[Amount]"
    }

    private fun resolvedCptCodes(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): List<String> {
        val fromVeryfi = veryfiData?.cptCodes.orEmpty().filter { it.isNotBlank() }
        if (fromVeryfi.isNotEmpty()) return fromVeryfi
        return selectedEob.charges.map { it.cptCode.trim() }.filter { it.isNotBlank() }.distinct()
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

    private fun emptyDraft(profile: UserProfile, target: AppealTarget): String {
        val memberName = profile.fullName.ifBlank { "[Member name]" }
        return when (target) {
            AppealTarget.INSURANCE -> """
                To: [Insurance company]

                Re: Appeal of EOB determination

                Member: $memberName
                Insurance ID: ${profile.insuranceId.ifBlank { "[Insurance ID]" }}

                Please review the attached Explanation of Benefits and reprocess the claim according to the plan benefits and provider contract.

                Thank you,
                $memberName
            """.trimIndent()
            AppealTarget.DOCTOR -> """
                To: [Provider name]

                Re: Patient billing dispute and account review

                Member: $memberName

                Please review the attached patient statement and provide a written response regarding the disputed balance.

                Thank you,
                $memberName
            """.trimIndent()
        }
    }
}
