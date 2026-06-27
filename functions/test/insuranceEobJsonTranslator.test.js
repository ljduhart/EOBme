"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  isNestedInsuranceEobPayload,
  parseMoney,
  normalizeServiceDate,
  translateNestedInsuranceEobPayload
} = require("../lib/insuranceEobJsonTranslator");
const {veryfiToEobDocument} = require("../lib/eobNormalizer");

const samplePayload = {
  group_name: "HEALTHTEXAS MEDICAL GROUP",
  payer_name: "BlueCross BlueShield of Texas",
  benefit_type: "Dental",
  subscriber_id: "829723024",
  claims: [{
    provider_name: "ALAMO FAMILY & COSMETIC D",
    claim_number_1: "0020260831991150500000",
    processed_date: "03/24/26",
    claim_totals: {
      total_billed_1: "$ 1,578.00",
      total_patient_responsibility_1: 473.35
    },
    service_lines: [{
      cpt_code_1: "D5225",
      cpt_code_2: "D0120",
      service_date_1: "03/21/26",
      service_date_2: "04/11/26",
      amount_billed_1: 1,
      total_amount_billed_1: "$ 1,438.00",
      patient_responsibility_1: 473.35,
      health_plan_responsibility_1: 423.35,
      contractual_adjustment_1: 541.3,
      amount_billed_2: 45,
      contractual_adjustment_2: 14.51,
      health_plan_responsibility_2: 30.49
    }]
  }]
};

test("isNestedInsuranceEobPayload detects claims array", () => {
  assert.equal(isNestedInsuranceEobPayload(samplePayload), true);
  assert.equal(isNestedInsuranceEobPayload({provider_name: "Test"}), false);
});

test("parseMoney handles US and European currency strings", () => {
  assert.equal(parseMoney("$ 1,578.00"), 1578);
  assert.equal(parseMoney("$ 511,42"), 511.42);
});

test("normalizeServiceDate expands two digit year", () => {
  assert.equal(normalizeServiceDate("03/24/26"), "03/24/2026");
});

test("translateNestedInsuranceEobPayload unpivots numbered service line columns", () => {
  const translated = translateNestedInsuranceEobPayload(samplePayload, {sourceName: "Veryfi"});
  assert.ok(translated);
  assert.equal(translated.flattened.charges.length, 2);
  assert.equal(translated.flattened.charges[0].cptCode, "D5225");
  assert.equal(translated.flattened.charges[1].cptCode, "D0120");
  assert.equal(translated.flattened.insurance_name, "BlueCross BlueShield of Texas");
  assert.ok(translated.flattened.billed_amount > 0);
});

test("veryfiToEobDocument maps nested insurance EOB claims into normalized EOB", () => {
  const normalized = veryfiToEobDocument(samplePayload, {
    documentId: "eob_123",
    sourceName: "Veryfi"
  });
  assert.equal(normalized.insuranceName, "BlueCross BlueShield of Texas");
  assert.equal(normalized.providerName, "ALAMO FAMILY & COSMETIC D");
  assert.ok(normalized.charges.length >= 2);
  assert.ok(normalized.totalBilledAmount > 0);
});
