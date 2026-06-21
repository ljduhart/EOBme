"use strict";

/** Mirrors Android [VeryfiAnyDocConstants] — single source for AnyDocs EOB endpoint wiring. */
const VERYFI_ANY_DOCS_BASE_URL = "https://api.veryfi.com/api/v8/";
const VERYFI_ANY_DOCS_PATH = "partner/any-documents/";
const VERYFI_ANY_DOCS_URL = `${VERYFI_ANY_DOCS_BASE_URL}${VERYFI_ANY_DOCS_PATH}`;
const BLUEPRINT_HEALTH_INSURANCE_EOB = "health_insurance_eob";

module.exports = {
  VERYFI_ANY_DOCS_BASE_URL,
  VERYFI_ANY_DOCS_PATH,
  VERYFI_ANY_DOCS_URL,
  BLUEPRINT_HEALTH_INSURANCE_EOB
};
