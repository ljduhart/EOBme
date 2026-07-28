"use strict";

const {describe, it} = require("node:test");
const assert = require("node:assert/strict");
const {
  normalizeEmail,
  isValidEmail,
  generateResetCode,
  isPasswordValid,
  isValidResetCode,
  GENERIC_USERNAME_MESSAGE,
  GENERIC_RESET_CODE_MESSAGE
} = require("../lib/authRecoveryCore");

describe("authRecovery", () => {
  it("normalizes email addresses", () => {
    assert.equal(normalizeEmail("  User@Example.COM "), "user@example.com");
  });

  it("validates email format", () => {
    assert.equal(isValidEmail("user@example.com"), true);
    assert.equal(isValidEmail("not-an-email"), false);
  });

  it("generates five digit reset codes", () => {
    const code = generateResetCode();
    assert.match(code, /^\d{5}$/);
  });

  it("validates password policy", () => {
    assert.equal(isPasswordValid("password1"), true);
    assert.equal(isPasswordValid("short1"), false);
    assert.equal(isPasswordValid("longpassword"), false);
  });

  it("validates reset code format", () => {
    assert.equal(isValidResetCode("12345"), true);
    assert.equal(isValidResetCode("1234"), false);
    assert.equal(isValidResetCode("12a45"), false);
  });

  it("uses generic success messages", () => {
    assert.match(GENERIC_USERNAME_MESSAGE, /account exists/i);
    assert.match(GENERIC_RESET_CODE_MESSAGE, /account exists/i);
  });
});
