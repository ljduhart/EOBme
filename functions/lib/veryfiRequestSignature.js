"use strict";

const {createHmac} = require("crypto");

/**
 * Matches @veryfi/veryfi-sdk generateSignature.js — signs the JSON body fields in insertion order.
 */
function generateVeryfiRequestSignature(clientSecret, payloadParams, timestamp) {
  let payload = `timestamp:${timestamp}`;
  const keys = Object.keys(payloadParams || {});
  for (let index = 0; index < keys.length; index += 1) {
    const key = keys[index];
    const value = payloadParams[key];
    payload = `${payload},${key}:${value}`;
  }
  const secretBytes = encodeURI(String(clientSecret || ""));
  const payloadBytes = encodeURI(payload);
  return createHmac("sha256", secretBytes)
    .update(payloadBytes)
    .digest("base64")
    .trim();
}

function buildVeryfiSignedHeaders(payloadParams, credentials) {
  if (!credentials?.clientSecret) {
    throw new Error("VERYFI_CLIENT_SECRET is required to sign Veryfi POST requests.");
  }
  const timestamp = Date.now();
  const headers = {
    "Content-Type": "application/json",
    "Accept": "application/json",
    "Client-Id": credentials.clientId,
    "Authorization": `apikey ${credentials.username}:${credentials.apiKey}`,
    "X-Veryfi-Request-Timestamp": String(timestamp),
    "X-Veryfi-Request-Signature": generateVeryfiRequestSignature(
      credentials.clientSecret,
      payloadParams,
      timestamp
    )
  };
  return headers;
}

module.exports = {
  buildVeryfiSignedHeaders,
  generateVeryfiRequestSignature
};
