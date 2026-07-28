"use strict";

const RESET_CODES_COLLECTION = "auth_password_reset_codes";
const RESET_CODE_TTL_MS = 15 * 60 * 1000;
const GENERIC_USERNAME_MESSAGE =
  "If an account exists for this email, we sent your username.";
const GENERIC_RESET_CODE_MESSAGE =
  "If an account exists for this email, we sent a password reset code.";

function normalizeEmail(email) {
  return String(email || "").trim().toLowerCase();
}

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function generateResetCode() {
  return String(Math.floor(10000 + Math.random() * 90000));
}

function isPasswordValid(password) {
  const value = String(password || "");
  return value.length >= 8 && /\d/.test(value);
}

function isValidResetCode(code) {
  return /^\d{5}$/.test(String(code || "").trim());
}

module.exports = {
  RESET_CODES_COLLECTION,
  RESET_CODE_TTL_MS,
  GENERIC_USERNAME_MESSAGE,
  GENERIC_RESET_CODE_MESSAGE,
  normalizeEmail,
  isValidEmail,
  generateResetCode,
  isPasswordValid,
  isValidResetCode
};
