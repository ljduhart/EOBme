"use strict";

/**
 * Firestore document id shared by client stream and Storage trigger (hash of sanitized file name).
 */
function hybridFirestoreDocId(fileName) {
  const documentRefId = fileName.replace(/[^A-Za-z0-9_-]/g, "_");
  let hash = 0;
  for (let index = 0; index < documentRefId.length; index += 1) {
    hash = ((hash << 5) - hash + documentRefId.charCodeAt(index)) | 0;
  }
  return String(Math.abs(hash) || 1);
}

/**
 * Returns true when the Storage trigger must not re-call Veryfi because the
 * client hybrid stream already committed authoritative extraction data.
 */
function shouldSkipStorageVeryfiExtraction(existingData) {
  if (!existingData || typeof existingData !== "object") {
    return false;
  }
  const clientStream = existingData.processedByClientStream || existingData.processed_by_client_stream;
  const status = existingData.hybridReconciliationStatus || existingData.hybrid_reconciliation_status;
  return (
    clientStream === "veryfi_hybrid" ||
    status === "client_stream_committed" ||
    status === "reconciled"
  );
}

/**
 * Patch applied when Storage upload confirms a client-stream-authoritative document.
 */
function storageUploadReconciliationPatch(objectName) {
  return {
    sourceFilePath: objectName,
    hybridReconciliationStatus: "reconciled",
    processedByStorageTrigger: "skipped_duplicate_veryfi",
    storageUploadConfirmedAt: "SERVER_TIMESTAMP"
  };
}

module.exports = {
  hybridFirestoreDocId,
  shouldSkipStorageVeryfiExtraction,
  storageUploadReconciliationPatch
};
