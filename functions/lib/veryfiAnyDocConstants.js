"use strict";

/** Mirrors Android [VeryfiAnyDocConstants] — single source for EOB documents endpoint wiring. */
const VERYFI_ANY_DOCS_BASE_URL = "https://api.veryfi.com/api/v8/";
const VERYFI_ANY_DOCS_PATH = "partner/documents/";
const VERYFI_ANY_DOCS_URL = `${VERYFI_ANY_DOCS_BASE_URL}${VERYFI_ANY_DOCS_PATH}`;
const BLUEPRINT_HEALTH_INSURANCE_EOB = "health_insurance_eob";
const DOCUMENT_TYPE_EOB = "eob";
const CATEGORY_INSURANCE = "insurance";
const CATEGORIES_INSURANCE = [CATEGORY_INSURANCE];

module.exports = {
  VERYFI_ANY_DOCS_BASE_URL,
  VERYFI_ANY_DOCS_PATH,
  VERYFI_ANY_DOCS_URL,
  BLUEPRINT_HEALTH_INSURANCE_EOB,
  DOCUMENT_TYPE_EOB,
  CATEGORY_INSURANCE,
  CATEGORIES_INSURANCE
};
