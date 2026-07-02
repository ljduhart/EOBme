"use strict";

const admin = require("firebase-admin");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onObjectFinalized } = require("firebase-functions/v2/storage");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const nodemailer = require("nodemailer");

const {
  comparableEobData,
  normalizeEobDocument,
  veryfiToEobDocument
} = require("./lib/eobNormalizer");
const {
  BLUEPRINT_HEALTH_INSURANCE_EOB,
  DOCUMENT_TYPE_EOB,
  CATEGORIES_INSURANCE
} = require("./lib/veryfiAnyDocConstants");
const {
  shouldSkipStorageVeryfiExtraction,
  storageUploadReconciliationPatch,
  hybridFirestoreDocId
} = require("./lib/hybridReconciliation");
const {extractWithVeryfi} = require("./lib/veryfiAnyDocClient");

admin.initializeApp();

const db = admin.firestore();

// ----------------------------------------------------------------------
// Secrets Declaration
// ----------------------------------------------------------------------
const veryfiClientId = defineSecret("VERYFI_CLIENT_ID");
const veryfiUsername = defineSecret("VERYFI_USERNAME");
const veryfiApiKey = defineSecret("VERYFI_API_KEY");

const smtpHost = defineSecret("AUTH_SMTP_HOST");
const smtpUser = defineSecret("AUTH_SMTP_USER");
const smtpPass = defineSecret("AUTH_SMTP_PASS");
const smtpFrom = defineSecret("AUTH_SMTP_FROM");

// ----------------------------------------------------------------------
// Global Connection Pooling
// ----------------------------------------------------------------------
let mailTransporter = null;

// ----------------------------------------------------------------------
// Firestore Mirroring Triggers
// ----------------------------------------------------------------------
exports.mirrorEobToLegacyRecord = onDocumentWritten("users/{userId}/eobs/{eobId}", async (event) => {
  return mirrorEobWrite(event, "eobs", "eob_records");
});

exports.mirrorLegacyRecordToEob = onDocumentWritten("users/{userId}/eob_records/{eobId}", async (event) => {
  return mirrorEobWrite(event, "eob_records", "eobs");
});

async function mirrorEobWrite(event, sourceCollection, targetCollection) {
  const { userId, eobId } = event.params;
  const targetRef = db.collection("users").doc(userId).collection(targetCollection).doc(eobId);

  // Handle Deletions
  if (!event.data.after.exists) {
    const targetSnapshot = await targetRef.get();
    if (targetSnapshot.exists) {
      await targetRef.delete();
    }
    return null;
  }

  const afterData = event.data.after.data();

  // CRITICAL: Architectural short-circuit to prevent infinite execution loops.
  // If the document we are reacting to was written BY the target collection, drop it immediately.
  if (afterData.mirroredFrom === targetCollection) {
    return null;
  }

  const normalized = normalizeEobDocument(afterData, eobId);
  const targetSnapshot = await targetRef.get();

  const targetComparable = targetSnapshot.exists ?
    comparableEobData(normalizeEobDocument(targetSnapshot.data(), eobId)) :
    null;
  const normalizedComparable = comparableEobData(normalized);

  // Fallback structural comparison
  if (targetComparable && JSON.stringify(targetComparable) === JSON.stringify(normalizedComparable)) {
    return null;
  }

  await targetRef.set({
    ...normalized,
    mirroredFrom: sourceCollection,
    mirroredAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  return null;
}

// ----------------------------------------------------------------------
// Veryfi Extraction Capabilities
// ----------------------------------------------------------------------
exports.processUploadedEobWithVeryfi = onObjectFinalized({
  secrets: [veryfiClientId, veryfiUsername, veryfiApiKey],
  timeoutSeconds: 120,
  memory: "512MiB"
}, async (event) => {
  const objectName = event.data.name || "";
  const userRootedMatch = objectName.match(/^users\/([^/]+)\/eobs\/(.+)$/);
  const documentRootedMatch = objectName.match(/^eobs\/([^/]+)\/(.+)$/);
  const match = userRootedMatch || documentRootedMatch;
  if (!match) return null;

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
      eobRef.set(reconcilePatch, { merge: true }),
      userRef.collection("eob_records").doc(stableDocId).set(reconcilePatch, { merge: true })
    ]);
    return null;
  }

  const [fileBytes] = await file.download();
  let fileUrl = "";
  try {
    const [signedUrl] = await file.getSignedUrl({
      action: "read",
      expires: Date.now() + 15 * 60 * 1000
    });
    fileUrl = signedUrl;
  } catch (signedUrlError) {
    console.warn("Veryfi storage trigger could not mint signed URL; using multipart bytes.", signedUrlError);
  }

  const veryfiResponse = await extractWithVeryfi(fileBytes, {
    fileName,
    contentType: event.data.contentType || "application/octet-stream",
    blueprintName: BLUEPRINT_HEALTH_INSURANCE_EOB,
    documentType: DOCUMENT_TYPE_EOB,
    categories: CATEGORIES_INSURANCE,
    fileUrl
  }, {
    clientId: veryfiClientId.value(),
    username: veryfiUsername.value(),
    apiKey: veryfiApiKey.value()
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
    hybridReconciliationStatus: "storage_trigger_committed",
    processedByStorageTrigger: "veryfi_storage_authority",
    veryfiClientStream: veryfiResponse,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await Promise.all([
    userRef.collection("eobs").doc(docId).set(payload, { merge: true }),
    userRef.collection("eob_records").doc(docId).set(payload, { merge: true })
  ]);

  return null;
});

exports.extractVeryfiHybridStream = onCall({
  secrets: [veryfiClientId, veryfiUsername, veryfiApiKey],
  timeoutSeconds: 120,
  memory: "512MiB"
}, async (request) => {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Sign in is required for hybrid Veryfi extraction.");
  }

  const { fileBase64, fileName, contentType, blueprintName, documentType, categories } = request.data || {};

  if (!fileBase64 || !fileName) {
    throw new HttpsError("invalid-argument", "fileBase64 and fileName are required.");
  }

  const fileBytes = Buffer.from(fileBase64, "base64");
  const veryfiResponse = await extractWithVeryfi(fileBytes, {
    fileName,
    contentType: contentType || "application/octet-stream",
    blueprintName: blueprintName || BLUEPRINT_HEALTH_INSURANCE_EOB,
    documentType: documentType || DOCUMENT_TYPE_EOB,
    categories: Array.isArray(categories) && categories.length > 0 ? categories : CATEGORIES_INSURANCE,
    fileUrl: request.data?.fileUrl
  }, {
    clientId: veryfiClientId.value(),
    username: veryfiUsername.value(),
    apiKey: veryfiApiKey.value()
  });

  return { veryfi: veryfiResponse };
});

