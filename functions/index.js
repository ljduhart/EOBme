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
  VERYFI_ANY_DOCS_URL,
  BLUEPRINT_HEALTH_INSURANCE_EOB
} = require("./lib/veryfiAnyDocConstants");

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
  const match = objectName.match(/^users\/([^/]+)\/eob_uploads\/(.+)$/);
  if (!match) return;

  const userId = match[1];
  const fileName = match[2];
  const bucket = admin.storage().bucket(event.data.bucket);
  const file = bucket.file(objectName);
  const [fileBytes] = await file.download();
  const veryfiResponse = await extractWithVeryfi(fileBytes, {
    fileName,
    contentType: event.data.contentType || "application/octet-stream"
  });
  const storageMetadata = event.data.metadata || {};
  const customMetadata = storageMetadata.metadata || storageMetadata.customMetadata || {};
  const uploadSourceName = customMetadata.sourceName || storageMetadata.sourceName || "Veryfi";
  const normalized = veryfiToEobDocument(veryfiResponse, {
    documentId: fileName.replace(/[^A-Za-z0-9_-]/g, "_"),
    sourceName: uploadSourceName,
    sourceFilePath: objectName
  });
  const docId = String(normalized.id);
  const payload = {
    ...normalized,
    sourceFilePath: objectName,
    processedBy: "veryfi",
    processedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  const userRef = db.collection("users").doc(userId);
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
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Sign in is required for hybrid Veryfi extraction.");
  }
  const {fileBase64, fileName, contentType, blueprintName} = request.data || {};
  if (!fileBase64 || !fileName) {
    throw new HttpsError("invalid-argument", "fileBase64 and fileName are required.");
  }
  const fileBytes = Buffer.from(fileBase64, "base64");
  const veryfiResponse = await extractWithVeryfi(fileBytes, {
    fileName,
    contentType: contentType || "application/octet-stream",
    blueprintName: blueprintName || BLUEPRINT_HEALTH_INSURANCE_EOB
  });
  return {veryfi: veryfiResponse};
});

async function extractWithVeryfi(fileBytes, fileMetadata) {
  const blueprintName = fileMetadata.blueprintName || BLUEPRINT_HEALTH_INSURANCE_EOB;
  const form = new FormData();
  form.append("file", new Blob([fileBytes], {type: fileMetadata.contentType}), fileMetadata.fileName);
  form.append("blueprint_name", blueprintName);

  const response = await fetch(VERYFI_ANY_DOCS_URL, {
    method: "POST",
    headers: {
      "Client-Id": veryfiClientId.value(),
      "Authorization": `apikey ${veryfiUsername.value()}:${veryfiApiKey.value()}`
    },
    body: form
  });

  if (!response.ok) {
    throw new Error(`Veryfi AnyDocs extraction failed with status ${response.status}: ${await response.text()}`);
  }
  return response.json();
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
