package com.eobme.app.data

object AppealLetterGenerator {
    fun generate(profile: UserProfile, eob: EobRecord?): String {
        val selectedEob = eob ?: return emptyDraft(profile)
        val memberName = profile.fullName.ifBlank { "[Member name]" }
        val cityState = listOf(profile.city, profile.state).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "[City, State]" }
        val subscriber = profile.subscriberId.ifBlank { "[Subscriber ID]" }

        return """
            To: ${selectedEob.insuranceName}

            Re: Appeal of EOB determination

            Member: $memberName
            Subscriber ID: $subscriber
            Location: $cityState
            Provider: ${selectedEob.providerName}
            Date of Service: ${selectedEob.serviceDate}

            I am requesting a review of the Explanation of Benefits for the services listed above. The EOB shows a billed amount of ${selectedEob.totalBilledAmount.asCurrency()}, insurance paid of ${selectedEob.totalInsurancePaidAmount.asCurrency()}, and contractual adjustment of ${selectedEob.totalContractualAdjustmentAmount.asCurrency()}.

            Please reprocess this claim, confirm the provider contract rate, and provide a written explanation for any copay, deductible, coinsurance, denial, or patient responsibility amount that remains.

            Thank you,
            $memberName
        """.trimIndent()
    }

    private fun emptyDraft(profile: UserProfile): String {
        val memberName = profile.fullName.ifBlank { "[Member name]" }
        return """
            To: [Insurance company]

            Re: Appeal of EOB determination

            Member: $memberName
            Subscriber ID: ${profile.subscriberId.ifBlank { "[Subscriber ID]" }}

            Please review the attached Explanation of Benefits and reprocess the claim according to the plan benefits and provider contract.

            Thank you,
            $memberName
        """.trimIndent()
    }
}
