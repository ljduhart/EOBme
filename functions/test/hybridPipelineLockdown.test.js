"use strict";

const {describe, it} = require("node:test");
const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");

const REPO_ROOT = path.resolve(__dirname, "..", "..");
const MANIFEST_PATH = path.join(REPO_ROOT, "hybrid-pipeline", "HYBRID_PIPELINE_MANIFEST.json");

function sha256File(filePath) {
  const bytes = fs.readFileSync(filePath);
  return crypto.createHash("sha256").update(bytes).digest("hex");
}

function readRepoFile(relativePath) {
  return fs.readFileSync(path.join(REPO_ROOT, relativePath), "utf8");
}

describe("hybridPipelineLockdown", () => {
  it("protected Cloud Functions files match manifest sha256", () => {
    const manifest = JSON.parse(fs.readFileSync(MANIFEST_PATH, "utf8"));
    const cloudEntries = manifest.protectedFiles.filter((entry) => entry.path.startsWith("functions/"));
    assert.ok(cloudEntries.length > 0);
    cloudEntries.forEach((entry) => {
      const absolutePath = path.join(REPO_ROOT, entry.path);
      assert.ok(fs.existsSync(absolutePath), `Missing protected file: ${entry.path}`);
      assert.equal(
        sha256File(absolutePath),
        entry.sha256,
        `Protected file modified without manifest update: ${entry.path}`
      );
    });
  });

  it("extractVeryfiHybridStream remains auth-gated with defineSecret Veryfi credentials", () => {
    const indexSource = readRepoFile("functions/index.js");
    assert.match(indexSource, /defineSecret\("VERYFI_CLIENT_ID"\)/);
    assert.match(indexSource, /defineSecret\("VERYFI_USERNAME"\)/);
    assert.match(indexSource, /defineSecret\("VERYFI_API_KEY"\)/);
    assert.match(indexSource, /exports\.extractVeryfiHybridStream = onCall/);
    assert.match(indexSource, /if \(!request\.auth\?\.uid\)/);
    assert.match(indexSource, /Buffer\.from\(fileBase64, "base64"\)/);
    assert.match(indexSource, /return \{ veryfi: veryfiResponse \}/);
  });

  it("storage trigger and hybrid stream share blueprint-only AnyDocs transport", () => {
    const indexSource = readRepoFile("functions/index.js");
    const clientSource = readRepoFile("functions/lib/veryfiAnyDocClient.js");
    assert.match(indexSource, /exports\.processUploadedEobWithVeryfi = onObjectFinalized/);
    assert.match(indexSource, /BLUEPRINT_HEALTH_INSURANCE_EOB/);
    assert.match(clientSource, /partner\/any-documents/);
    assert.match(clientSource, /document_type and categories are not accepted/);
    assert.doesNotMatch(clientSource, /form\.append\("document_type"/);
    assert.doesNotMatch(clientSource, /form\.append\("categories"/);
  });

  it("hybrid reconciliation skip gates remain intact", () => {
    const reconciliationSource = readRepoFile("functions/lib/hybridReconciliation.js");
    assert.match(reconciliationSource, /shouldSkipStorageVeryfiExtraction/);
    assert.match(reconciliationSource, /client_stream_committed/);
    assert.match(reconciliationSource, /veryfi_hybrid/);
    assert.match(reconciliationSource, /hybridFirestoreDocId/);
  });
});
