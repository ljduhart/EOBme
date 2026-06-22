"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  VERYFI_ANY_DOCS_BASE_URL,
  VERYFI_ANY_DOCS_PATH,
  VERYFI_ANY_DOCS_URL,
  BLUEPRINT_HEALTH_INSURANCE_EOB,
  DOCUMENT_TYPE_EOB,
  CATEGORY_INSURANCE,
  CATEGORIES_INSURANCE
} = require("../lib/veryfiAnyDocConstants");

test("AnyDocs URL mirrors Android VeryfiAnyDocConstants", () => {
  assert.equal(VERYFI_ANY_DOCS_BASE_URL, "https://api.veryfi.com/api/v8/");
  assert.equal(VERYFI_ANY_DOCS_PATH, "partner/documents/");
  assert.equal(VERYFI_ANY_DOCS_URL, "https://api.veryfi.com/api/v8/partner/documents/");
});

test("health_insurance_eob blueprint is configured for EOB AnyDocs extraction", () => {
  assert.equal(BLUEPRINT_HEALTH_INSURANCE_EOB, "health_insurance_eob");
});

test("EOB document_type and insurance categories mirror Android AnyDocs contract", () => {
  assert.equal(DOCUMENT_TYPE_EOB, "eob");
  assert.equal(CATEGORY_INSURANCE, "insurance");
  assert.deepEqual(CATEGORIES_INSURANCE, ["insurance"]);
});
