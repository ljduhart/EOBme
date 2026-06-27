package app.eob.me.network

import app.eob.me.data.InsuranceClaim
import app.eob.me.data.InsuranceClaimTotals
import app.eob.me.data.NormalizedInsuranceEob
import app.eob.me.data.NormalizedInsuranceEobResult
import app.eob.me.data.ServiceLine
import app.eob.me.network.dto.VeryfiIndexedServiceLineDto
import app.eob.me.network.dto.VeryfiInsuranceClaimDto
import app.eob.me.network.dto.VeryfiInsuranceEobResponseDto
import com.google.gson.Gson
import java.util.Locale

/**
 * Parses untyped Veryfi hybrid-stream maps into the nested insurance EOB DTO.
 */
object VeryfiInsuranceEobPayloadParser {
    private val gson = Gson()

    fun isNestedClaimsPayload(payload: Map<String, Any?>): Boolean {
        val claims = payload["claims"] ?: payload["Claims"]
        return claims is List<*> && claims.isNotEmpty()
    }

    fun parse(payload: Map<String, Any?>): VeryfiInsuranceEobResponseDto {
        val json = gson.toJson(payload)
        return gson.fromJson(json, VeryfiInsuranceEobResponseDto::class.java)
    }
}

/**
 * Maps [VeryfiInsuranceEobResponseDto] into normalized domain models.
 * Veryfi flat indexed fields never leak past this mapper.
 */
fun VeryfiInsuranceEobResponseDto.toNormalizedInsuranceEob(): Result<NormalizedInsuranceEobResult> {
    return runCatching {
        val warnings = mutableListOf<String>()
        val claimDtos = resolvedClaims()
        if (claimDtos.isEmpty()) {
            error("Veryfi insurance EOB payload is missing claims[].")
        }

        val claims = claimDtos.mapIndexed { index, claimDto ->
            mapClaim(claimDto, index, warnings)
        }

        NormalizedInsuranceEobResult(
            document = NormalizedInsuranceEob(
                groupName = firstNonBlank(groupName, groupNameCamel),
                payerName = firstNonBlank(payerName, payerNameCamel),
                benefitType = firstNonBlank(benefitType, benefitTypeCamel),
                groupNumber = firstNonBlank(groupNumber, groupNumberCamel),
                subscriberId = firstNonBlank(subscriberId, subscriberIdCamel, memberId),
                subscriberName = firstNonBlank(subscriberName, subscriberNameCamel, patientName),
                benefitYearMaxRemaining = VeryfiCurrencyParser.parse(benefitYearMaxRemaining),
                orthodontiaMaxRemaining = VeryfiCurrencyParser.parse(orthodontiaMaxRemaining),
                claims = claims
            ),
            warnings = warnings
        )
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { error -> Result.failure(error) }
    )
}

fun Map<String, Any?>.toNormalizedInsuranceEob(): Result<NormalizedInsuranceEobResult> {
    if (!VeryfiInsuranceEobPayloadParser.isNestedClaimsPayload(this)) {
        return Result.failure(IllegalArgumentException("Payload does not contain nested insurance EOB claims."))
    }
    return VeryfiInsuranceEobPayloadParser.parse(this).toNormalizedInsuranceEob()
}

private fun mapClaim(
    claimDto: VeryfiInsuranceClaimDto,
    claimIndex: Int,
    warnings: MutableList<String>
): InsuranceClaim {
    val totalsMap = claimDto.resolvedClaimTotals()
    val serviceLineRows = claimDto.resolvedServiceLines()
    val serviceLines = serviceLineRows.flatMap { row ->
        mapIndexedServiceLineRow(row, warnings)
    }

    val resolvedLines = serviceLines.ifEmpty {
        warnings.add("claim $claimIndex has no valid service lines; synthesizing from claim_totals")
        listOf(syntheticServiceLineFromTotals(totalsMap, claimDto))
    }

    return InsuranceClaim(
        claimIndex = claimIndex,
        providerName = firstNonBlank(claimDto.providerName, claimDto.providerNameCamel),
        claimNumber = firstNonBlank(
            claimDto.claimNumber1,
            claimDto.claimNumber2,
            claimDto.claimNumber,
            claimDto.claimId
        ),
        processedDateIso = VeryfiDateNormalizer.toIsoDate(
            firstNonBlank(claimDto.processedDate, claimDto.processedDateCamel)
        ),
        serviceLines = resolvedLines,
        claimTotals = totalsMap.takeIf { it.isNotEmpty() }?.let { mapClaimTotals(it, resolvedLines) }
    )
}

