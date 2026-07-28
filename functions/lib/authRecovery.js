"use strict";

const { HttpsError } = require("firebase-functions/v2/https");
const {
  RESET_CODES_COLLECTION,
  GENERIC_USERNAME_MESSAGE,
  GENERIC_RESET_CODE_MESSAGE,
  normalizeEmail,
  isValidEmail,
  generateResetCode,
  isPasswordValid,
  isValidResetCode
} = require("./authRecoveryCore");

async function findUserByEmail(auth, email) {
  try {
    return await auth.getUserByEmail(email);
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      return null;
    }
    throw error;
  }
}

function createMailTransporter(smtpHost, smtpUser, smtpPass) {
  const nodemailer = require("nodemailer");
  return nodemailer.createTransport({
    host: smtpHost,
    port: 465,
    secure: true,
    auth: {
      user: smtpUser,
      pass: smtpPass
    }
  });
}

async function sendAuthEmail(transporter, fromAddress, recipientEmail, subject, textBody, htmlBody) {
  const info = await transporter.sendMail({
    from: fromAddress,
    to: recipientEmail,
    subject,
    text: textBody,
    html: htmlBody || textBody
  });
  return info.messageId;
}

async function sendForgotUsernameReminder({
  auth,
  transporter,
  fromAddress,
  email
}) {
  const normalizedEmail = normalizeEmail(email);
  if (!normalizedEmail) {
    throw new HttpsError("invalid-argument", "Email is required.");
  }
  if (!isValidEmail(normalizedEmail)) {
    throw new HttpsError("invalid-argument", "Enter a valid email address.");
  }

  const user = await findUserByEmail(auth, normalizedEmail);
  if (user?.email) {
    const subject = "Your EOBme username";
    const textBody =
      `Your EOBme sign-in email (username) is:\n\n${user.email}\n\n` +
      "Use this email address to log in to EOBme.";
    const htmlBody =
      `<p>Your EOBme sign-in email (username) is:</p>` +
      `<p><strong>${user.email}</strong></p>` +
      `<p>Use this email address to log in to EOBme.</p>`;
    await sendAuthEmail(transporter, fromAddress, user.email, subject, textBody, htmlBody);
  }

  return { message: GENERIC_USERNAME_MESSAGE };
}

async function requestPasswordResetCode({
  db,
  auth,
  transporter,
  fromAddress,
  email,
  FieldValue,
  Timestamp
}) {
  const normalizedEmail = normalizeEmail(email);
  if (!normalizedEmail) {
    throw new HttpsError("invalid-argument", "Email is required.");
  }
  if (!isValidEmail(normalizedEmail)) {
    throw new HttpsError("invalid-argument", "Enter a valid email address.");
  }

  const user = await findUserByEmail(auth, normalizedEmail);
  if (user?.email) {
    const code = generateResetCode();
    const expiresAt = Timestamp.fromMillis(Date.now() + RESET_CODE_TTL_MS);
    await db.collection(RESET_CODES_COLLECTION).doc(normalizedEmail).set({
      code,
      email: normalizedEmail,
      createdAt: FieldValue.serverTimestamp(),
      expiresAt
    });

    const subject = "Your EOBme password reset code";
    const textBody =
      `Your EOBme password reset code is: ${code}\n\n` +
      "This code expires in 15 minutes. If you did not request a reset, you can ignore this email.";
    const htmlBody =
      `<p>Your EOBme password reset code is:</p>` +
      `<p><strong style="font-size:20px;letter-spacing:2px;">${code}</strong></p>` +
      `<p>This code expires in 15 minutes. If you did not request a reset, you can ignore this email.</p>`;
    await sendAuthEmail(transporter, fromAddress, user.email, subject, textBody, htmlBody);
  }

  return { message: GENERIC_RESET_CODE_MESSAGE };
}

async function confirmPasswordResetCode({
  db,
  auth,
  email,
  code,
  newPassword
}) {
  const normalizedEmail = normalizeEmail(email);
  const normalizedCode = String(code || "").trim();

  if (!normalizedEmail) {
    throw new HttpsError("invalid-argument", "Email is required.");
  }
  if (!isValidEmail(normalizedEmail)) {
    throw new HttpsError("invalid-argument", "Enter a valid email address.");
  }
  if (!isValidResetCode(normalizedCode)) {
    throw new HttpsError("invalid-argument", "Enter the 5-digit reset code from your email.");
  }
  if (!isPasswordValid(newPassword)) {
    throw new HttpsError(
      "invalid-argument",
      "Password must be at least 8 characters and include a number."
    );
  }

  const resetRef = db.collection(RESET_CODES_COLLECTION).doc(normalizedEmail);
  const resetSnapshot = await resetRef.get();
  if (!resetSnapshot.exists) {
    throw new HttpsError("not-found", "Invalid or expired reset code.");
  }

  const resetData = resetSnapshot.data() || {};
  if (resetData.code !== normalizedCode) {
    throw new HttpsError("invalid-argument", "Invalid reset code.");
  }

  const expiresAt = resetData.expiresAt;
  if (!expiresAt || typeof expiresAt.toMillis !== "function" || expiresAt.toMillis() < Date.now()) {
    await resetRef.delete().catch(() => null);
    throw new HttpsError("failed-precondition", "Reset code expired. Request a new code.");
  }

  const user = await findUserByEmail(auth, normalizedEmail);
  if (!user) {
    await resetRef.delete().catch(() => null);
    throw new HttpsError("not-found", "Invalid or expired reset code.");
  }

  await auth.updateUser(user.uid, { password: newPassword });
  await resetRef.delete();

  return { message: "Password updated. You can sign in with your new password." };
}

module.exports = {
  findUserByEmail,
  createMailTransporter,
  sendAuthEmail,
  sendForgotUsernameReminder,
  requestPasswordResetCode,
  confirmPasswordResetCode
};
