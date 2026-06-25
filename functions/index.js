"use strict";

const admin = require("firebase-admin");
const {onDocumentWritten} = require("firebase-functions/v2/firestore");
const {onObjectFinalized} = require("firebase-functions/v2/storage");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const {
  comparableEobData,
  normalizeEobDocument,
  veryfiToEobDocument
} = require("./lib/eobNormalizer");
const {
  BLUEPRINT_HEALTH_INSURANCE_EOB
} = require("./lib/veryfiAnyDocConstants");
const {
  shouldSkipStorageVeryfiExtraction,
  storageUploadReconciliationPatch,
  hybridFirestoreDocId
} = require("./lib/hybridReconciliation");
const {
  postAnyDocument,
  postAnyDocumentFromBytes,
  VeryfiApiError,
  VeryfiConfigurationError,
  VeryfiRequestValidationError
} = require("./lib/veryfiAnyDocClient");

admin.initializeApp();

const db = admin.firestore();
const veryfiClientId = defineSecret("VERYFI_CLIENT_ID");
const veryfiUsername = defineSecret("VERYFI_USERNAME");
const veryfiApiKey = defineSecret("VERYFI_API_KEY");

exports.mirrorEobToLegacyRecord = onDocumentWritten("users/{userId}/eobs/{eobId}", async (event) => {
  return mirrorEobWrite(event, "eobs", "eob_records");
});

exports.mirrorLegacyRecordToEob = onDocumentWritten("users/{userId}/eob_records/{eobId}", async (event) => {
  return mirrorEobWrite(event, "eob_records", "eobs");
});

