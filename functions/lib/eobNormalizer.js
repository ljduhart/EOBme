"use strict";

const VALID_CPT_PATTERN = /^[1-9][0-9]{4}$|^[A-J][0-9]{4}$/;

const CPT_DESCRIPTIONS = {
  "99202": "New patient office visit, straightforward medical decision making.",
  "99203": "New patient office visit, low complexity.",
  "99204": "New patient office visit, moderate complexity.",
  "99205": "New patient office visit, high complexity.",
  "99211": "Established patient office visit, minimal problem.",
  "99212": "Established patient office visit, straightforward medical decision making.",
  "99213": "Established patient office visit, low complexity.",
  "99214": "Established patient office visit, moderate complexity.",
  "99215": "Established patient office visit, high complexity.",
  "80053": "Comprehensive metabolic panel.",
  "80061": "Lipid panel.",
  "83036": "Hemoglobin A1c test.",
  "85025": "Complete blood count with automated differential.",
  "A0425": "Ground mileage for ambulance transport.",
  "A0427": "Advanced life support emergency transport.",
  "A4253": "Blood glucose test strips.",
  "A7030": "Full face CPAP mask.",
  "J1100": "Dexamethasone sodium phosphate injection.",
  "J1885": "Ketorolac tromethamine injection.",
  "J3301": "Triamcinolone acetonide injection.",
  "J3420": "Vitamin B-12 injection."
};

function normalizeEobDocument(data = {}, documentId = "") {
  const serviceDate = normalizeDate(firstValue(data, [
    "serviceDate",
    "dateOfService",
    "date_of_service"
  ]));
  const rawText = stringValue(data, [
    "rawText",
    "raw_text",
    "raw_analysis_text",
    "full_ocr_data",
    "ocrText",
    "ocr_text"
  ]);
  const explicitCharges = Array.isArray(data.charges) ? data.charges : [];
  const charges = explicitCharges.length > 0 ?
    explicitCharges.map((charge) => normalizeCharge(charge, serviceDate)) :
    synthesizeCharges(data, rawText, serviceDate);

  const providerName = stringValue(data, [
    "providerName",
    "provider_name",
    "provider"
  ]) || findProviderName(rawText);
  const insuranceName = stringValue(data, [
    "insuranceName",
    "insurance_name",
    "insurance",
    "payerName",
    "payer_name"
  ]) || findInsuranceName(rawText);

  const totals = totalCharges(charges);

  return {
    id: numberValue(data, ["id"]) || stableId(documentId, providerName, insuranceName, serviceDate, charges),
    sourceName: stringValue(data, ["sourceName", "source_name", "source"]) || "Firebase",
    providerName,
    insuranceName,
    serviceDate,
    serviceDateSortKey: numberValue(data, ["serviceDateSortKey", "service_date_sort_key"]) || serviceDateSortKey(serviceDate),
    rawText,
    charges,
    duplicateChargeWarnings: arrayValue(data, ["duplicateChargeWarnings", "duplicate_charge_warnings"]),
    totalBilledAmount: totals.billedAmount,
    totalInsurancePaidAmount: totals.insurancePaidAmount,
    totalContractualAdjustmentAmount: totals.contractualAdjustmentAmount,
    totalCopayAmount: totals.copayAmount,
    totalDeductibleAmount: totals.deductibleAmount,
    totalCoinsuranceAmount: totals.coinsuranceAmount,
    provider_name: providerName,
    insurance_name: insuranceName,
    date_of_service: serviceDate,
    billed_amount: totals.billedAmount,
    insurance_paid: totals.insurancePaidAmount,
    contractual_adj: totals.contractualAdjustmentAmount,
    copay: totals.copayAmount,
    deductible: totals.deductibleAmount,
    coinsurance: totals.coinsuranceAmount,
    total_amount_billed: totals.billedAmount,
    patient_responsibility: totals.copayAmount + totals.deductibleAmount + totals.coinsuranceAmount,
    cptCodes: charges.map((charge) => charge.cptCode).filter(Boolean).join(",")
  };
}