// ----------------------------------------------------------------------
// Vault Receipt Reconciliation
// ----------------------------------------------------------------------
exports.stapleVaultReceiptToEob = onDocumentWritten("users/{userId}/vault_receipts/{receiptId}", async (event) => {
  const after = event.data?.after?.data();
  if (!after || after.stapledEobId) return null;

  const userId = event.params.userId;
  const receiptId = event.params.receiptId;
  const amount = Number(after.amount || 0);

  const serviceDate = normalizeServiceDate(
    String(after.serviceDate || after.service_date || "").trim()
  );

  const receiptDateSortKey = Number(
    after.serviceDateSortKey || after.service_date_sort_key || computeServiceDateSortKey(serviceDate)
  );

  if (!amount || !receiptDateSortKey) return null;

  const eobsSnap = await db.collection("users").doc(userId).collection("eobs")
    .where("serviceDateSortKey", "==", receiptDateSortKey)
    .get();

  for (const doc of eobsSnap.docs) {
    const eob = doc.data() || {};
    if (eob.stapledReceiptId || eob.vaultSubstantiationStatus) continue;

    const patientResp = Number(
      eob.patient_responsibility ??
      eob.patientResponsibility ??
      ((Number(eob.copay || 0)) + (Number(eob.deductible || 0)) + (Number(eob.coinsurance || 0)))
    );

    if (Math.abs(patientResp - amount) < 0.01) {
      await Promise.all([
        doc.ref.set({
          vaultSubstantiationStatus: "Paid & Substantiated",
          stapledReceiptId: receiptId,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true }),
        event.data.after.ref.set({
          stapledEobId: doc.id,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true })
      ]);
      break;
    }
  }
  return null;
});

function normalizeServiceDate(rawDate) {
  const trimmed = String(rawDate || "").trim();
  if (!trimmed || trimmed === "Date not recognized") return "";
  const slashMatch = /^(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})$/.exec(trimmed);
  if (slashMatch) {
    const month = slashMatch[1].padStart(2, "0");
    const day = slashMatch[2].padStart(2, "0");
    let year = slashMatch[3];
    if (year.length === 2) {
      year = Number(year) > 50 ? `19${year}` : `20${year}`;
    }
    return `${month}/${day}/${year}`;
  }
  const isoMatch = /^(\d{4})-(\d{1,2})-(\d{1,2})/.exec(trimmed);
  if (isoMatch) {
    const month = isoMatch[2].padStart(2, "0");
    const day = isoMatch[3].padStart(2, "0");
    const year = isoMatch[1];
    return `${month}/${day}/${year}`;
  }
  return trimmed;
}

function computeServiceDateSortKey(dateStr) {
  const normalized = normalizeServiceDate(dateStr);
  const parts = normalized.split("/");
  if (parts.length !== 3) return 0;
  const month = Number(parts[0]);
  const day = Number(parts[1]);
  const year = Number(parts[2]);
  if (!month || !day || !year) return 0;
  return year * 10000 + month * 100 + day;
}

// ----------------------------------------------------------------------
// External Communications Engine (Nodemailer)
// ----------------------------------------------------------------------
exports.sendEmailNotification = onCall({
  secrets: [smtpHost, smtpUser, smtpPass, smtpFrom],
  timeoutSeconds: 60,
  memory: "256MiB"
}, async (request) => {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Authentication required to request email transmission.");
  }

  const { recipientEmail, subject, textBody, htmlBody } = request.data || {};
  if (!recipientEmail || !subject || (!textBody && !htmlBody)) {
    throw new HttpsError("invalid-argument", "Missing recipientEmail, subject, or text/html body payloads.");
  }

  try {
    // Singleton Initialization: Only construct the transport if it doesn't exist
    // This reuses the socket pool for warm instances, preventing connection timeouts.
    if (!mailTransporter) {
      mailTransporter = nodemailer.createTransport({
        host: smtpHost.value(),
        port: 465,
        secure: true,
        auth: {
          user: smtpUser.value(),
          pass: smtpPass.value(),
        },
      });
    }

    const mailOptions = {
      from: smtpFrom.value(),
      to: recipientEmail,
      subject: subject,
      text: textBody,
      html: htmlBody || textBody,
    };

    const info = await mailTransporter.sendMail(mailOptions);
    console.log(`Live communication successfully dispatched to ${recipientEmail}. Message ID: ${info.messageId}`);

    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error("Critical delivery infrastructure exception:", error);
    // Explicitly nullify the transport if it crashed, forcing a rebuild on next call
    mailTransporter = null;
    throw new HttpsError("internal", `Email relay transmission aborted: ${error.message}`);
  }
});