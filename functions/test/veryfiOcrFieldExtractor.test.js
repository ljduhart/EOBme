"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  enrichPayload,
  extractFromOcrText,
  extractCustomFields
} = require("../lib/veryfiOcrFieldExtractor");
const {veryfiToEobDocument} = require("../lib/eobNormalizer");

test("extracts billed_amount and cpt from ocr_text", () => {
  const fields = extractFromOcrText("Billed Amount: $240.00 Insurance Paid: $180.00 CPT - 99213");
  assert.equal(fields.billed_amount, 240);
  assert.equal(fields.insurance_paid, 180);
  assert.equal(fields.cpt, "99213");
});

test("prefers custom_fields over ocr regex", () => {
  const enriched = enrichPayload({
    ocr_text: "Billed Amount: $100.00 CPT - 99211",
    custom_fields: {
      billed_amount: {value: "350.00"},
      cpt: {value: "99214"},
      patient_responsibility: {value: "75.00"}
    }
  });
  assert.equal(enriched.billed_amount, 350);
  assert.equal(enriched.cpt, "99214");
  assert.equal(enriched.patient_responsibility, 75);
});

test("veryfiToEobDocument maps OCR-only payload into normalized totals", () => {
  const normalized = veryfiToEobDocument({
    ocr_text: [
      "North Clinic",
      "Cigna",
      "Date of Service: 03/01/2026",
      "Billed Amount: $275.00",
      "Insurance Paid: $180.00",
      "Copay: $20.00",
      "Patient Responsibility: $20.00",
      "CPT - 99213"
    ].join("\n")
  }, {
    documentId: "ocr-only-doc",
    sourceName: "Library upload"
  });

  assert.equal(normalized.totalBilledAmount, 275);
  assert.equal(normalized.totalInsurancePaidAmount, 180);
  assert.equal(normalized.totalCopayAmount, 20);
  assert.equal(normalized.cptCodes, "99213");
  assert.equal(normalized.insuranceName, "Cigna");
});

test("extractCustomFields supports list form", () => {
  const fields = extractCustomFields({
    custom_fields: [
      {name: "billed_amount", value: "425.50"},
      {name: "cpt", value: "99213"}
    ]
  });
  assert.equal(fields.billed_amount, "425.50");
  assert.equal(fields.cpt, "99213");
});