exports.processUploadedEobWithVeryfi = onObjectFinalized({
  secrets: [veryfiClientId, veryfiUsername, veryfiApiKey],
  timeoutSeconds: 120,
  memory: "512MiB"
}, async (event) => {
  const objectName = event.data.name || "";
  const userRootedMatch = objectName.match(/^users\/([^/]+)\/eobs\/(.+)$/);
  const documentRootedMatch = objectName.match(/^eobs\/([^/]+)\/(.+)$/);
  const match = userRootedMatch || documentRootedMatch;
  if (!match) return;

  const userId = match[1];
  const fileName = match[2];
  const bucket = admin.storage().bucket(event.data.bucket);
  const file = bucket.file(objectName);
  const stableDocId = hybridFirestoreDocId(fileName);
  const userRef = db.collection("users").doc(userId);
  const eobRef = userRef.collection("eobs").doc(stableDocId);
  const existingSnapshot = await eobRef.get();
  if (shouldSkipStorageVeryfiExtraction(existingSnapshot.data())) {
    const reconcilePatch = {
      ...storageUploadReconciliationPatch(objectName),
      storageUploadConfirmedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };
    await Promise.all([
      eobRef.set(reconcilePatch, {merge: true}),
      userRef.collection("eob_records").doc(stableDocId).set(reconcilePatch, {merge: true})
    ]);
    return;
  }

  const [fileBytes] = await file.download();
  const veryfiResponse = await postAnyDocumentFromBytes({
    fileBytes,
    fileName,
    blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB,
    externalId: stableDocId,
    credentials: readVeryfiCredentials()
  });
  const storageMetadata = event.data.metadata || {};
  const customMetadata = storageMetadata.metadata || storageMetadata.customMetadata || {};
  const uploadSourceName = customMetadata.sourceName || storageMetadata.sourceName || "Veryfi";
  const normalized = veryfiToEobDocument(veryfiResponse, {
    documentId: fileName.replace(/[^A-Za-z0-9_-]/g, "_"),
    sourceName: uploadSourceName,
    sourceFilePath: objectName
  });
  const docId = stableDocId;
  const payload = {
    ...normalized,
    id: docId,
    sourceFilePath: objectName,
    processedBy: "veryfi",
    processedAt: admin.firestore.FieldValue.serverTimestamp(),
    hybridReconciliationStatus: "storage_trigger_committed",
    processedByStorageTrigger: "veryfi_storage_authority",
    veryfiClientStream: veryfiResponse,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await Promise.all([
    userRef.collection("eobs").doc(docId).set(payload, {merge: true}),
    userRef.collection("eob_records").doc(docId).set(payload, {merge: true})
  ]);
});

exports.extractVeryfiHybridStream = onCall({
  secrets: [veryfiClientId, veryfiUsername, veryfiApiKey],
  timeoutSeconds: 120,
  memory: "512MiB"
}, async (request) => {
  try {
    if (!request.auth?.uid) {
      throw new HttpsError("unauthenticated", "Sign in is required for hybrid Veryfi extraction.");
    }

    const {
      fileBase64,
      fileName,
      blueprintName,
      documentRefId
    } = request.data || {};

    if (!fileBase64 || !fileName) {
      throw new HttpsError("invalid-argument", "fileBase64 and fileName are required.");
    }

    const resolvedBlueprint = String(blueprintName || "").trim() || BLUEPRINT_HEALTH_INSURANCE_EOB;
    if (resolvedBlueprint !== BLUEPRINT_HEALTH_INSURANCE_EOB) {
      throw new HttpsError(
        "invalid-argument",
        `Unsupported blueprint_name '${resolvedBlueprint}'. Expected '${BLUEPRINT_HEALTH_INSURANCE_EOB}'.`
      );
    }

    const veryfiResponse = await postAnyDocument({
      fileDataBase64: fileBase64,
      fileName,
      blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB,
      externalId: documentRefId,
      credentials: readVeryfiCredentials()
    });

    return {veryfi: veryfiResponse};
  } catch (error) {
    throw mapVeryfiHybridStreamError(error);
  }
});

function readVeryfiCredentials() {
  try {
    return {
      clientId: veryfiClientId.value(),
      username: veryfiUsername.value(),
      apiKey: veryfiApiKey.value()
    };
  } catch (error) {
    throw new VeryfiConfigurationError(
      "Veryfi API credentials are not configured for this Firebase project."
    );
  }
}

function mapVeryfiHybridStreamError(error) {
  if (error instanceof HttpsError) {
    return error;
  }
  if (error instanceof VeryfiRequestValidationError) {
    return new HttpsError("invalid-argument", error.message);
  }
  if (error instanceof VeryfiConfigurationError) {
    return new HttpsError("failed-precondition", error.message);
  }
  if (error instanceof VeryfiApiError) {
    if (error.status === 401 || error.status === 403) {
      return new HttpsError("permission-denied", error.message);
    }
    if (error.status === 413) {
      return new HttpsError("invalid-argument", "Document exceeds Veryfi upload size limits.");
    }
    if (error.status === 429) {
      return new HttpsError("resource-exhausted", "Veryfi rate limit reached. Please retry shortly.");
    }
    if (error.status >= 500) {
      return new HttpsError("unavailable", error.message);
    }
    return new HttpsError("invalid-argument", error.message);
  }

  const message = error?.message ? String(error.message) : "Unexpected Veryfi hybrid stream failure.";
  return new HttpsError("internal", message);
}

async function mirrorEobWrite(event, sourceCollection, targetCollection) {
  const {userId, eobId} = event.params;
  const targetRef = db.collection("users").doc(userId).collection(targetCollection).doc(eobId);

  if (!event.data.after.exists) {
    const targetSnapshot = await targetRef.get();
    if (targetSnapshot.exists) {
      await targetRef.delete();
    }
    return;
  }

  const normalized = normalizeEobDocument(event.data.after.data(), eobId);
  const targetSnapshot = await targetRef.get();
  const targetComparable = targetSnapshot.exists ?
    comparableEobData(normalizeEobDocument(targetSnapshot.data(), eobId)) :
    null;
  const normalizedComparable = comparableEobData(normalized);

  if (targetComparable && JSON.stringify(targetComparable) === JSON.stringify(normalizedComparable)) {
    return;
  }

  await targetRef.set({
    ...normalized,
    mirroredFrom: sourceCollection,
    mirroredAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, {merge: true});
}
