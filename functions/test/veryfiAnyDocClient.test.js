"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  buildAnyDocRequestBody,
  normalizeBase64Payload,
  resolveBlueprintName,
  VeryfiRequestValidationError
} = require("../lib/veryfiAnyDocClient");
const {BLUEPRINT_HEALTH_INSURANCE_EOB, VERYFI_ANY_DOCS_URL} = require("../lib/veryfiAnyDocConstants");

test("normalizeBase64Payload strips data URI prefix", () => {
  const payload = normalizeBase64Payload("data:image/jpeg;base64,QUJD");
  assert.equal(payload, "QUJD");
});

test("buildAnyDocRequestBody posts blueprint_name health_insurance_eob to any-documents", () => {
  const body = buildAnyDocRequestBody({
    fileDataBase64: Buffer.from("a".repeat(300)).toString("base64"),
    fileName: "eob_123.jpg",
    blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB,
    externalId: "eob_123_jpg"
  });
  assert.equal(body.blueprint_name, "health_insurance_eob");
  assert.equal(body.file_name, "eob_123.jpg");
  assert.equal(body.external_id, "eob_123_jpg");
  assert.ok(body.file_data);
});

test("resolveBlueprintName defaults to health_insurance_eob", () => {
  assert.equal(resolveBlueprintName(""), "health_insurance_eob");
  assert.equal(resolveBlueprintName("health_insurance_eob"), "health_insurance_eob");
});

test("buildAnyDocRequestBody rejects undersized documents", () => {
  assert.throws(
    () => buildAnyDocRequestBody({
      fileDataBase64: Buffer.from("tiny").toString("base64"),
      fileName: "eob.jpg",
      blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB
    }),
    VeryfiRequestValidationError
  );
});

test("any-documents URL targets partner any-documents endpoint", () => {
  assert.equal(VERYFI_ANY_DOCS_URL, "https://api.veryfi.com/api/v8/partner/any-documents/");
});
