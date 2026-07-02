"use strict";

const {parseCurrency, firstPositive} = require("./veryfiCurrencyParser");
const {toIsoDate, toDisplayDate} = require("./veryfiDateNormalizer");
const {normalizeEobDocument} = require("./eobNormalizer");

const MAX_SERVICE_LINE_INDEX = 8;
const CODE_BASE_KEYS = ["cpt_code", "cptCode", "code"];
const DESCRIPTION_BASE_KEYS = ["service_description", "description", "cpt_description"];
const DATE_BASE_KEYS = ["service_date", "date_of_service", "serviceDate"];
const TOTAL_BILLED_BASE_KEYS = ["total_amount_billed", "totalAmountBilled"];
const BILLED_BASE_KEYS = ["amount_billed", "billed_amount", "amountBilled"];
const INSURANCE_PAID_BASE_KEYS = ["health_plan_responsibility", "insurance_paid", "insurancePaid"];
const CONTRACTUAL_BASE_KEYS = ["contractual_adjustment", "contractualAdjustment", "contractual_adj"];
const COPAY_BASE_KEYS = ["copay_amount", "copay", "co_pay"];
const DEDUCTIBLE_BASE_KEYS = ["deductible_amount", "deductible"];
const COINSURANCE_BASE_KEYS = ["coinsurance_amount", "coinsurance", "co_insurance"];
const PATIENT_RESP_BASE_KEYS = ["patient_responsibility", "patientResponsibility"];

function isNestedClaimsPayload(payload) {
  const claims = payload?.claims ?? payload?.Claims;
  return Array.isArray(claims) && claims.length > 0;
}

function resolveClaims(payload = {}) {
  return Array.isArray(payload.claims) ? payload.claims :
    (Array.isArray(payload.Claims) ? payload.Claims : []);
}

function stringValue(fieldMap, keys, index) {
  const suffix = `_${index}`;
  for (const baseKey of keys) {
    const indexed = fieldMap[`${baseKey}${suffix}`] ?? fieldMap[`${baseKey}${index}`];
    const value = indexed == null ? "" : String(indexed).trim();
    if (value) return value;
  }
  if (index === 1) {
    for (const baseKey of keys) {
      const value = fieldMap[baseKey] == null ? "" : String(fieldMap[baseKey]).trim();
      if (value) return value;
    }
  }
  return "";
}

function moneyValue(fieldMap, keys, index) {
  const suffix = `_${index}`;
  for (const baseKey of keys) {
    const indexed = fieldMap[`${baseKey}${suffix}`] ?? fieldMap[`${baseKey}${index}`];
    if (indexed != null) return parseCurrency(indexed);
  }
  if (index === 1) {
    for (const baseKey of keys) {
      if (fieldMap[baseKey] != null) return parseCurrency(fieldMap[baseKey]);
    }
  }
  return 0;
}

function discoverIndices(row) {
  const indices = [];
  for (let index = 1; index <= MAX_SERVICE_LINE_INDEX; index++) {
    if (stringValue(row, CODE_BASE_KEYS, index)) {
      indices.push(index);
    }
  }
  return indices;
}

function resolveServiceDateIso(row, index) {
  for (let candidate = index; candidate >= 1; candidate--) {
    const iso = toIsoDate(stringValue(row, DATE_BASE_KEYS, candidate));
    if (iso) return iso;
  }
  return "";
}

function mapServiceLineAtIndex(row, index) {
  const procedureCode = stringValue(row, CODE_BASE_KEYS, index).toUpperCase();
  if (!procedureCode) return null;
  const serviceDateIso = resolveServiceDateIso(row, index);
  const billedAmount = firstPositive(
    moneyValue(row, TOTAL_BILLED_BASE_KEYS, index),
    moneyValue(row, BILLED_BASE_KEYS, index)
  );
  return {
    cptCode: procedureCode,
    cptDescription: stringValue(row, DESCRIPTION_BASE_KEYS, index),
    billedAmount,
    insurancePaidAmount: moneyValue(row, INSURANCE_PAID_BASE_KEYS, index),
    contractualAdjustmentAmount: moneyValue(row, CONTRACTUAL_BASE_KEYS, index),
    copayAmount: moneyValue(row, COPAY_BASE_KEYS, index),
    deductibleAmount: moneyValue(row, DEDUCTIBLE_BASE_KEYS, index),
    coinsuranceAmount: moneyValue(row, COINSURANCE_BASE_KEYS, index),
    patientResponsibilityAmount: moneyValue(row, PATIENT_RESP_BASE_KEYS, index),
    serviceDateIso
  };
}

