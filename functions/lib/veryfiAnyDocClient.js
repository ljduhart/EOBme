"use strict";

const {
  VERYFI_ANY_DOCS_URL,
  BLUEPRINT_HEALTH_INSURANCE_EOB
} = require("./veryfiAnyDocConstants");
const {buildVeryfiSignedHeaders} = require("./veryfiRequestSignature");

const MIN_FILE_BYTES = 256;
const MAX_FILE_BYTES = 20 * 1024 * 1024;

class VeryfiConfigurationError extends Error {
  constructor(message) {
    super(message);
    this.name = "VeryfiConfigurationError";
  }
}

class VeryfiRequestValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = "VeryfiRequestValidationError";
  }
}

class VeryfiApiError extends Error {
  constructor(status, body) {
    super(`Veryfi any-documents extraction failed with status ${status}: ${body}`);
    this.name = "VeryfiApiError";
    this.status = status;
    this.body = body;
  }
}

function normalizeBase64Payload(fileBase64) {
  const trimmed = String(fileBase64 || "").trim();
  const dataUriMatch = trimmed.match(/^data:[^;]+;base64,(.+)$/i);
  return (dataUriMatch ? dataUriMatch[1] : trimmed).replace(/\s+/g, "");
}

function formatFileDataForVeryfi(normalizedBase64, contentType) {
  const mimeType = String(contentType || "").trim() || "image/jpeg";
  return `data:${mimeType};base64,${normalizedBase64}`;
}

function resolveBlueprintName(blueprintName) {
  const resolved = String(blueprintName || "").trim();
  return resolved || BLUEPRINT_HEALTH_INSURANCE_EOB;
}

function assertVeryfiCredentials(credentials) {
  if (!credentials?.clientId || !credentials?.username || !credentials?.apiKey) {
    throw new VeryfiConfigurationError(
      "Veryfi API credentials are incomplete. Configure VERYFI_CLIENT_ID, VERYFI_USERNAME, and VERYFI_API_KEY."
    );
  }
  if (!credentials?.clientSecret) {
    throw new VeryfiConfigurationError(
      "VERYFI_CLIENT_SECRET is not configured. Set it via firebase functions:secrets:set VERYFI_CLIENT_SECRET."
    );
  }
}

function buildAnyDocRequestBodyFromUrl({
  fileUrl,
  blueprintName,
  externalId
}) {
  const resolvedUrl = String(fileUrl || "").trim();
  if (!resolvedUrl) {
    throw new VeryfiRequestValidationError("file_url is required for Veryfi extraction.");
  }
  const body = {
    file_url: resolvedUrl,
    blueprint_name: resolveBlueprintName(blueprintName)
  };
  const external = String(externalId || "").trim();
  if (external) {
    body.external_id = external;
  }
  return body;
}

function buildAnyDocRequestBody({
  fileDataBase64,
  fileName,
  blueprintName,
  externalId,
  contentType
}) {
  const normalizedBase64 = normalizeBase64Payload(fileDataBase64);
  if (!normalizedBase64) {
    throw new VeryfiRequestValidationError("Document bytes are required for Veryfi extraction.");
  }
  const fileBytes = Buffer.from(normalizedBase64, "base64");
  if (!fileBytes.length) {
    throw new VeryfiRequestValidationError("fileBase64 is not valid base64.");
  }
  if (fileBytes.length < MIN_FILE_BYTES) {
    throw new VeryfiRequestValidationError(
      `Document is too small for Veryfi processing (${fileBytes.length} bytes).`
    );
  }
  if (fileBytes.length > MAX_FILE_BYTES) {
    throw new VeryfiRequestValidationError(
      `Document exceeds Veryfi 20MB limit (${fileBytes.length} bytes).`
    );
  }

  const resolvedName = String(fileName || "").trim() || "eob.jpg";
  const body = {
    file_name: resolvedName,
    file_data: formatFileDataForVeryfi(normalizedBase64, contentType),
    blueprint_name: resolveBlueprintName(blueprintName)
  };
  const external = String(externalId || "").trim();
  if (external) {
    body.external_id = external;
  }
  return body;
}

async function postSignedAnyDocument(body, credentials) {
  assertVeryfiCredentials(credentials);
  const headers = buildVeryfiSignedHeaders(body, credentials);
  const response = await fetch(VERYFI_ANY_DOCS_URL, {
    method: "POST",
    headers,
    body: JSON.stringify(body)
  });

  const responseText = await response.text();
  if (!response.ok) {
    throw new VeryfiApiError(response.status, responseText);
  }

  try {
    return JSON.parse(responseText);
  } catch (error) {
    throw new VeryfiApiError(response.status, responseText || "Invalid JSON response from Veryfi.");
  }
}

async function postAnyDocument({
  fileDataBase64,
  fileName,
  blueprintName,
  externalId,
  contentType,
  credentials
}) {
  const body = buildAnyDocRequestBody({
    fileDataBase64,
    fileName,
    blueprintName,
    externalId,
    contentType
  });
  return postSignedAnyDocument(body, credentials);
}

async function postAnyDocumentFromUrl({
  fileUrl,
  blueprintName,
  externalId,
  credentials
}) {
  const body = buildAnyDocRequestBodyFromUrl({
    fileUrl,
    blueprintName,
    externalId
  });
  return postSignedAnyDocument(body, credentials);
}

async function postAnyDocumentFromBytes({
  fileBytes,
  fileName,
  blueprintName,
  externalId,
  contentType,
  credentials
}) {
  const base64 = Buffer.from(fileBytes).toString("base64");
  return postAnyDocument({
    fileDataBase64: base64,
    fileName,
    blueprintName,
    externalId,
    contentType,
    credentials
  });
}

module.exports = {
  MIN_FILE_BYTES,
  MAX_FILE_BYTES,
  VeryfiApiError,
  VeryfiConfigurationError,
  VeryfiRequestValidationError,
  buildAnyDocRequestBody,
  buildAnyDocRequestBodyFromUrl,
  formatFileDataForVeryfi,
  normalizeBase64Payload,
  postAnyDocument,
  postAnyDocumentFromBytes,
  postAnyDocumentFromUrl,
  resolveBlueprintName
};