/**
 * Unpivots one Veryfi `service_lines` row into 1–8 [ServiceLine] entries.
 * Indices with blank procedure codes are skipped without failing the entire claim.
 */
private fun mapIndexedServiceLineRow(
    row: VeryfiIndexedServiceLineDto,
    warnings: MutableList<String>
): List<ServiceLine> {
    return VeryfiIndexedFieldReader.discoverIndices(row).mapNotNull { index ->
        mapServiceLineAtIndex(row, index).fold(
            onSuccess = { it },
            onFailure = { error ->
                val message = "service line index $index skipped: ${error.message.orEmpty()}"
                warnings.add(message)
                null
            }
        )
    }
}

private fun mapServiceLineAtIndex(
    row: VeryfiIndexedServiceLineDto,
    index: Int
): Result<ServiceLine> {
    return runCatching {
        val procedureCode = VeryfiIndexedFieldReader
            .stringValue(row, VeryfiIndexedFieldReader.CODE_BASE_KEYS, index)
            .trim()
            .uppercase(Locale.US)
        if (procedureCode.isBlank()) {
            error("procedure code is blank")
        }

        val description = VeryfiIndexedFieldReader
            .stringValue(row, VeryfiIndexedFieldReader.DESCRIPTION_BASE_KEYS, index)
        val serviceDateIso = VeryfiDateNormalizer.toIsoDate(
            VeryfiIndexedFieldReader.stringValue(row, VeryfiIndexedFieldReader.DATE_BASE_KEYS, index)
        )

        val billedAmount = VeryfiCurrencyParser.firstPositive(
            VeryfiIndexedFieldReader.moneyValue(row, VeryfiIndexedFieldReader.TOTAL_BILLED_BASE_KEYS, index),
            VeryfiIndexedFieldReader.moneyValue(row, VeryfiIndexedFieldReader.BILLED_BASE_KEYS, index),
            VeryfiIndexedFieldReader.moneyValue(row, VeryfiIndexedFieldReader.ALLOWED_BASE_KEYS, index)
        )
        val allowedAmount = VeryfiIndexedFieldReader.moneyValue(row, VeryfiIndexedFieldReader.ALLOWED_BASE_KEYS, index)
        val insurancePaidAmount = VeryfiIndexedFieldReader.moneyValue(
            row,
            VeryfiIndexedFieldReader.INSURANCE_PAID_BASE_KEYS,
            index
        )
        val contractualAdjustmentAmount = VeryfiIndexedFieldReader.moneyValue(
            row,
            VeryfiIndexedFieldReader.CONTRACTUAL_BASE_KEYS,
            index
        )
        val copayAmount = VeryfiIndexedFieldReader.moneyValue(row, VeryfiIndexedFieldReader.COPAY_BASE_KEYS, index)
        val deductibleAmount = VeryfiIndexedFieldReader.moneyValue(
            row,
            VeryfiIndexedFieldReader.DEDUCTIBLE_BASE_KEYS,
            index
        )
        var coinsuranceAmount = VeryfiIndexedFieldReader.moneyValue(
            row,
            VeryfiIndexedFieldReader.COINSURANCE_BASE_KEYS,
            index
        )
        val patientResponsibilityAmount = VeryfiIndexedFieldReader.moneyValue(
            row,
            VeryfiIndexedFieldReader.PATIENT_RESP_BASE_KEYS,
            index
        )
        if (patientResponsibilityAmount > 0.0 &&
            copayAmount + deductibleAmount + coinsuranceAmount <= 0.0
        ) {
            coinsuranceAmount = patientResponsibilityAmount
        }

        ServiceLine(
            lineIndex = index,
            procedureCode = procedureCode,
            description = description,
            serviceDateIso = serviceDateIso,
            billedAmount = billedAmount,
            allowedAmount = allowedAmount,
            insurancePaidAmount = insurancePaidAmount,
            contractualAdjustmentAmount = contractualAdjustmentAmount,
            copayAmount = copayAmount,
            deductibleAmount = deductibleAmount,
            coinsuranceAmount = coinsuranceAmount,
            patientResponsibilityAmount = patientResponsibilityAmount
        )
    }
}

