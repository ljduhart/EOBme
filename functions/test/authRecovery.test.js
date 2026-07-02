"use strict";

const {describe, it} = require("node:test");
const assert = require("node:assert/strict");
const {
  normalizeEmail,
  isValidEmail,
  hashCode,
  generateFiveDigitCode,
  isPasswordValid
} = require("../lib/authRecovery");

describe("authRecovery", () => {
  it("normalizes email addresses", () => {
    assert.equal(normalizeEmail("  User@Example.COM "), "user@example.com");
  });

  it("validates email format", () => {
    assert.equal(isValidEmail("user@example.com"), true);
    assert.equal(isValidEmail("invalid"), false);
  });

  it("generates five digit codes", () => {
    const code = generateFiveDigitCode();
    assert.match(code, /^\d{5}$/);
  });

  it("hashes reset codes deterministically", () => {
    assert.equal(hashCode("12345"), hashCode("12345"));
    assert.notEqual(hashCode("12345"), hashCode("54321"));
  });

  it("enforces password policy", () => {
    assert.equal(isPasswordValid("password1"), true);
    assert.equal(isPasswordValid("short1"), false);
    assert.equal(isPasswordValid("longpassword"), false);
  });
});