function mapClaimTotals(totalsMap, serviceLines) {
  const totalBilled = firstPositive(
    parseCurrency(totalsMap.total_billed_1),
    parseCurrency(totalsMap.total_amount_billed_1),
    serviceLines.reduce((sum, line) => sum + line.billedAmount, 0)
  );
  const totalInsurancePaid = firstPositive(
    parseCurrency(totalsMap.total_health_plan_responsibility_1),
    parseCurrency(totalsMap.health_plan_responsibility),
    serviceLines.reduce((sum, line) => sum + line.insurancePaidAmount, 0)
  );
  const totalPatientResponsibility = firstPositive(
    parseCurrency(totalsMap.total_patient_responsibility_1),
    parseCurrency(totalsMap.patient_responsibility_1),
    serviceLines.reduce((sum, line) => sum + line.patientResponsibilityAmount, 0)
  );
  const totalContractualAdjustment = firstPositive(
    parseCurrency(totalsMap.total_contractual_adjustment_1),
    parseCurrency(totalsMap.contractual_adjustment_1),
    serviceLines.reduce((sum, line) => sum + line.contractualAdjustmentAmount, 0)
  );
  return {
    totalBilled,
    totalInsurancePaid,
    totalPatientResponsibility,
    totalContractualAdjustment
  };
}

function mapClaim(claimDto, claimIndex) {
  const totalsMap = claimDto.claim_totals || claimDto.claimTotals || {};
  const serviceLineRows = Array.isArray(claimDto.service_lines) ?
    claimDto.service_lines :
    (Array.isArray(claimDto.serviceLines) ? claimDto.serviceLines : []);
  const serviceLines = serviceLineRows.flatMap((row) => {
    return discoverIndices(row)
      .map((index) => mapServiceLineAtIndex(row, index))
      .filter(Boolean);
  });
  const claimTotals = mapClaimTotals(totalsMap, serviceLines);
  return {
    providerName: String(claimDto.provider_name || claimDto.providerName || "").trim(),
    claimNumber: String(
      claimDto.claim_number_1 ||
      claimDto.claim_number_2 ||
      claimDto.claim_number ||
      claimDto.claim_id ||
      ""
    ).trim(),
    processedDateIso: toIsoDate(claimDto.processed_date || claimDto.processedDate || ""),
    serviceLines,
    claimTotals
  };
}