private fun syntheticServiceLineFromTotals(
    totalsMap: Map<String, Any?>,
    claimDto: VeryfiInsuranceClaimDto
): ServiceLine {
    val processedDateIso = VeryfiDateNormalizer.toIsoDate(
        firstNonBlank(claimDto.processedDate, claimDto.processedDateCamel)
    )
    return ServiceLine(
        lineIndex = 1,
        procedureCode = "",
        description = "Claim total",
        serviceDateIso = processedDateIso,
        billedAmount = VeryfiCurrencyParser.firstPositive(
            VeryfiIndexedFieldReader.moneyValue(totalsMap, listOf("total_billed_1", "total_amount_billed_1")),
            VeryfiIndexedFieldReader.moneyValue(totalsMap, listOf("totalBilled1"))
        ),
        allowedAmount = 0.0,
        insurancePaidAmount = VeryfiIndexedFieldReader.moneyValue(
            totalsMap,
            listOf("total_health_plan_responsibility_1", "health_plan_responsibility")
        ),
        contractualAdjustmentAmount = VeryfiIndexedFieldReader.moneyValue(
            totalsMap,
            listOf("total_contractual_adjustment_1")
        ),
        copayAmount = 0.0,
        deductibleAmount = 0.0,
        coinsuranceAmount = 0.0,
        patientResponsibilityAmount = VeryfiIndexedFieldReader.moneyValue(
            totalsMap,
            listOf("total_patient_responsibility_1", "patient_responsibility_1")
        )
    )
}

private fun mapClaimTotals(
    totalsMap: Map<String, Any?>,
    lines: List<ServiceLine>
): InsuranceClaimTotals {
    return InsuranceClaimTotals(
        totalBilled = VeryfiCurrencyParser.firstPositive(
            VeryfiIndexedFieldReader.moneyValue(totalsMap, listOf("total_billed_1", "total_amount_billed_1")),
            lines.sumOf { it.billedAmount }
        ),
        totalInsurancePaid = VeryfiCurrencyParser.firstPositive(
            VeryfiIndexedFieldReader.moneyValue(
                totalsMap,
                listOf("total_health_plan_responsibility_1", "health_plan_responsibility")
            ),
            lines.sumOf { it.insurancePaidAmount }
        ),
        totalContractualAdjustment = VeryfiCurrencyParser.firstPositive(
            VeryfiIndexedFieldReader.moneyValue(totalsMap, listOf("total_contractual_adjustment_1")),
            lines.sumOf { it.contractualAdjustmentAmount }
        ),
        totalPatientResponsibility = VeryfiCurrencyParser.firstPositive(
            VeryfiIndexedFieldReader.moneyValue(
                totalsMap,
                listOf("total_patient_responsibility_1", "patient_responsibility_1")
            ),
            lines.sumOf { it.patientResponsibilityAmount }
        ),
        totalCopay = lines.sumOf { it.copayAmount },
        totalDeductible = lines.sumOf { it.deductibleAmount },
        totalCoinsurance = lines.sumOf { it.coinsuranceAmount }
    )
}

private fun firstNonBlank(vararg values: String?): String =
    values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
