"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const Client = require("@veryfi/veryfi-sdk/lib/client/constructor");
require("@veryfi/veryfi-sdk/lib/client/generateSignature");
const {
  generateVeryfiRequestSignature,
  buildVeryfiSignedHeaders
} = require("../lib/veryfiRequestSignature");
const {
  buildAnyDocRequestBody,
  buildAnyDocRequestBodyFromUrl,
  formatFileDataForVeryfi,
  normalizeBase64Payload,
  resolveBlueprintName,
  VeryfiRequestValidationError
} = require("../lib/veryfiAnyDocClient");
const {BLUEPRINT_HEALTH_INSURANCE_EOB, VERYFI_ANY_DOCS_URL} = require("../lib/veryfiAnyDocConstants");

test("normalizeBase64Payload strips data URI prefix", () => {
  const payload = normalizeBase64Payload("data:image/jpeg;base64,QUJD");
  assert.equal(payload, "QUJD");
});

test("buildAnyDocRequestBody posts blueprint_name health_insurance_eob with data URI file_data", () => {
  const body = buildAnyDocRequestBody({
    fileDataBase64: Buffer.from("a".repeat(300)).toString("base64"),
    fileName: "eob_123.jpg",
    blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB,
    externalId: "eob_123_jpg",
    contentType: "image/jpeg"
  });
  assert.equal(body.blueprint_name, "health_insurance_eob");
  assert.equal(body.file_name, "eob_123.jpg");
  assert.equal(body.external_id, "eob_123_jpg");
  assert.match(body.file_data, /^data:image\/jpeg;base64,/);
});

test("buildAnyDocRequestBodyFromUrl posts file_url and blueprint_name", () => {
  const body = buildAnyDocRequestBodyFromUrl({
    fileUrl: "https://firebasestorage.googleapis.com/v0/b/demo/o/eob.jpg?alt=media&token=abc",
    blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB,
    externalId: "eob_123_jpg"
  });
  assert.equal(body.file_url, "https://firebasestorage.googleapis.com/v0/b/demo/o/eob.jpg?alt=media&token=abc");
  assert.equal(body.blueprint_name, "health_insurance_eob");
  assert.equal(body.external_id, "eob_123_jpg");
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

test("generateVeryfiRequestSignature matches official Veryfi SDK", () => {
  const client = new Client("demo-client", "demo-secret", "demo-user", "demo-key");
  const payload = {
    file_url: "https://example.com/document.pdf",
    blueprint_name: "health_insurance_eob"
  };
  const timestamp = 1710000000000;
  const sdkSignature = client._generate_signature(payload, timestamp);
  const localSignature = generateVeryfiRequestSignature("demo-secret", payload, timestamp);
  assert.equal(localSignature, sdkSignature);
});

test("buildVeryfiSignedHeaders includes Client-Id, Authorization, and signature headers", () => {
  const body = buildAnyDocRequestBodyFromUrl({
    fileUrl: "https://example.com/document.pdf",
    blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB
  });
  const headers = buildVeryfiSignedHeaders(body, {
    clientId: "demo-client",
    clientSecret: "demo-secret",
    username: "demo-user",
    apiKey: "demo-key"
  });
  assert.equal(headers["Client-Id"], "demo-client");
  assert.equal(headers.Authorization, "apikey demo-user:demo-key");
  assert.ok(headers["X-Veryfi-Request-Timestamp"]);
  assert.ok(headers["X-Veryfi-Request-Signature"]);
});

test("formatFileDataForVeryfi wraps base64 in data URI", () => {
  assert.equal(
    formatFileDataForVeryfi("QUJD", "image/png"),
    "data:image/png;base64,QUJD"
  );
});
