"use strict";

const INDEX_SUFFIX = /_(\d+)$/;

function isNestedInsuranceEobPayload(payload = {}) {
  return Array.isArray(payload.claims) && payload.claims.length > 0;
}

function parseMoney(value) {
  if (value == null) return 0;
  if (typeof value === "number") return value;
  const trimmed = String(value).trim();
  if (!trimmed) return 0;
  const normalized = trimmed.replace(/\$/g, "").replace(/USD/gi, "").trim();
  const commaCount = (normalized.match(/,/g) || []).length;
  const dotCount = (normalized.match(/\./g) || []).length;
  let cleaned = normalized;
  if (commaCount === 1 && dotCount === 0) {
    cleaned = normalized.replace(",", ".");
  } else if (commaCount >= 1 && dotCount === 1) {
    cleaned = normalized.replace(/,/g, "");
  } else {
    cleaned = normalized.replace(/,/g, "");
  }
  const parsed = Number(cleaned);
  return Number.isFinite(parsed) ? parsed : 0;
}

function normalizeServiceDate(raw) {
  const trimmed = String(raw || "").trim();
  if (!trimmed) return "Date not recognized";
  const shortYear = trimmed.match(/^(\d{1,2})\/(\d{1,2})\/(\d{2})$/);
  if (shortYear) {
    const month = shortYear[1].padStart(2, "0");
    const day = shortYear[2].padStart(2, "0");
    const yearSuffix = shortYear[3];
    const year = Number(yearSuffix) >= 70 ? `19${yearSuffix}` : `20${yearSuffix}`;
    return `${month}/${day}/${year}`;
  }
  return trimmed;
}

function stringField(source, keys) {
  for (const key of keys) {
    const value = source?.[key];
    if (value != null && String(value).trim()) {
      return String(value).trim();
    }
  }
  return "";
}

function mapField(source, keys) {
  for (const key of keys) {
    const value = source?.[key];
    if (value && typeof value === "object" && !Array.isArray(value)) {
      return value;
    }
  }
  return {};
}

function moneyField(source, keys) {
  for (const key of keys) {
    if (source?.[key] != null) {
      return parseMoney(source[key]);
    }
  }
  return 0;
}

function firstPositive(...values) {
  return values.find((value) => value > 0) || 0;
}

function discoverIndices(line = {}) {
  const indices = new Set();
  Object.keys(line).forEach((key) => {
    const match = key.match(INDEX_SUFFIX);
    if (match) {
      indices.add(Number(match[1]));
    }
  });
  return [...indices].sort((a, b) => a - b);
}

function chargeFromIndexedFields(line, suffix) {
  const code = stringField(line, [`cpt_code${suffix}`, `cptCode${suffix}`, `code${suffix}`])
    .trim()
    .toUpperCase();
  const serviceDate = normalizeServiceDate(
    stringField(line, [`service_date${suffix}`, `date_of_service${suffix}`, `serviceDate${suffix}`])
  );
  const copayAmount = moneyField(line, [`copay_amount${suffix}`, `copay${suffix}`]);
  const deductibleAmount = moneyField(line, [`deductible_amount${suffix}`, `deductible${suffix}`]);
  let coinsuranceAmount = moneyField(line, [`coinsurance_amount${suffix}`, `coinsurance${suffix}`]);
  const patientResponsibility = moneyField(line, [`patient_responsibility${suffix}`]);
  if (patientResponsibility > 0 && copayAmount + deductibleAmount + coinsuranceAmount <= 0) {
    coinsuranceAmount = patientResponsibility;
  }
  return {
    cptCode: code,
    cptDescription: stringField(line, [`service_description${suffix}`, `description${suffix}`]),
    billedAmount: firstPositive(
      moneyField(line, [`total_amount_billed${suffix}`]),
      moneyField(line, [`amount_billed${suffix}`, `billed_amount${suffix}`]),
      moneyField(line, [`allowed_amount${suffix}`])
    ),
    insurancePaidAmount: moneyField(line, [`health_plan_responsibility${suffix}`]),
    contractualAdjustmentAmount: moneyField(line, [`contractual_adjustment${suffix}`]),
    copayAmount,
    deductibleAmount,
    coinsuranceAmount,
    serviceDate
  };
}

function unpivotServiceLine(serviceLine = {}) {
  const indices = discoverIndices(serviceLine);
  if (!indices.length) {
    const singleCode = stringField(serviceLine, ["cpt_code", "cptCode", "code"]);
    if (!singleCode) return [];
    return [chargeFromIndexedFields(serviceLine, "")];
  }
  return indices
    .map((index) => chargeFromIndexedFields(serviceLine, `_${index}`))
    .filter((charge) => charge.cptCode);
}

