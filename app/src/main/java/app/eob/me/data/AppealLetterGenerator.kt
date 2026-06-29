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
        insuranceStrategy: InsuranceAppealStrategy = InsuranceAppealStrategy.PROCESSED_INCORRECTLY,
        veryfiData: VeryfiExtractedData? = null
    ): String {
        val selectedEob = eob ?: return emptyDraft(profile, target)
        return when (target) {
            AppealTarget.INSURANCE -> generateInsuranceAppeal(profile, selectedEob, insuranceStrategy, veryfiData)
            AppealTarget.DOCTOR -> generateDoctorAppeal(profile, selectedEob, strategy, veryfiData)
        }
    }

    private fun generateInsuranceAppeal(
        profile: UserProfile,
        selectedEob: EobRecord,
        strategy: InsuranceAppealStrategy,
        veryfiData: VeryfiExtractedData?
    ): String {
        val letterDate = formattedLetterDate()
        val insuranceCompany = resolvedInsuranceCompany(profile, selectedEob, veryfiData)
        val memberName = profile.fullName.ifBlank { "[Your Name]" }
        val memberId = profile.insuranceId.ifBlank { "[Your Member ID]" }
        val groupNumber = profile.groupNumber.ifBlank { "[Your Group Number]" }
        val claimNumber = "[Claim Number listed on EOB]"
        val serviceDate = resolvedServiceDate(selectedEob, veryfiData)
        val providerName = resolvedProviderName(selectedEob, veryfiData)
        val disputedAmount = resolvedPatientResponsibility(selectedEob, veryfiData)
        val eobDate = resolvedEobDate(selectedEob, veryfiData)
        val appealReason = insuranceAppealReason(strategy)
        val phoneNumber = "[Your Phone Number]"
        val emailAddress = profile.email.ifBlank { "[Your Email Address]" }
        val mailingAddress = profile.locationLine().ifBlank { "[Your Mailing Address]" }

        return """
            $letterDate

            To: $insuranceCompany – Member Appeals Department

            Re: Formal Claim Appeal for Member: $memberName

            Member ID: $memberId

            Group Number: $groupNumber

            Claim Number: $claimNumber

            Date of Service: $serviceDate

            Provider Name: $providerName

            I am writing to formally appeal the processing of the healthcare claim referenced above, which resulted in an unexpected patient balance of $disputedAmount on my recent Explanation of Benefits (EOB) dated $eobDate. After carefully reviewing my plan's Summary of Benefits, my provider's billing statement, and the EOB in question, I am disputing this determination because $appealReason Based on my active coverage policy at the time of the visit, the services rendered by $providerName fall well within my plan's covered benefits and should not have resulted in this financial outcome.

            Enclosed with this letter, please find a copy of the disputed Explanation of Benefits, the itemized statement from my healthcare provider, and [insert additional proof, e.g., a copy of my insurance card / my plan's coverage ledger]. I respectfully request that your claims department conduct a secondary review of this file, correct the system adjudication error, and issue a revised EOB reflecting the proper contractual reimbursement to the provider and the accurate patient responsibility. Please confirm receipt of this appeal and notify me of your written determination within the standard 30-day review window; I can be reached at $phoneNumber or $emailAddress should your review team require any clarification.

            Sincerely,

            $memberName

            [Your Signature]

            $mailingAddress
        """.trimIndent()
    }

    private fun insuranceAppealReason(strategy: InsuranceAppealStrategy): String {
        return when (strategy) {
            InsuranceAppealStrategy.PROCESSED_INCORRECTLY ->
                "your system appears to have processed this claim incorrectly. The billing codes submitted by the provider match covered standard benefits under my policy, yet the claim was finalized with administrative calculation errors that do not align with my plan's contracted fee schedule."
            InsuranceAppealStrategy.DENIED_INCORRECTLY ->
                "this claim was denied incorrectly. The procedures performed were medically necessary to treat my diagnosed condition, were ordered by an authorized provider, and are explicitly listed as covered medical services under my active plan document."
            InsuranceAppealStrategy.PATIENT_RESPONSIBILITY_INCORRECT ->
                "my patient responsibility was assigned incorrectly. According to my year-to-date plan records, I had already met my [select one: deductible / out-of-pocket maximum] prior to this date of service, meaning this claim should have been reimbursed at a higher tier rather than shifting the balance to me."
        }
    }

    private fun generateDoctorAppeal(
        profile: UserProfile,
        selectedEob: EobRecord,
        strategy: DoctorDisputeStrategy,
        veryfiData: VeryfiExtractedData?
    ): String {
        val letterDate = formattedLetterDate()
        val insuranceCompany = resolvedInsuranceCompany(profile, selectedEob, veryfiData)
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
            Insurance Carrier: $insuranceCompany
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

    private fun resolvedInsuranceCompany(
        profile: UserProfile,
        selectedEob: EobRecord,
        veryfiData: VeryfiExtractedData?
    ): String {
        return profile.insuranceCompany.takeIf { it.isNotBlank() }
            ?: veryfiData?.insuranceCompanyName?.takeIf { it.isNotBlank() }
            ?: selectedEob.insuranceName.takeIf {
                it.isNotBlank() && !it.contains("not recognized", ignoreCase = true)
            }
            ?: "[Insurance Company Name]"
    }

    private fun resolvedProviderName(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        return veryfiData?.providerName?.takeIf { it.isNotBlank() }
            ?: selectedEob.providerName.ifBlank { "[Provider or Clinic Name]" }
    }

    private fun resolvedStatementDate(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        return resolvedServiceDate(selectedEob, veryfiData).takeIf { it != "[Date]" }
            ?: formattedLetterDate()
    }

    private fun resolvedEobDate(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        return resolvedServiceDate(selectedEob, veryfiData).takeIf { it.isNotBlank() && it != "[Date of Service]" }
            ?: "[Date of EOB]"
    }

    private fun resolvedServiceDate(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        return veryfiData?.dateOfService?.takeIf { it.isNotBlank() }
            ?: selectedEob.serviceDate.ifBlank { "[Date of Service]" }
    }

    private fun resolvedPatientResponsibility(selectedEob: EobRecord, veryfiData: VeryfiExtractedData?): String {
        val amount = veryfiData?.patientResponsibility?.takeIf { it > 0.0 }
            ?: selectedEob.totalPatientResponsibility.takeIf { it > 0.0 }
        return amount?.asCurrency() ?: "[Disputed Amount]"
    }

    private fun emptyDraft(profile: UserProfile, target: AppealTarget): String {
        val memberName = profile.fullName.ifBlank { "[Your Name]" }
        val insuranceCompany = profile.insuranceCompany.ifBlank { "[Insurance Company Name]" }
        return when (target) {
            AppealTarget.INSURANCE -> """
                ${formattedLetterDate()}

                To: $insuranceCompany – Member Appeals Department

                Re: Formal Claim Appeal for Member: $memberName

                Member ID: ${profile.insuranceId.ifBlank { "[Your Member ID]" }}

                Group Number: ${profile.groupNumber.ifBlank { "[Your Group Number]" }}

                Claim Number: [Claim Number listed on EOB]

                Date of Service: [Date of Service]

                Provider Name: [Doctor or Facility Name]

                Please review the attached Explanation of Benefits and reprocess the claim according to the plan benefits and provider contract.

                Sincerely,

                $memberName

                [Your Signature]

                ${profile.locationLine().ifBlank { "[Your Mailing Address]" }}
            """.trimIndent()
            AppealTarget.DOCTOR -> """
                ${formattedLetterDate()}

                To: [Provider or Clinic Name] - Billing Department
                Re: Account Number: ${profile.insuranceId.ifBlank { "[Your Account Number]" }}
                Patient Name: $memberName
                Insurance Carrier: $insuranceCompany
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