function veryfiToEobDocument(veryfi = {}, metadata = {}) {
  const rawText = JSON.stringify(veryfi);
  const providerName = stringValue(veryfi, [
    "provider_name",
    "vendor_name"
  ]) || stringValue(veryfi.vendor || {}, ["name"]) || findProviderName(rawText);
  const insuranceName = stringValue(veryfi, [
    "insurance_company_name",
    "insurance_company",
    "insurance_name",
    "payer_name",
    "insurance"
  ]) || findInsuranceName(rawText);
  const dateOfService = normalizeDate(firstValue(veryfi, [
    "date_of_service",
    "service_date",
    "date"
  ]));
  const cptCodes = parseCptCodes([
    veryfi.cptCodes,
    veryfi.cpt_codes,
    JSON.stringify(veryfi.line_items || ""),
    rawText
  ].flat().filter(Boolean).join(" "));

  const normalized = normalizeEobDocument({
    sourceName: metadata.sourceName || "Veryfi",
    providerName,
    insuranceName,
    date_of_service: dateOfService,
    billed_amount: numberValue(veryfi, ["billed_amount", "total_amount_billed", "total", "subtotal"]),
    insurance_paid: numberValue(veryfi, ["insurance_paid", "amount_paid", "payment"]),
    contractual_adj: numberValue(veryfi, ["contractual_adj", "contractual_adjustment", "discount"]),
    copay: numberValue(veryfi, ["copay", "co_pay"]),
    deductible: numberValue(veryfi, ["deductible"]),
    coinsurance: numberValue(veryfi, ["coinsurance"]),
    in_network_out_of_pocket_balance: numberValue(veryfi, [
      "in_network_out_of_pocket_balance",
      "in_network_out_of_pocket"
    ]),
    out_of_network_out_of_pocket_balance: numberValue(veryfi, [
      "out_of_network_out_of_pocket_balance",
      "out_of_network_out_of_pocket"
    ]),
    member_name: stringValue(veryfi, ["member_name"]),
    member_id: stringValue(veryfi, ["member_id", "member_number"]),
    patient_name: stringValue(veryfi, ["patient_name"]),
    claim_id: stringValue(veryfi, ["claim_id", "claim_number"]),
    blueprint_name: stringValue(veryfi, ["blueprint_name"]) || "health_insurance_eob",
    ...(cptCodes.length > 0 ? {cptCodes} : {}),
    rawText,
    sourceFilePath: metadata.sourceFilePath || "",
    veryfiDocumentId: veryfi.id || veryfi.document_id || ""
  }, metadata.documentId || String(veryfi.id || veryfi.document_id || Date.now()));
  return {
    ...normalized,
    sourceFilePath: metadata.sourceFilePath || "",
    veryfiDocumentId: veryfi.id || veryfi.document_id || "",
    blueprint_name: stringValue(veryfi, ["blueprint_name"]) || "health_insurance_eob",
    member_name: stringValue(veryfi, ["member_name"]),
    member_id: stringValue(veryfi, ["member_id", "member_number"]),
    patient_name: stringValue(veryfi, ["patient_name"]),
    claim_id: stringValue(veryfi, ["claim_id", "claim_number"]),
    in_network_out_of_pocket_balance: numberValue(veryfi, [
      "in_network_out_of_pocket_balance",
      "in_network_out_of_pocket"
    ]),
    out_of_network_out_of_pocket_balance: numberValue(veryfi, [
      "out_of_network_out_of_pocket_balance",
      "out_of_network_out_of_pocket"
    ])
  };
}

