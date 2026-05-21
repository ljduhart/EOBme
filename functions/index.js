"use strict";

const admin = require("firebase-admin");
const crypto = require("crypto");
const nodemailer = require("nodemailer");
const {onDocumentWritten} = require("firebase-functions/v2/firestore");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
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
const smtpHost = defineSecret("SMTP_HOST");
const smtpPort = defineSecret("SMTP_PORT");
const smtpUser = defineSecret("SMTP_USER");
const smtpPass = defineSecret("SMTP_PASS");
const smtpFrom = defineSecret("SMTP_FROM");

exports.mirrorEobToLegacyRecord = onDocumentWritten("users/{userId}/eobs/{eobId}", async (event) => {
  return mirrorEobWrite(event, "eobs", "eob_records");
});

exports.mirrorLegacyRecordToEob = onDocumentWritten("users/{userId}/eob_records/{eobId}", async (event) => {
  return mirrorEobWrite(event, "eob_records", "eobs");
});

exports.sendAccountVerificationCode = onCall({
  secrets: [smtpHost, smtpPort, smtpUser, smtpPass, smtpFrom]
}, async (request) => {
  const uid = request.auth && request.auth.uid;
  const email = request.auth && request.auth.token && request.auth.token.email;
  if (!uid || !email) {
    throw new HttpsError("unauthenticated", "Sign in before requesting a verification code.");
  }

  const code = String(crypto.randomInt(100000, 1000000));
  const salt = crypto.randomBytes(16).toString("hex");
  const expiresAt = admin.firestore.Timestamp.fromMillis(Date.now() + 10 * 60 * 1000);
  const verificationRef = db.collection("users")
    .doc(uid)
    .collection("account_verifications")
    .doc("email_code");

  await verificationRef.set({
    email,
    salt,
    codeHash: hashVerificationCode(code, salt),
    attempts: 0,
    verified: false,
    expiresAt,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
  await sendVerificationEmail(email, code);
  return {sent: true, expiresInMinutes: 10};
});

exports.verifyAccountVerificationCode = onCall(async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Sign in before verifying your account.");
  }
  const code = String((request.data && request.data.code) || "").trim();
  if (!/^\d{6}$/.test(code)) {
    throw new HttpsError("invalid-argument", "Enter the 6-digit verification code.");
  }

  const userRef = db.collection("users").doc(uid);
  const verificationRef = userRef.collection("account_verifications").doc("email_code");
  const result = await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(verificationRef);
    if (!snapshot.exists) {
      throw new HttpsError("failed-precondition", "Request a new verification code.");
    }
    const data = snapshot.data() || {};
    const attempts = Number(data.attempts || 0);
    const expiresAt = data.expiresAt && data.expiresAt.toMillis ? data.expiresAt.toMillis() : 0;
    if (expiresAt < Date.now()) {
      throw new HttpsError("deadline-exceeded", "Verification code expired. Request a new code.");
    }
    if (attempts >= 5) {
      throw new HttpsError("resource-exhausted", "Too many attempts. Request a new code.");
    }
    if (hashVerificationCode(code, data.salt || "") !== data.codeHash) {
      transaction.update(verificationRef, {
        attempts: attempts + 1,
        lastAttemptAt: admin.firestore.FieldValue.serverTimestamp()
      });
      return {verified: false, message: "Verification code did not match."};
    }

    transaction.update(verificationRef, {
      attempts: attempts + 1,
      verified: true,
      verifiedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    transaction.set(userRef, {
      accountSetupVerified: true,
      emailSecondFactorVerified: true,
      emailVerifiedAt: admin.firestore.FieldValue.serverTimestamp()
    }, {merge: true});
    return {verified: true};
  });

  if (!result.verified) {
    throw new HttpsError("invalid-argument", result.message);
  }
  return {verified: true};
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

function hashVerificationCode(code, salt) {
  return crypto.createHash("sha256").update(`${salt}:${code}`).digest("hex");
}

async function sendVerificationEmail(email, code) {
  const host = smtpHost.value();
  const user = smtpUser.value();
  const pass = smtpPass.value();
  if (!host || !user || !pass) {
    throw new HttpsError("failed-precondition", "SMTP email verification is not configured.");
  }

  const port = Number(smtpPort.value() || 587);
  const transport = nodemailer.createTransport({
    host,
    port,
    secure: port === 465,
    auth: {user, pass}
  });
  await transport.sendMail({
    from: smtpFrom.value() || user,
    to: email,
    subject: "Your EOBme verification code",
    text: `Your EOBme verification code is ${code}. It expires in 10 minutes.`,
    html: `<p>Your EOBme verification code is <strong>${code}</strong>.</p><p>It expires in 10 minutes.</p>`
  });
}
