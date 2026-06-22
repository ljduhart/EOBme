"use strict";

const {describe, it} = require("node:test");
const assert = require("node:assert/strict");
const {
  shouldSkipStorageVeryfiExtraction,
  storageUploadReconciliationPatch,
  hybridFirestoreDocId
} = require("../lib/hybridReconciliation");

describe("hybridReconciliation", () => {
  it("skips duplicate Veryfi when client stream committed", () => {
    assert.equal(
      shouldSkipStorageVeryfiExtraction({
        processedByClientStream: "veryfi_hybrid",
        hybridReconciliationStatus: "client_stream_committed"
      }),
      true
    );
  });

  it("skips duplicate Veryfi when already reconciled", () => {
    assert.equal(
      shouldSkipStorageVeryfiExtraction({hybridReconciliationStatus: "reconciled"}),
      true
    );
  });

  it("does not skip when no client stream authority exists", () => {
    assert.equal(shouldSkipStorageVeryfiExtraction(null), false);
    assert.equal(shouldSkipStorageVeryfiExtraction({processedBy: "veryfi"}), false);
  });

  it("builds storage upload reconciliation patch", () => {
    const patch = storageUploadReconciliationPatch("users/u1/eobs/file.jpg");
    assert.equal(patch.sourceFilePath, "users/u1/eobs/file.jpg");
    assert.equal(patch.hybridReconciliationStatus, "reconciled");
    assert.equal(patch.processedByStorageTrigger, "skipped_duplicate_veryfi");
  });

  it("derives stable Firestore doc id from upload file name", () => {
    const docId = hybridFirestoreDocId("eob_1718932011000.jpg");
    assert.match(docId, /^\d+$/);
    assert.equal(docId, hybridFirestoreDocId("eob_1718932011000.jpg"));
  });
});
