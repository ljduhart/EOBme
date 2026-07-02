"use strict";

const {describe, it} = require("node:test");
const assert = require("node:assert/strict");
const {parseCurrency} = require("../lib/veryfiCurrencyParser");
const {toIsoDate} = require("../lib/veryfiDateNormalizer");
const {
  isNestedClaimsPayload,
  nestedClaimsToEobDocument
} = require("../lib/veryfiInsuranceEobNormalizer");

function healthTexasInsuranceEobPayload() {
  return {
    group_name: "HEALTHTEXAS MEDICAL GROUP",
    payer_name: "BlueCross BlueShield of Texas",
    benefit_type: "Dental",
    group_number: "329372",
    subscriber_id: "829723024",
    subscriber_name: "DUHART,LESTER J",
    claims: [
      {
        provider_name: "ALAMO FAMILY & COSMETIC D",
        claim_number_1: "0020260831991150500000",
        processed_date: "03/24/26",
        claim_totals: {
          total_billed_1: "$ 1,578.00",
          patient_responsibility_1: 473.35,
          total_health_plan_responsibility_1: "$ 511,42"
        },
        service_lines: [
          {
            cpt_code_1: "D5225",
            cpt_code_2: "D0120",
            cpt_code_3: "D1110 ",
            cpt_code_4: "99213",
            cpt_code_5: "99214",
            cpt_code_6: "36415",
            cpt_code_7: "81003",
            cpt_code_8: "99395",
            service_date_1: "03/21/26",
            service_date_2: "04/11/26 ",
            service_date_3: " 04/11/26",
            amount_billed_1: 1,
            amount_billed_2: 45,
            amount_billed_3: 95,
            total_amount_billed_1: "$ 1,438.00",
            total_amount_billed_2: "$ 140.00",
            health_plan_responsibility_1: 423.35,
            health_plan_responsibility_2: 30.49,
            health_plan_responsibility_3: 57.58,
            patient_responsibility_1: 473.35,
            service_description_1: "Maxillary Partial Denture - Flexible Bas"
          }
        ]
      }
    ]
  };
}

describe("veryfiInsuranceEobNormalizer", () => {
  it("detects nested claims payloads", () => {
    assert.equal(isNestedClaimsPayload(healthTexasInsuranceEobPayload()), true);
    assert.equal(isNestedClaimsPayload({Claims: [{provider_name: "Clinic"}]}), true);
    assert.equal(isNestedClaimsPayload({provider_name: "Clinic"}), false);
  });

  it("maps health_insurance_eob nested claims into charge rows", () => {
    const normalized = nestedClaimsToEobDocument(healthTexasInsuranceEobPayload(), {
      documentId: "eob_123_jpg",
      sourceName: "Camera",
      sourceFilePath: "users/u1/eobs/eob_123_jpg.jpg"
    });

    assert.equal(normalized.insuranceName, "BlueCross BlueShield of Texas");
    assert.equal(normalized.providerName, "ALAMO FAMILY & COSMETIC D");
    assert.equal(normalized.charges.length, 8);
    assert.equal(normalized.charges[0].cptCode, "D5225");
    assert.equal(normalized.charges[1].cptCode, "D0120");
    assert.equal(normalized.charges[3].cptCode, "99213");
    assert.equal(normalized.charges[0].serviceDate, "03/21/2026");
    assert.equal(normalized.charges[1].serviceDate, "04/11/2026");
    assert.ok(normalized.totalBilledAmount > 0);
    assert.ok(normalized.patient_responsibility > 0);
    assert.equal(normalized.blueprint_name, "health_insurance_eob");
  });

  it("parses european currency commas", () => {
    assert.equal(parseCurrency("$ 511,42"), 511.42);
    assert.equal(parseCurrency("$ 1,578.00"), 1578);
  });

  it("normalizes service dates to ISO", () => {
    assert.equal(toIsoDate("03/24/26"), "2026-03-24");
    assert.equal(toIsoDate(" 04/11/26 "), "2026-04-11");
  });
});