function normalizeCharge(charge = {}, fallbackDate = "Date not recognized") {
  const cptCode = stringValue(charge, ["cptCode", "cpt_code", "code"]).toUpperCase();
  return {
    cptCode,
    cptDescription: stringValue(charge, ["cptDescription", "cpt_description", "description"]) || cptDescription(cptCode),
    category: stringValue(charge, ["category"]) || cptCategory(cptCode),
    billedAmount: numberValue(charge, ["billedAmount", "billed_amount", "charge"]),
    insurancePaidAmount: numberValue(charge, ["insurancePaidAmount", "insurance_paid", "paid"]),
    contractualAdjustmentAmount: numberValue(charge, [
      "contractualAdjustmentAmount",
      "contractual_adj",
      "contractual_adjustment",
      "adjustment"
    ]),
    copayAmount: numberValue(charge, ["copayAmount", "copay", "co_pay"]),
    deductibleAmount: numberValue(charge, ["deductibleAmount", "deductible"]),
    coinsuranceAmount: numberValue(charge, ["coinsuranceAmount", "coinsurance"]),
    serviceDate: normalizeDate(firstValue(charge, ["serviceDate", "dateOfService", "date_of_service"])) || fallbackDate
  };
}

function synthesizeCharges(data, rawText, serviceDate) {
  const codes = parseCptCodes(firstValue(data, ["cptCodes", "cpt_codes", "cptCode", "cpt_code"]) || rawText);
  if (codes.length === 0) return [];
  const firstCode = codes[0];
  return codes.map((code) => ({
    cptCode: code,
    cptDescription: cptDescription(code),
    category: cptCategory(code),
    billedAmount: code === firstCode ? numberValue(data, ["totalBilledAmount", "billedAmount", "totalAmountBilled", "total_amount_billed", "billed_amount"]) : 0,
    insurancePaidAmount: code === firstCode ? numberValue(data, ["totalInsurancePaidAmount", "insurancePaid", "insurance_paid"]) : 0,
    contractualAdjustmentAmount: code === firstCode ? numberValue(data, ["totalContractualAdjustmentAmount", "contractualAdjustment", "contractual_adj", "contractual_adjustment"]) : 0,
    copayAmount: code === firstCode ? numberValue(data, ["totalCopayAmount", "copayAmount", "copay"]) : 0,
    deductibleAmount: code === firstCode ? numberValue(data, ["totalDeductibleAmount", "deductibleAmount", "deductible"]) : 0,
    coinsuranceAmount: code === firstCode ? numberValue(data, ["totalCoinsuranceAmount", "coinsuranceAmount", "coinsurance"]) : 0,
    serviceDate
  }));
}

function comparableEobData(data) {
  const clone = normalizePlainObject(data);
  delete clone.updatedAt;
  delete clone.mirroredFrom;
  delete clone.mirroredAt;
  return clone;
}

function parseCptCodes(value) {
  const text = Array.isArray(value) ?
    value.map((item) => typeof item === "object" ? JSON.stringify(item) : String(item)).join(" ") :
    String(value || "");
  const matches = text.toUpperCase().match(/(?<![A-Z0-9])(?:[1-9][0-9]{4}|[A-J][0-9]{4})(?![A-Z0-9])/g) || [];
  return [...new Set(matches.filter((code) => VALID_CPT_PATTERN.test(code)))];
}

function totalCharges(charges) {
  return charges.reduce((totals, charge) => ({
    billedAmount: totals.billedAmount + numberValue(charge, ["billedAmount"]),
    insurancePaidAmount: totals.insurancePaidAmount + numberValue(charge, ["insurancePaidAmount"]),
    contractualAdjustmentAmount: totals.contractualAdjustmentAmount + numberValue(charge, ["contractualAdjustmentAmount"]),
    copayAmount: totals.copayAmount + numberValue(charge, ["copayAmount"]),
    deductibleAmount: totals.deductibleAmount + numberValue(charge, ["deductibleAmount"]),
    coinsuranceAmount: totals.coinsuranceAmount + numberValue(charge, ["coinsuranceAmount"])
  }), {
    billedAmount: 0,
    insurancePaidAmount: 0,
    contractualAdjustmentAmount: 0,
    copayAmount: 0,
    deductibleAmount: 0,
    coinsuranceAmount: 0
  });
}

