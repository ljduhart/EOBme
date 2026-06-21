"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  comparableEobData,
  normalizeEobDocument,
  parseCptCodes,
  veryfiToEobDocument
} = require("../lib/eobNormalizer");

test("normalizes legacy eob_records fields into app-ready EOB data", () => {
  const normalized = normalizeEobDocument({
    provider_name: "Downtown Medical Group",
    insurance_name: "Aetna",
    date_of_service: "2026-02-03",
    billed_amount: "$310.00",
    insurance_paid: "$130.00",
    contractual_adj: "$105.00",
    copay: 25,
    deductible: 40,
    coinsurance: 10,
    cptCodes: "99215, 80053",
    rawText: "Aetna Provider: Downtown Medical Group 99215 80053"
  }, "legacy-doc");

  assert.equal(normalized.providerName, "Downtown Medical Group");
  assert.equal(normalized.insuranceName, "Aetna");
  assert.equal(normalized.serviceDate, "02/03/2026");
  assert.equal(normalized.totalBilledAmount, 310);
  assert.equal(normalized.totalInsurancePaidAmount, 130);
  assert.equal(normalized.totalContractualAdjustmentAmount, 105);
  assert.equal(normalized.patient_responsibility, 75);
  assert.deepEqual(normalized.charges.map((charge) => charge.cptCode), ["99215", "80053"]);
});

test("normalizes nested charge documents and mirrors snake_case fields", () => {
  const normalized = normalizeEobDocument({
    providerName: "Lakeside Clinic",
    insuranceName: "UnitedHealthcare",
    serviceDate: "03/05/2026",
    charges: [
      {
        cptCode: "99214",
        billedAmount: 200,
        insurancePaidAmount: 80,
        contractualAdjustmentAmount: 60,
        copayAmount: 20,
        deductibleAmount: 30,
        coinsuranceAmount: 10
      }
    ]
  }, "nested-doc");

  assert.equal(normalized.billed_amount, 200);
  assert.equal(normalized.insurance_paid, 80);
  assert.equal(normalized.contractual_adj, 60);
  assert.equal(normalized.cptCodes, "99214");
});

test("parses only valid CPT and HCPCS codes", () => {
  assert.deepEqual(parseCptCodes("99215 01234 Z9999 A0425 J3301 123456"), ["99215", "A0425", "J3301"]);
});

test("maps Veryfi AnyDocs health_insurance_eob payload into normalized EOB fields", () => {
  const normalized = veryfiToEobDocument({
    id: "anydoc-1",
    blueprint_name: "health_insurance_eob",
    insurance_company_name: "Cigna",
    member_name: "Alex Patient",
    member_id: "MEM-7788",
    patient_name: "Alex Patient",
    claim_id: "CLM-2200",
    provider_name: "North Clinic",
    date_of_service: "2026-03-01",
    in_network_out_of_pocket_balance: 900,
    out_of_network_out_of_pocket_balance: 2100,
    billed_amount: 275,
    insurance_paid: 180,
    copay: 20
  }, {
    documentId: "storage-file-2",
    sourceFilePath: "users/u1/eob_uploads/storage-file-2.jpg"
  });

  assert.equal(normalized.insuranceName, "Cigna");
  assert.equal(normalized.member_name, "Alex Patient");
  assert.equal(normalized.member_id, "MEM-7788");
  assert.equal(normalized.patient_name, "Alex Patient");
  assert.equal(normalized.claim_id, "CLM-2200");
  assert.equal(normalized.blueprint_name, "health_insurance_eob");
  assert.equal(normalized.in_network_out_of_pocket_balance, 900);
  assert.equal(normalized.out_of_network_out_of_pocket_balance, 2100);
});

test("maps legacy Veryfi invoice payload into normalized EOB fields", () => {
  const normalized = veryfiToEobDocument({
    id: "veryfi-doc-1",
    vendor: {name: "Downtown Medical Group"},
    insurance_name: "Aetna",
    date_of_service: "2026-02-03",
    total: 300,
    insurance_paid: 125,
    contractual_adj: 100,
    copay: 30,
    deductible: 25,
    coinsurance: 20,
    line_items: [{description: "99215 office visit"}]
  }, {
    documentId: "storage-file-1",
    sourceFilePath: "users/u1/eob_uploads/storage-file-1.jpg"
  });

  assert.equal(normalized.providerName, "Downtown Medical Group");
  assert.equal(normalized.insuranceName, "Aetna");
  assert.equal(normalized.serviceDate, "02/03/2026");
  assert.equal(normalized.totalBilledAmount, 300);
  assert.equal(normalized.cptCodes, "99215");
  assert.equal(normalized.sourceFilePath, "users/u1/eob_uploads/storage-file-1.jpg");
});

test("comparison ignores sync-only fields to prevent mirror loops", () => {
  const base = normalizeEobDocument({
    provider_name: "Downtown Medical Group",
    insurance_name: "Aetna",
    date_of_service: "2026-02-03",
    billed_amount: 310,
    cptCodes: "99215"
  }, "same-doc");
  const mirrored = {
    ...base,
    mirroredFrom: "eobs",
    updatedAt: "server timestamp",
    mirroredAt: "server timestamp"
  };

  assert.deepEqual(comparableEobData(base), comparableEobData(mirrored));
});
