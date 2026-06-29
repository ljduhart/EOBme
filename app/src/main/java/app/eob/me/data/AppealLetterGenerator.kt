package app.eob.me.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppealLetterGenerator {
    fun generate(
        profile: UserProfile,
        eob: EobRecord?,
        target: AppealTarget = AppealTarget.INSURANCE,
        strategy: DoctorDisputeStrategy = DoctorDisputeStrategy.IMPROPER_BALANCE_BILLING,
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
        val letterDate = formattedLetterDate()
        val providerName = resolvedProviderName(selectedEob, veryfiData)
        val accountNumber = profile.insuranceId.ifBlank { "[Your Account Number]" }
        val patientName = profile.fullName.ifBlank { "[Your Name]" }
        val serviceDate = resolvedServiceDate(selectedEob, veryfiData)
        val disputedAmount = resolvedPatientResponsibility(selectedEob, veryfiData)
        val statementDate = resolvedStatementDate(selectedEob, veryfiData)
        val disputeReason = doctorDisputeReason(strategy)
        val phoneNumber = "[Your Phone Number]"
        val emailAddress = profile.email.ifBlank { "[Your Email Address]" }

        return """
            $letterDate

            To: $providerName - Billing Department
            Re: Account Number: $accountNumber
            Patient Name: $patientName
            Date of Service: $serviceDate
            Disputed Amount: $disputedAmount

            I am writing to formally dispute the balance of $disputedAmount listed on my recent billing statement dated $statementDate for services rendered on $serviceDate. After carefully reviewing my itemized bill and comparing it with my insurance company's Explanation of Benefits (EOB), I am requesting a formal review of this account because $disputeReason I have attached copies of my current bill and my insurance EOB for your reference.

            I respectfully request that you place my account on hold to prevent any late fees or progression to collections while we resolve this billing discrepancy. Please review the attached documentation, adjust the charges or submit a corrected claim to my insurance as necessary, and provide me with an updated statement reflecting the accurate, resolved balance. Thank you for your time and assistance; you can reach me at $phoneNumber or $emailAddress if you need any further information to process this review.

            Sincerely,

            $patientName
            [Your Signature]
            $emailAddress
        """.trimIndent()
    }

    private fun doctorDisputeReason(strategy: DoctorDisputeStrategy): String {
        return when (strategy) {
            DoctorDisputeStrategy.IMPROPER_BALANCE_BILLING ->
                "the amount billed exceeds my patient responsibility. Your office is an in-network provider with my insurance, and according to the EOB, you are contractually obligated to write off the network discount rather than billing me for the difference."
            DoctorDisputeStrategy.CODING_UPCODING_ERROR ->
                "the services billed do not accurately reflect the level of care I received during my visit. I was billed for a highly complex, extended visit and procedures that were not performed, rather than the standard routine check-up that actually occurred."
            DoctorDisputeStrategy.PRIOR_AUTHORIZATION_FAILURE ->
                "my insurance denied the claim due to a lack of prior authorization. As the treating provider, your office was responsible for securing this clinical authorization prior to the procedure, and I should not be held financially liable for an administrative oversight."
            DoctorDisputeStrategy.NO_SURPRISES_ACT ->
                "this charge violates the protections under the federal No Surprises Act. I received care at an in-network facility, but was billed by an out-of-network provider who I did not explicitly choose or consent to see."
        }
    }

    private fun formattedLetterDate(): String {
        return SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
    }

    private fun resolvedProviderName(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        return veryfiData?.providerName?.takeIf { it.isNotBlank() }
            ?: selectedEob.providerName.ifBlank { "[Provider or Clinic Name]" }
    }

    private fun resolvedStatementDate(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        return resolvedServiceDate(selectedEob, veryfiData).takeIf { it != "[Date]" }
            ?: formattedLetterDate()
    }

    private fun resolvedServiceDate(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        return veryfiData?.dateOfService?.takeIf { it.isNotBlank() }
            ?: selectedEob.serviceDate.ifBlank { "[Date of Service]" }
    }

    private fun resolvedPatientResponsibility(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        val amount = veryfiData?.patientResponsibility?.takeIf { it > 0.0 }
            ?: selectedEob.totalPatientResponsibility.takeIf { it > 0.0 }
        return amount?.asCurrency() ?: "[Amount]"
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
        val memberName = profile.fullName.ifBlank { "[Your Name]" }
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
                ${formattedLetterDate()}

                To: [Provider or Clinic Name] - Billing Department
                Re: Account Number: ${profile.insuranceId.ifBlank { "[Your Account Number]" }}
                Patient Name: $memberName
                Date of Service: [Date of Service]
                Disputed Amount: [Amount]

                I am writing to formally dispute the balance listed on my recent billing statement. Please review the attached bill and insurance EOB and provide an updated statement.

                Sincerely,

                $memberName
                [Your Signature]
                ${profile.email.ifBlank { "[Your Email Address]" }}
            """.trimIndent()
        }
    }
}
