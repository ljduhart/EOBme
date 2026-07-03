# EOBme Hybrid Extraction Pipeline — Immutable Charter

**Status:** LOCKED  
**Authorization:** Changes to any file listed in `HYBRID_PIPELINE_MANIFEST.json` require explicit owner permission and a manifest hash update in the same PR.

This charter defines the structural walls between the Android client and Firebase backend. Treat these rules as immutable laws when building new UI, screens, or features.

---

## 1. Zero Client-Side Credentials (API Barrier)

- Android Kotlin must **never** contain, request, or handle Veryfi API keys, client IDs, or `apikey` authorization headers.
- All Veryfi authentication lives in Firebase Cloud Functions via `defineSecret("VERYFI_CLIENT_ID")`, `defineSecret("VERYFI_USERNAME")`, and `defineSecret("VERYFI_API_KEY")`.
- Production extraction is proxied through `VeryfiDocumentClient` → `extractVeryfiHybridStream`. Do not move API headers into Retrofit/OkHttp on mobile.

## 2. Base64 Payload Contract (Live UI Stream)

- Local image preparation uses `OcrProcessor.prepareUriForUpload` (JPEG compression via `ImageCompressionLevel`) before hybrid extraction.
- Track B streams document bytes as **Base64** from `VeryfiDocumentClient.streamExtractDocument` to `extractVeryfiHybridStream`.
- Do **not** refactor the live UI stream to depend on Firebase Storage `fileUrl` downloads. Storage upload (Track A) runs in parallel for reconciliation only.

## 3. Strict Cloud Gatekeeping (Node.js)

- Callable `extractVeryfiHybridStream` requires Firebase Auth (`request.auth.uid`).
- Legacy client fields (`documentType`, `categories`) may arrive on the callable payload but **must not** be forwarded to Veryfi AnyDocs.
- Multipart AnyDocs transport sends only:
  - `file` (binary, filename embedded in the multipart part)
  - `blueprint_name` → `health_insurance_eob`
- Endpoint: `https://api.veryfi.com/api/v8/partner/any-documents/`
- Android ↔ CF callable name: `extractVeryfiHybridStream`

## 4. Payload Sanitization (Confidence / Nested Objects)

- Android persists embedded Veryfi payloads through `sanitizeForFirestore` inside `VeryfiDocumentClient.buildReconciliationPayload`.
- Do not expose raw Veryfi confidence-score wrapper objects to Firestore or Compose without sanitization.
- If a server-side `deepFlatten()` is introduced in the future, it requires explicit authorization and manifest update.

## 5. Mathematical UI Safety (Kotlin)

- `veryfiNumberField` defaults missing values to `0.0`; `veryfiStringField` defaults to `""`.
- Jetpack Compose layouts that divide by dynamic Veryfi totals must retain `> 0.0` / `coerceAtLeast(1.0)` guards to prevent NaN crashes.

## 6. Hybrid Reconciliation (Dual Track)

| Track | Path | Authority |
|-------|------|-----------|
| **B — Client stream** | `extractVeryfiHybridStream` → `writeReconciliationFindings` | Immediate extraction |
| **A — Storage trigger** | `processUploadedEobWithVeryfi` on `users/{uid}/eobs/{fileName}` | Skips when client stream committed / reconciled |

Shared Firestore document id: `HybridDocumentRef.stableDocumentId(documentRefId)` = `hybridFirestoreDocId(fileName)`.

## 7. OCR Pre-Check (Read-Only)

- `OcrProcessor.kt` and `EobDocumentOcrPreCheck.kt` are locked. Do not modify OCR validation or compression behavior without explicit authorization.

## 8. Protected Files

See `HYBRID_PIPELINE_MANIFEST.json` for the authoritative list and SHA-256 fingerprints.

**Core pipeline (user-specified):**

- `DocumentScanPipelineRepository.kt`
- `VeryfiDocumentClient.kt`
- `VeryfiAnyDocRepository.kt`
- `FirebaseEobMapper.kt`
- `functions/index.js` (Veryfi exports)

**Supporting locked infrastructure:**

- `HybridDocumentRef.kt`, `VeryfiAnyDocConstants.kt`
- `functions/lib/veryfiAnyDocClient.js`, `hybridReconciliation.js`, `veryfiAnyDocConstants.js`
- `functions/lib/veryfiInsuranceEobNormalizer.js`, `eobNormalizer.js`
- `OcrProcessor.kt`, `EobDocumentOcrPreCheck.kt`

---

## Agent / Engineer Directive

When asked to build new features:

1. Treat protected files as **READ-ONLY structural walls**.
2. Do not alter core logic, return types, or extraction algorithms without explicit authorization.
3. If authorized changes are approved, update `HYBRID_PIPELINE_MANIFEST.json` hashes in the same PR.
4. Run `HybridPipelineLockdownTest` and `hybridPipelineLockdown.test.js` before merge.
