"use strict";

const {
  VERYFI_ANY_DOCS_URL,
  BLUEPRINT_HEALTH_INSURANCE_EOB
} = require("./veryfiAnyDocConstants");

async function extractWithVeryfiJson({
  fileUrl,
  fileName,
  blueprintName,
  clientId,
  username,
  apiKey
}) {
  const body = {
    file_url: fileUrl,
    blueprint_name: blueprintName || BLUEPRINT_HEALTH_INSURANCE_EOB
  };
  if (fileName) {
    body.file_name = fileName;
  }

  const response = await fetch(VERYFI_ANY_DOCS_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "CLIENT-ID": clientId,
      "Authorization": `apikey ${username}:${apiKey}`
    },
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    throw new Error(`Veryfi JSON extraction failed with status ${response.status}: ${await response.text()}`);
  }
  return response.json();
}

async function extractWithVeryfiMultipart({
  fileBytes,
  fileName,
  contentType,
  blueprintName,
  clientId,
  username,
  apiKey
}) {
  const form = new FormData();
  form.append(
    "file",
    new Blob([fileBytes], {type: contentType || "application/octet-stream"}),
    fileName || "document.jpg"
  );
  form.append("blueprint_name", blueprintName || BLUEPRINT_HEALTH_INSURANCE_EOB);

  const response = await fetch(VERYFI_ANY_DOCS_URL, {
    method: "POST",
    headers: {
      "Client-Id": clientId,
      "Authorization": `apikey ${username}:${apiKey}`
    },
    body: form
  });

  if (!response.ok) {
    throw new Error(`Veryfi multipart extraction failed with status ${response.status}: ${await response.text()}`);
  }
  return response.json();
}

/**
 * Veryfi AnyDocs transport:
 * - JSON + file_url when a publicly accessible URL is available.
 * - multipart file upload for hybrid stream bytes and storage-trigger fallbacks.
 *
 * The partner/any-documents contract routes extraction via blueprint_name only.
 * document_type and categories are not accepted by AnyDocs and must not be sent.
 */
async function extractWithVeryfi(fileBytes, fileMetadata, credentials) {
  const blueprintName = fileMetadata.blueprintName || BLUEPRINT_HEALTH_INSURANCE_EOB;
  const fileUrl = String(fileMetadata.fileUrl || "").trim();
  const hasBytes = fileBytes && fileBytes.length > 0;

  if (fileUrl) {
    try {
      return await extractWithVeryfiJson({
        fileUrl,
        fileName: fileMetadata.fileName,
        blueprintName,
        clientId: credentials.clientId,
        username: credentials.username,
        apiKey: credentials.apiKey
      });
    } catch (jsonError) {
      if (!hasBytes) {
        throw jsonError;
      }
    }
  }

  if (!hasBytes) {
    throw new Error("Veryfi extraction requires file bytes or a public file_url.");
  }

  return extractWithVeryfiMultipart({
    fileBytes,
    fileName: fileMetadata.fileName,
    contentType: fileMetadata.contentType,
    blueprintName,
    clientId: credentials.clientId,
    username: credentials.username,
    apiKey: credentials.apiKey
  });
}

module.exports = {
  extractWithVeryfi,
  extractWithVeryfiJson,
  extractWithVeryfiMultipart
};