function findProviderName(rawText) {
  const match = String(rawText || "").match(/\b(provider|facility|doctor|physician)\b\s*[:#-]?\s*([^\n]+)/i);
  return match ? match[2].trim().slice(0, 80) : "Provider not recognized";
}

function findInsuranceName(rawText) {
  const insurers = ["United Healthcare", "UnitedHealthcare", "Blue Cross BlueShield", "Blue Cross Blue Shield", "Aetna", "Cigna", "Humana", "BCBS", "Anthem", "Kaiser Permanente", "Medicare", "Medicaid"];
  const lower = String(rawText || "").toLowerCase();
  return insurers.find((name) => lower.includes(name.toLowerCase())) || "Insurance not recognized";
}

function cptDescription(code) {
  return CPT_DESCRIPTIONS[code] || "CPT/HCPCS code recognized; review payer-specific details.";
}

function cptCategory(code) {
  if (/^99/.test(code)) return "OfficeVisit";
  if (/^(8|36)/.test(code)) return "Lab";
  if (/^(4|7)/.test(code)) return "Hospital";
  if (/^A/.test(code)) return "Dme";
  if (/^J/.test(code)) return "Injection";
  return "Other";
}

function normalizeDate(value) {
  if (!value) return "Date not recognized";
  if (typeof value.toDate === "function") {
    return formatDate(value.toDate());
  }
  if (value instanceof Date) {
    return formatDate(value);
  }
  if (typeof value === "number") {
    return formatDate(new Date(value));
  }
  const raw = String(value).trim();
  const iso = raw.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
  if (iso) return `${iso[2].padStart(2, "0")}/${iso[3].padStart(2, "0")}/${iso[1]}`;
  return raw || "Date not recognized";
}

function serviceDateSortKey(date) {
  const parts = String(date || "").split("/");
  if (parts.length !== 3) return Number.MAX_SAFE_INTEGER;
  const [month, day, year] = parts.map((part) => Number.parseInt(part, 10));
  if (!month || !day || !year) return Number.MAX_SAFE_INTEGER;
  return year * 10000 + month * 100 + day;
}

function formatDate(date) {
  const month = String(date.getUTCMonth() + 1).padStart(2, "0");
  const day = String(date.getUTCDate()).padStart(2, "0");
  return `${month}/${day}/${date.getUTCFullYear()}`;
}

function stableId(documentId, providerName, insuranceName, serviceDate, charges) {
  const base = documentId || [providerName, insuranceName, serviceDate, charges.map((charge) => charge.cptCode).join(",")].join("|");
  let hash = 0;
  for (let index = 0; index < base.length; index += 1) {
    hash = ((hash << 5) - hash + base.charCodeAt(index)) | 0;
  }
  return Math.abs(hash) || 1;
}

function firstValue(data, keys) {
  return keys.map((key) => data[key]).find((value) => value !== undefined && value !== null && value !== "");
}

function stringValue(data, keys) {
  const value = firstValue(data, keys);
  return value === undefined || value === null ? "" : String(value).trim();
}

function numberValue(data, keys) {
  const value = firstValue(data, keys);
  if (typeof value === "number") return value;
  if (typeof value === "string") {
    const parsed = Number.parseFloat(value.replace(/[$,]/g, ""));
    return Number.isFinite(parsed) ? parsed : 0;
  }
  return 0;
}

function arrayValue(data, keys) {
  const value = firstValue(data, keys);
  return Array.isArray(value) ? value : [];
}

function normalizePlainObject(value) {
  if (Array.isArray(value)) return value.map(normalizePlainObject);
  if (!value || typeof value !== "object") return value;
  return Object.keys(value).sort().reduce((accumulator, key) => {
    accumulator[key] = normalizePlainObject(value[key]);
    return accumulator;
  }, {});
}

module.exports = {
  comparableEobData,
  normalizeEobDocument,
  veryfiToEobDocument,
  parseCptCodes,
  serviceDateSortKey
};
