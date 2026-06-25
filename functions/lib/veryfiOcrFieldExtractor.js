"use strict";

/**
 * Mirrors Android VeryfiOcrFieldExtractor — PCRE-style regex against ocr_text and
 * custom_fields from Veryfi dashboard Rules.
 */
const AMOUNT_CAPTURE = "(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)";

const FIELD_RULES = [
  {
    key: "billed_amount",
    aliases: new Set(["total_amount_billed", "total_billed"]),
    patterns: [
      new RegExp(`Billed Amount:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Total Billed:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Amount Billed:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Charges:\\s*\\$?${AMOUNT_CAPTURE}`, "i")
    ]
  },
  {
    key: "insurance_paid",
    aliases: new Set(["amount_paid", "plan_paid"]),
    patterns: [
      new RegExp(`Insurance Paid:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Plan Paid:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Payer Paid:\\s*\\$?${AMOUNT_CAPTURE}`, "i")
    ]
  },
  {
    key: "contractual_adj",
    aliases: new Set(["contractual_adjustment", "adjustment"]),
    patterns: [
      new RegExp(`Contractual Adjustment:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Contractual Adj(?:ustment)?\\.?:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Adjustment:\\s*\\$?${AMOUNT_CAPTURE}`, "i")
    ]
  },
  {
    key: "copay",
    aliases: new Set(["co_pay"]),
    patterns: [
      new RegExp(`Copay:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Co-?Pay:\\s*\\$?${AMOUNT_CAPTURE}`, "i")
    ]
  },
  {
    key: "deductible",
    aliases: new Set(),
    patterns: [new RegExp(`Deductible:\\s*\\$?${AMOUNT_CAPTURE}`, "i")]
  },
  {
    key: "coinsurance",
    aliases: new Set(),
    patterns: [
      new RegExp(`Coinsurance:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Co-?Insurance:\\s*\\$?${AMOUNT_CAPTURE}`, "i")
    ]
  },
  {
    key: "patient_responsibility",
    aliases: new Set(["patientResponsibility", "your_responsibility"]),
    patterns: [
      new RegExp(`Patient Responsibility:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Your Responsibility:\\s*\\$?${AMOUNT_CAPTURE}`, "i"),
      new RegExp(`Amount You Owe:\\s*\\$?${AMOUNT_CAPTURE}`, "i")
    ]
  },
  {
    key: "cpt",
    aliases: new Set(["cpt_code", "cpt_codes", "cptCodes"]),
    patterns: [
      /CPT\s*-\s*(\d{5})/i,
      /CPT(?:\s*Code)?\s*[:#-]?\s*(\d{5})/i,
      /HCPCS\s*[:#-]?\s*([A-J]\d{4})/i
    ]
  },
  {
    key: "date_of_service",
    aliases: new Set(["service_date", "dateOfService"]),
    patterns: [
      /Date of Service:\s*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})/i,
      /Service Date:\s*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})/i
    ]
  },
  {
    key: "provider_name",
    aliases: new Set(["provider", "rendering_provider"]),
    patterns: [/(?:Provider|Rendering Provider|Facility):\s*([^\n\r]{2,80})/i]
  },
  {
    key: "insurance_name",
    aliases: new Set(["insurance_company_name", "payer_name", "insurance_company"]),
    patterns: [/(?:Insurance|Payer|Plan)(?:\s+Company)?:\s*([^\n\r]{2,80})/i]
  },
  {
    key: "claim_id",
    aliases: new Set(["claim_number"]),
    patterns: [/Claim(?:\s*(?:ID|Number|#))?:\s*([A-Za-z0-9-]{4,})/i]
  },
  {
    key: "member_id",
    aliases: new Set(["member_number", "subscriber_id"]),
    patterns: [/Member(?:\s*(?:ID|Number|#))?:\s*([A-Za-z0-9-]{4,})/i]
  }
];

function enrichPayload(payload = {}) {
  const enriched = {...payload};
  const ocrText = extractOcrText(enriched);
  const customFields = extractCustomFields(enriched);
  const regexFields = extractFromOcrText(ocrText);

  Object.entries(customFields).forEach(([key, value]) => mergeField(enriched, key, value));
  Object.entries(regexFields).forEach(([key, value]) => mergeField(enriched, key, value));

  if (ocrText && !hasMeaningfulString(enriched, "ocr_text", "ocrText", "text")) {
    enriched.ocr_text = ocrText;
  }
  propagateCptAliases(enriched);
  return enriched;
}

function extractOcrText(payload = {}) {
  return stringValue(payload, ["ocr_text", "ocrText", "text", "raw_text"]);
}

function extractCustomFields(payload = {}) {
  const raw = payload.custom_fields ?? payload.customFields;
  if (!raw) return {};

  if (Array.isArray(raw)) {
    return Object.fromEntries(
      raw.map((item) => {
        if (!item || typeof item !== "object") return null;
        const fieldKey = stringValue(item, ["name", "key", "field", "id"]);
        if (!fieldKey) return null;
        const value = unwrapCustomFieldValue(item.value ?? item.text ?? item.content);
        return value == null ? null : [fieldKey, value];
      }).filter(Boolean)
    );
  }

  if (typeof raw === "object") {
    return Object.fromEntries(
      Object.entries(raw).map(([key, value]) => [key, unwrapCustomFieldValue(value)]).filter(([, value]) => value != null)
    );
  }

  return {};
}

function extractFromOcrText(ocrText) {
  if (!ocrText) return {};
  const extracted = {};
  FIELD_RULES.forEach((rule) => {
    const match = rule.patterns.map((pattern) => {
      const found = pattern.exec(ocrText);
      return found?.[1]?.trim();
    }).find(Boolean);
    if (!match) return;
    if (rule.key === "cpt") {
      extracted.cpt = match.toUpperCase();
    } else if (isAmountRule(rule.key)) {
      const amount = parseAmount(match);
      if (amount != null && amount > 0) extracted[rule.key] = amount;
    } else {
      extracted[rule.key] = match;
    }
  });
  return extracted;
}

function mergeField(target, key, value) {
  if (value == null) return;
  const rule = FIELD_RULES.find((candidate) => candidate.key === key || candidate.aliases.has(key));
  const canonicalKey = rule?.key ?? key;
  if (hasMeaningfulValue(target, canonicalKey, rule?.aliases ?? new Set())) return;

  if (isAmountRule(canonicalKey)) {
    const amount = parseAmount(value);
    if (amount != null && amount > 0) target[canonicalKey] = amount;
    return;
  }
  if (canonicalKey === "cpt") {
    const code = String(value).trim().toUpperCase();
    if (code) target.cpt = code;
    return;
  }
  const text = String(value).trim();
  if (text) target[canonicalKey] = text;
}

function propagateCptAliases(target) {
  const cpt = stringValue(target, ["cpt", "cpt_code", "cpt_codes", "cptCodes"]);
  if (!cpt) return;
  if (!hasMeaningfulString(target, "cpt_codes", "cptCodes", "cpt_code", "cptCode")) {
    target.cpt_codes = cpt;
    target.cptCodes = cpt;
  }
}

function unwrapCustomFieldValue(value) {
  if (value == null) return null;
  if (Array.isArray(value)) return unwrapCustomFieldValue(value[0]);
  if (typeof value === "object") return value.value ?? value.text ?? value.content ?? null;
  return value;
}

function isAmountRule(key) {
  return [
    "billed_amount",
    "insurance_paid",
    "contractual_adj",
    "copay",
    "deductible",
    "coinsurance",
    "patient_responsibility"
  ].includes(key);
}

function parseAmount(value) {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number.parseFloat(value.replace(/[$,]/g, "").trim());
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function hasMeaningfulValue(target, key, aliases) {
  const keys = new Set([key, ...aliases]);
  return [...keys].some((candidate) => {
    const value = target[candidate];
    if (value == null) return false;
    if (typeof value === "number") return value > 0;
    if (typeof value === "string") return value.trim().length > 0;
    return true;
  });
}

function hasMeaningfulString(target, ...keys) {
  return keys.some((key) => String(target[key] ?? "").trim().length > 0);
}

function stringValue(data, keys) {
  for (const key of keys) {
    const value = data[key];
    if (value != null && String(value).trim()) return String(value).trim();
  }
  return "";
}

module.exports = {
  enrichPayload,
  extractOcrText,
  extractCustomFields,
  extractFromOcrText
};
