"use strict";

const admin = require("firebase-admin");
const {onDocumentWritten} = require("firebase-functions/v2/firestore");
const {onObjectFinalized} = require("firebase-functions/v2/storage");
const {defineSecret} = require("firebase-functions/params");
const {
  comparableEobData,
  normalizeEobDocument,
  veryfiToEobDocument
} = require("./lib/eobNormalizer");

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
  secrets: [veryfiClientId, veryfiUsername, veryfiApiKey]
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
  const normalized = veryfiToEobDocument(veryfiResponse, {
    documentId: fileName.replace(/[^A-Za-z0-9_-]/g, "_"),
    sourceName: "Veryfi",
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

async function extractWithVeryfi(fileBytes, fileMetadata) {
  const form = new FormData();
  form.append("file", new Blob([fileBytes], {type: fileMetadata.contentType}), fileMetadata.fileName);
  form.append("parameters", JSON.stringify({
    categories: ["Medical", "Health Insurance", "EOB"],
    tags: ["provider_name", "billed_amount", "insurance_paid", "patient_responsibility", "cpt_codes"],
    auto_delete: false
  }));

  const response = await fetch("https://api.veryfi.com/api/v8/partner/documents", {
    method: "POST",
    headers: {
      "Client-Id": veryfiClientId.value(),
      "Authorization": `apikey ${veryfiUsername.value()}:${veryfiApiKey.value()}`
    },
    body: form
  });

  if (!response.ok) {
    throw new Error(`Veryfi extraction failed with status ${response.status}: ${await response.text()}`);
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