function nestedClaimsToEobDocument(veryfi = {}, metadata = {}) {
  const claims = resolveClaims(veryfi).map((claimDto, index) => mapClaim(claimDto, index));
  const allServiceLines = claims.flatMap((claim) => claim.serviceLines);
  const providers = [...new Set(claims.map((claim) => claim.providerName).filter(Boolean))];
  const primaryProvider = providers.join(" / ");
  const primaryDateIso = allServiceLines.map((line) => line.serviceDateIso).find(Boolean) ||
    claims[0]?.processedDateIso ||
    "";
  const displayDate = toDisplayDate(primaryDateIso);

  const charges = allServiceLines.map((line) => ({
    cptCode: line.cptCode,
    cpt_code: line.cptCode,
    cptDescription: line.cptDescription,
    description: line.cptDescription,
    billedAmount: line.billedAmount,
    billed_amount: line.billedAmount,
    insurancePaidAmount: line.insurancePaidAmount,
    insurance_paid: line.insurancePaidAmount,
    contractualAdjustmentAmount: line.contractualAdjustmentAmount,
    contractual_adj: line.contractualAdjustmentAmount,
    copayAmount: line.copayAmount,
    copay: line.copayAmount,
    deductibleAmount: line.deductibleAmount,
    deductible: line.deductibleAmount,
    coinsuranceAmount: line.coinsuranceAmount,
    coinsurance: line.coinsuranceAmount,
    serviceDate: toDisplayDate(line.serviceDateIso),
    date_of_service: toDisplayDate(line.serviceDateIso)
  }));

  const totalBilled = claims.reduce((sum, claim) => {
    return sum + (claim.claimTotals.totalBilled ||
      claim.serviceLines.reduce((lineSum, line) => lineSum + line.billedAmount, 0));
  }, 0);
  const totalInsurancePaid = claims.reduce((sum, claim) => {
    return sum + (claim.claimTotals.totalInsurancePaid ||
      claim.serviceLines.reduce((lineSum, line) => lineSum + line.insurancePaidAmount, 0));
  }, 0);
  const totalContractualAdj = claims.reduce((sum, claim) => {
    return sum + (claim.claimTotals.totalContractualAdjustment ||
      claim.serviceLines.reduce((lineSum, line) => lineSum + line.contractualAdjustmentAmount, 0));
  }, 0);
  const totalCopay = charges.reduce((sum, charge) => sum + charge.copayAmount, 0);
  const totalDeductible = charges.reduce((sum, charge) => sum + charge.deductibleAmount, 0);
  const totalCoinsurance = charges.reduce((sum, charge) => sum + charge.coinsuranceAmount, 0);
  const patientResponsibility = (totalCopay + totalDeductible + totalCoinsurance) > 0 ?
    totalCopay + totalDeductible + totalCoinsurance :
    claims.reduce((sum, claim) => sum + claim.claimTotals.totalPatientResponsibility, 0);

  const normalized = normalizeEobDocument({
    sourceName: metadata.sourceName || "Veryfi",
    providerName: primaryProvider,
    insuranceName: String(veryfi.payer_name || veryfi.payerName || "").trim(),
    payer_name: String(veryfi.payer_name || veryfi.payerName || "").trim(),
    group_name: String(veryfi.group_name || veryfi.groupName || "").trim(),
    group_number: String(veryfi.group_number || veryfi.groupNumber || "").trim(),
    member_id: String(veryfi.subscriber_id || veryfi.subscriberId || "").trim(),
    member_name: String(veryfi.subscriber_name || veryfi.subscriberName || "").trim(),
    patient_name: String(veryfi.subscriber_name || veryfi.subscriberName || "").trim(),
    benefit_type: String(veryfi.benefit_type || veryfi.benefitType || "").trim(),
    date_of_service: displayDate,
    billed_amount: totalBilled,
    insurance_paid: totalInsurancePaid,
    contractual_adj: totalContractualAdj,
    copay: totalCopay,
    deductible: totalDeductible,
    coinsurance: totalCoinsurance,
    patient_responsibility: patientResponsibility,
    blueprint_name: String(veryfi.blueprint_name || "health_insurance_eob"),
    charges,
    cptCodes: charges.map((charge) => charge.cptCode).filter(Boolean).join(","),
    rawText: JSON.stringify(veryfi),
    sourceFilePath: metadata.sourceFilePath || "",
    veryfiDocumentId: veryfi.id || veryfi.document_id || ""
  }, metadata.documentId || String(veryfi.id || veryfi.document_id || Date.now()));

  return {
    ...normalized,
    sourceFilePath: metadata.sourceFilePath || "",
    veryfiDocumentId: veryfi.id || veryfi.document_id || "",
    blueprint_name: String(veryfi.blueprint_name || "health_insurance_eob"),
    member_name: String(veryfi.subscriber_name || veryfi.subscriberName || "").trim(),
    member_id: String(veryfi.subscriber_id || veryfi.subscriberId || "").trim(),
    patient_name: String(veryfi.subscriber_name || veryfi.subscriberName || "").trim(),
    group_name: String(veryfi.group_name || veryfi.groupName || "").trim(),
    group_number: String(veryfi.group_number || veryfi.groupNumber || "").trim(),
    benefit_type: String(veryfi.benefit_type || veryfi.benefitType || "").trim()
  };
}

module.exports = {
  isNestedClaimsPayload,
  nestedClaimsToEobDocument,
  resolveClaims
};