function claimToFlatMap(claim, context) {
  const totals = mapField(claim, ["claim_totals", "claimTotals"]);
  const providerName = stringField(claim, ["provider_name", "providerName"]);
  const claimId = stringField(claim, [
    "claim_number_1",
    "claim_number_2",
    "claim_number",
    "claim_id",
    "claimId"
  ]);
  const processedDate = normalizeServiceDate(
    stringField(claim, ["processed_date", "processedDate", "date_of_service", "service_date"])
  );
  const serviceLines = Array.isArray(claim.service_lines) ? claim.service_lines : claim.serviceLines;
  const lineMaps = Array.isArray(serviceLines) ? serviceLines : [];
  let charges = lineMaps.flatMap((line) => unpivotServiceLine(line));
  if (!charges.length) {
    charges = unpivotServiceLine(claim);
  }
  if (!charges.length) {
    charges = [{
      cptCode: "",
      cptDescription: "Claim total",
      billedAmount: moneyField(totals, ["total_billed_1", "total_amount_billed_1"]),
      insurancePaidAmount: moneyField(totals, ["total_health_plan_responsibility_1", "health_plan_responsibility"]),
      contractualAdjustmentAmount: moneyField(totals, ["total_contractual_adjustment_1"]),
      copayAmount: 0,
      deductibleAmount: 0,
      coinsuranceAmount: 0,
      serviceDate: processedDate
    }];
  }

  const totalBilled = firstPositive(
    moneyField(totals, ["total_billed_1", "totalBilled1", "total_amount_billed_1"]),
    charges.reduce((sum, charge) => sum + charge.billedAmount, 0)
  );
  const totalInsurancePaid = firstPositive(
    moneyField(totals, ["total_health_plan_responsibility_1", "health_plan_responsibility"]),
    charges.reduce((sum, charge) => sum + charge.insurancePaidAmount, 0)
  );
  const totalContractualAdj = firstPositive(
    moneyField(totals, ["total_contractual_adjustment_1"]),
    charges.reduce((sum, charge) => sum + charge.contractualAdjustmentAmount, 0)
  );
  const totalCopay = charges.reduce((sum, charge) => sum + charge.copayAmount, 0);
  const totalDeductible = charges.reduce((sum, charge) => sum + charge.deductibleAmount, 0);
  const totalCoinsurance = charges.reduce((sum, charge) => sum + charge.coinsuranceAmount, 0);
  const patientResponsibility = firstPositive(
    moneyField(totals, ["total_patient_responsibility_1", "patient_responsibility_1"]),
    totalCopay + totalDeductible + totalCoinsurance
  );
  const primaryServiceDate = charges.find((charge) => charge.serviceDate && charge.serviceDate !== "Date not recognized")?.serviceDate
    || processedDate;

  return {
    provider_name: providerName,
    insurance_name: context.payerName,
    payer_name: context.payerName,
    group_name: context.groupName || context.groupNumber,
    group_number: context.groupNumber,
    member_id: context.subscriberId,
    member_name: context.subscriberName,
    patient_name: context.subscriberName,
    claim_id: claimId,
    benefit_type: context.benefitType,
    date_of_service: primaryServiceDate,
    processed_date: processedDate,
    billed_amount: totalBilled,
    insurance_paid: totalInsurancePaid,
    contractual_adj: totalContractualAdj,
    copay: totalCopay,
    deductible: totalDeductible,
    coinsurance: totalCoinsurance,
    patient_responsibility: patientResponsibility,
    charges,
    cptCodes: charges.map((charge) => charge.cptCode).filter(Boolean).join(",")
  };
}

function translateNestedInsuranceEobPayload(payload = {}, metadata = {}) {
  if (!isNestedInsuranceEobPayload(payload)) return null;
  const context = {
    payerName: stringField(payload, ["payer_name", "payerName", "insurance_name", "insuranceName"]),
    groupName: stringField(payload, ["group_name", "groupName"]),
    groupNumber: stringField(payload, ["group_number", "groupNumber"]),
    subscriberId: stringField(payload, ["subscriber_id", "subscriberId", "member_id", "memberId"]),
    subscriberName: stringField(payload, ["subscriber_name", "subscriberName", "patient_name", "patientName"]),
    benefitType: stringField(payload, ["benefit_type", "benefitType"])
  };
  const claimMaps = payload.claims.map((claim) => claimToFlatMap(claim, context));
  const mergedCharges = claimMaps.flatMap((claim) => claim.charges);
  const providers = [...new Set(claimMaps.map((claim) => claim.provider_name).filter(Boolean))];
  const merged = {
    sourceName: metadata.sourceName || "Veryfi",
    provider_name: providers.join(" / ") || claimMaps[0]?.provider_name || "",
    insurance_name: context.payerName,
    payer_name: context.payerName,
    group_name: context.groupName,
    group_number: context.groupNumber,
    member_id: context.subscriberId,
    member_name: context.subscriberName,
    patient_name: context.subscriberName,
    benefit_type: context.benefitType,
    benefit_year_max_remaining: parseMoney(payload.benefit_year_max_remaining),
    orthodontia_max_remaining: parseMoney(payload.orthodontia_max_remaining),
    date_of_service: mergedCharges.find((charge) => charge.serviceDate)?.serviceDate
      || claimMaps[0]?.date_of_service
      || "Date not recognized",
    billed_amount: claimMaps.reduce((sum, claim) => sum + claim.billed_amount, 0),
    insurance_paid: claimMaps.reduce((sum, claim) => sum + claim.insurance_paid, 0),
    contractual_adj: claimMaps.reduce((sum, claim) => sum + claim.contractual_adj, 0),
    copay: claimMaps.reduce((sum, claim) => sum + claim.copay, 0),
    deductible: claimMaps.reduce((sum, claim) => sum + claim.deductible, 0),
    coinsurance: claimMaps.reduce((sum, claim) => sum + claim.coinsurance, 0),
    patient_responsibility: claimMaps.reduce((sum, claim) => sum + claim.patient_responsibility, 0),
    charges: mergedCharges,
    cptCodes: mergedCharges.map((charge) => charge.cptCode).filter(Boolean).join(","),
    claim_count: claimMaps.length,
    claims: payload.claims,
    insuranceEobTranslated: true
  };
  return {
    flattened: merged,
    claimMaps
  };
}

module.exports = {
  isNestedInsuranceEobPayload,
  parseMoney,
  normalizeServiceDate,
  translateNestedInsuranceEobPayload,
  unpivotServiceLine
};
