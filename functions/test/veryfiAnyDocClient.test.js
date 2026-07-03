"use strict";

const {describe, it, beforeEach, afterEach} = require("node:test");
const assert = require("node:assert/strict");
const {
  extractWithVeryfiJson,
  extractWithVeryfiMultipart
} = require("../lib/veryfiAnyDocClient");
const {BLUEPRINT_HEALTH_INSURANCE_EOB, VERYFI_ANY_DOCS_URL} = require("../lib/veryfiAnyDocConstants");

describe("veryfiAnyDocClient", () => {
  let originalFetch;

  beforeEach(() => {
    originalFetch = global.fetch;
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it("multipart sends only file and blueprint_name to AnyDocs", async () => {
    let capturedUrl = "";
    let capturedInit = null;
    global.fetch = async (url, init) => {
      capturedUrl = url;
      capturedInit = init;
      return {
        ok: true,
        json: async () => ({blueprint_name: BLUEPRINT_HEALTH_INSURANCE_EOB})
      };
    };

    await extractWithVeryfiMultipart({
      fileBytes: Buffer.from("jpeg-bytes"),
      fileName: "eob_123.jpg",
      contentType: "image/jpeg",
      blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB,
      clientId: "client-id",
      username: "username",
      apiKey: "api-key"
    });

    assert.equal(capturedUrl, VERYFI_ANY_DOCS_URL);
    const formKeys = [];
    for (const [key] of capturedInit.body) {
      formKeys.push(key);
    }
    assert.deepEqual(formKeys.sort(), ["blueprint_name", "file"]);
    assert.equal(capturedInit.body.get("blueprint_name"), BLUEPRINT_HEALTH_INSURANCE_EOB);
  });

  it("json sends file_url and blueprint_name only to AnyDocs", async () => {
    let capturedBody = null;
    global.fetch = async (_url, init) => {
      capturedBody = JSON.parse(init.body);
      return {
        ok: true,
        json: async () => ({blueprint_name: BLUEPRINT_HEALTH_INSURANCE_EOB})
      };
    };

    await extractWithVeryfiJson({
      fileUrl: "https://storage.example/eob.jpg",
      fileName: "eob_123.jpg",
      blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB,
      clientId: "client-id",
      username: "username",
      apiKey: "api-key"
    });

    assert.deepEqual(capturedBody, {
      file_url: "https://storage.example/eob.jpg",
      blueprint_name: BLUEPRINT_HEALTH_INSURANCE_EOB,
      file_name: "eob_123.jpg"
    });
    assert.equal(capturedBody.document_type, undefined);
    assert.equal(capturedBody.categories, undefined);
  });
});
