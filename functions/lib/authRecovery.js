"use strict";

const crypto = require("crypto");
const {HttpsError} = require("firebase-functions/v2/https");

const OTP_COLLECTION = "auth_recovery_otps";
const OTP_TTL_MS = 10 * 60 * 1000;
const MAX_OTP_ATTEMPTS = 5;

function normalizeEmail(email) {
  return String(email || "").trim().toLowerCase();
}

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function hashEmail(email) {
  return crypto.createHash("sha256").update(normalizeEmail(email)).digest("hex");
}

function hashCode(code) {
  return crypto.createHash("sha256").update(String(code)).digest("hex");
}

function generateFiveDigitCode() {
  return String(crypto.randomInt(10000, 100000));
}

function isPasswordValid(password) {
  const value = String(password || "");
  return value.length >= 8 && /\d/.test(value);
}

async function resolveUserByEmail(adminAuth, email) {
  try {
    return await adminAuth.getUserByEmail(email);
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      return null;
    }
    throw error;
  }
}

async function sendAuthEmail(transporter, fromAddress, to, subject, text) {
  if (!transporter) {
    throw new HttpsError("failed-precondition", "Auth email delivery is not configured.");
  }
  await transporter.sendMail({
    from: fromAddress,
    to,
    subject,
    text
  });
}

async function sendForgotUsernameEmail(adminAuth, transporter, fromAddress, email) {
  if (!isValidEmail(email)) {
    throw new HttpsError("invalid-argument", "A valid email address is required.");
  }
  const user = await resolveUserByEmail(adminAuth, email);
  if (user) {
    await sendAuthEmail(
      transporter,
      fromAddress,
      email,
      "Your EOBme username",
      `Your EOBme username is:\n\n${email}\n\nUse this email address to sign in to EOBme.`
    );
  }
  return {
    message: "If an account exists for that email, your username has been sent."
  };
}

async function requestPasswordResetCode(db, adminAuth, transporter, fromAddress, email) {
  if (!isValidEmail(email)) {
    throw new HttpsError("invalid-argument", "A valid email address is required.");
  }
  const user = await resolveUserByEmail(adminAuth, email);
  if (user) {
    const code = generateFiveDigitCode();
    const docRef = db.collection(OTP_COLLECTION).doc(hashEmail(email));
    await docRef.set({
      codeHash: hashCode(code),
      uid: user.uid,
      email,
      attempts: 0,
      expiresAt: Date.now() + OTP_TTL_MS,
      createdAt: Date.now()
    });
    await sendAuthEmail(
      transporter,
      fromAddress,
      email,
      "Your EOBme password reset code",
      `Your EOBme password reset code is ${code}.\n\nEnter this 5-digit code in the app to set a new password. The code expires in 10 minutes.`
    );
  }
  return {
    message: "If an account exists for that email, a 5-digit reset code has been sent."
  };
}

async function confirmPasswordResetCode(db, adminAuth, email, code, newPassword) {
  if (!isValidEmail(email)) {
    throw new HttpsError("invalid-argument", "A valid email address is required.");
  }
  if (!/^\d{5}$/.test(String(code || ""))) {
    throw new HttpsError("invalid-argument", "Enter the 5-digit reset code from your email.");
  }
  if (!isPasswordValid(newPassword)) {
    throw new HttpsError(
      "invalid-argument",
      "Password must be at least 8 characters and include at least 1 number."
    );
  }

  const docRef = db.collection(OTP_COLLECTION).doc(hashEmail(email));
  const snapshot = await docRef.get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Reset code is invalid or expired.");
  }

  const data = snapshot.data() || {};
  const expiresAt = Number(data.expiresAt || 0);
  const attempts = Number(data.attempts || 0);
  if (!expiresAt || Date.now() > expiresAt) {
    await docRef.delete();
    throw new HttpsError("deadline-exceeded", "Reset code has expired. Request a new code.");
  }
  if (attempts >= MAX_OTP_ATTEMPTS) {
    await docRef.delete();
    throw new HttpsError("resource-exhausted", "Too many invalid attempts. Request a new code.");
  }
  if (data.codeHash !== hashCode(code)) {
    await docRef.set({attempts: attempts + 1}, {merge: true});
    throw new HttpsError("permission-denied", "Reset code is incorrect.");
  }

  const uid = String(data.uid || "");
  if (!uid) {
    await docRef.delete();
    throw new HttpsError("not-found", "Reset code is invalid or expired.");
  }

  await adminAuth.updateUser(uid, {password: newPassword});
  await docRef.delete();
  return {
    message: "Password updated. You can sign in with your new password."
  };
}

module.exports = {
  OTP_COLLECTION,
  normalizeEmail,
  isValidEmail,
  hashEmail,
  hashCode,
  generateFiveDigitCode,
  isPasswordValid,
  sendForgotUsernameEmail,
  requestPasswordResetCode,
  confirmPasswordResetCode
};
