"use strict";

function parseCurrency(value) {
  if (value == null) return 0;
  if (typeof value === "number" && Number.isFinite(value)) return value;
  const trimmed = String(value).trim();
  if (!trimmed) return 0;
  const normalized = trimmed
    .replace(/\$/g, "")
    .replace(/USD/gi, "")
    .trim();
  if (!normalized) return 0;
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

function firstPositive(...values) {
  for (const value of values) {
    if (value > 0) return value;
  }
  return 0;
}

module.exports = {
  parseCurrency,
  firstPositive
};
