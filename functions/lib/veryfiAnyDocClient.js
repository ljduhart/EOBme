"use strict";

const {
  VERYFI_ANY_DOCS_URL,
  BLUEPRINT_HEALTH_INSURANCE_EOB,
  DOCUMENT_TYPE_EOB,
  CATEGORIES_INSURANCE
} = require("./veryfiAnyDocConstants");

async function extractWithVeryfiJson({
  fileUrl,
  blueprintName,
  documentType,
  categories,
  clientId,
  username,
  apiKey
}) {
  const response = await fetch(VERYFI_ANY_DOCS_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "CLIENT-ID": clientId,
      "Authorization": `apikey ${username}:${apiKey}`
    },
    body: JSON.stringify({
      file_url: fileUrl,
      blueprint_name: blueprintName || BLUEPRINT_HEALTH_INSURANCE_EOB,
      document_type: documentType || DOCUMENT_TYPE_EOB,
      categories: categories || CATEGORIES_INSURANCE
    })
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
  documentType,
  categories,
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
  form.append("document_type", documentType || DOCUMENT_TYPE_EOB);
  form.append("categories", JSON.stringify(categories || CATEGORIES_INSURANCE));

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
 * - JSON + file_url when a publicly accessible URL is available (portal contract).
 * - multipart file upload for hybrid stream bytes and storage-trigger fallbacks.
 */
async function extractWithVeryfi(fileBytes, fileMetadata, credentials) {
  const blueprintName = fileMetadata.blueprintName || BLUEPRINT_HEALTH_INSURANCE_EOB;
  const documentType = fileMetadata.documentType || DOCUMENT_TYPE_EOB;
  const categories = Array.isArray(fileMetadata.categories) && fileMetadata.categories.length > 0 ?
    fileMetadata.categories :
    CATEGORIES_INSURANCE;
  const fileUrl = String(fileMetadata.fileUrl || "").trim();
  const hasBytes = fileBytes && fileBytes.length > 0;

  if (fileUrl) {
    try {
      return await extractWithVeryfiJson({
        fileUrl,
        blueprintName,
        documentType,
        categories,
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
    documentType,
    categories,
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
