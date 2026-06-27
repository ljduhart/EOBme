package app.eob.me.data

import app.eob.me.network.VeryfiDateNormalizer

/**
 * Bridges normalized insurance EOB domain models into existing [EobRecord] storage/UI contracts.
 * Keeps Veryfi indexed schema out of repositories beyond the data layer boundary.
 */
object InsuranceEobRecordBridge {
    fun toEobRecord(
        document: NormalizedInsuranceEob,
        documentRefId: String,
        sourceName: String,
        rawText: String = ""
    ): EobRecord {
        val documentId = HybridDocumentRef.stableDocumentId(documentRefId)
        val charges = document.allServiceLines.map { it.toEobCharge() }
        val providers = document.claims.map { it.providerName }.filter { it.isNotBlank() }.distinct()
        val primaryProvider = providers.joinToString(" / ")
        val primaryDateIso = document.allServiceLines
            .map { it.serviceDateIso }
            .firstOrNull { it.isNotBlank() }
            ?: document.claims.firstOrNull()?.processedDateIso.orEmpty()
        val displayDate = VeryfiDateNormalizer.toDisplayDate(primaryDateIso)

        val totalBilled = document.claims.sumOf { claim ->
            claim.claimTotals?.totalBilled ?: claim.serviceLines.sumOf { it.billedAmount }
        }
        val totalInsurancePaid = document.claims.sumOf { claim ->
            claim.claimTotals?.totalInsurancePaid ?: claim.serviceLines.sumOf { it.insurancePaidAmount }
        }
        val totalContractualAdj = document.claims.sumOf { claim ->
            claim.claimTotals?.totalContractualAdjustment
                ?: claim.serviceLines.sumOf { it.contractualAdjustmentAmount }
        }
        val totalCopay = charges.sumOf { it.copayAmount }
        val totalDeductible = charges.sumOf { it.deductibleAmount }
        val totalCoinsurance = charges.sumOf { it.coinsuranceAmount }

        val normalizedFields = mapOf<String, Any?>(
            "id" to documentId,
            "sourceName" to sourceName.ifBlank { "Veryfi" },
            "provider_name" to primaryProvider,
            "insurance_name" to document.payerName,
            "payer_name" to document.payerName,
            "group_name" to document.groupName.ifBlank { document.groupNumber },
            "group_number" to document.groupNumber,
            "member_id" to document.subscriberId,
            "member_name" to document.subscriberName,
            "patient_name" to document.subscriberName,
            "benefit_type" to document.benefitType,
            "date_of_service" to displayDate,
            "billed_amount" to totalBilled,
            "insurance_paid" to totalInsurancePaid,
            "contractual_adj" to totalContractualAdj,
            "copay" to totalCopay,
            "deductible" to totalDeductible,
            "coinsurance" to totalCoinsurance,
            "patient_responsibility" to (
                (totalCopay + totalDeductible + totalCoinsurance).takeIf { it > 0.0 }
                    ?: document.claims.sumOf { it.claimTotals?.totalPatientResponsibility ?: 0.0 }
                ),
            "charges" to charges.map { chargeToMap(it) },
            "cptCodes" to charges.joinToString(",") { it.cptCode },
            "rawText" to rawText
        )
        return FirebaseEobMapper.eobFromMap(normalizedFields, documentId)
    }

    fun ServiceLine.toEobCharge(): EobCharge {
        val fallback = EobKnowledgeBase.cptInfoFor(procedureCode)
        return EobCharge(
            cptCode = procedureCode,
            cptDescription = description.ifBlank { fallback.description },
            category = fallback.category,
            billedAmount = billedAmount,
            insurancePaidAmount = insurancePaidAmount,
            contractualAdjustmentAmount = contractualAdjustmentAmount,
            copayAmount = copayAmount,
            deductibleAmount = deductibleAmount,
            coinsuranceAmount = coinsuranceAmount,
            serviceDate = VeryfiDateNormalizer.toDisplayDate(serviceDateIso)
        )
    }

    private fun chargeToMap(charge: EobCharge): Map<String, Any?> {
        return mapOf(
            "cptCode" to charge.cptCode,
            "cpt_code" to charge.cptCode,
            "cptDescription" to charge.cptDescription,
            "description" to charge.cptDescription,
            "category" to charge.category.name,
            "billedAmount" to charge.billedAmount,
            "billed_amount" to charge.billedAmount,
            "insurancePaidAmount" to charge.insurancePaidAmount,
            "insurance_paid" to charge.insurancePaidAmount,
            "contractualAdjustmentAmount" to charge.contractualAdjustmentAmount,
            "contractual_adj" to charge.contractualAdjustmentAmount,
            "copayAmount" to charge.copayAmount,
            "copay" to charge.copayAmount,
            "deductibleAmount" to charge.deductibleAmount,
            "deductible" to charge.deductibleAmount,
            "coinsuranceAmount" to charge.coinsuranceAmount,
            "coinsurance" to charge.coinsuranceAmount,
            "serviceDate" to charge.serviceDate,
            "date_of_service" to charge.serviceDate
        )
    }
}
