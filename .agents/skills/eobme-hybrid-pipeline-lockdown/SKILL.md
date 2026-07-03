---
name: eobme-hybrid-pipeline-lockdown
description: Immutable architectural laws for the EOBme Veryfi/Firebase hybrid extraction pipeline. Use when building features, refactoring, or reviewing PRs that might touch OCR, Veryfi, Firebase Cloud Functions extraction, or EOB scan upload flows.
---

# EOBme Hybrid Pipeline Lockdown

## When to Activate

Activate this skill whenever work might touch:

- EOB camera scan / document upload
- Veryfi AnyDocs extraction
- `extractVeryfiHybridStream` or `processUploadedEobWithVeryfi`
- `DocumentScanPipelineRepository`, `VeryfiDocumentClient`, `VeryfiAnyDocRepository`, `FirebaseEobMapper`
- `OcrProcessor`, `EobDocumentOcrPreCheck`

## Immutable Laws (never violate without explicit owner authorization)

1. **Zero client-side Veryfi credentials** — API keys and `apikey` headers live only in Firebase `defineSecret` Cloud Functions.
2. **Base64 live stream** — Track B uses `Base64.encodeToString` → `extractVeryfiHybridStream`. Do not replace the live UI path with Storage `fileUrl` downloads.
3. **AnyDocs multipart contract** — Only `file` + `blueprint_name` (`health_insurance_eob`) to `partner/any-documents/`. Never forward `document_type` or `categories` to Veryfi.
4. **Callable auth gate** — `extractVeryfiHybridStream` requires `request.auth.uid`.
5. **Payload sanitization** — Android uses `sanitizeForFirestore` before persisting `veryfiClientStream`. Do not strip or bypass without authorization.
6. **Mapper defaults** — `veryfiNumberField` → `0.0`, `veryfiStringField` → `""`.
7. **UI division safety** — Compose math on Veryfi totals must guard with `> 0.0` or `coerceAtLeast(1.0)`.
8. **OCR read-only** — Do not modify `OcrProcessor.kt` or `EobDocumentOcrPreCheck.kt`.
9. **EobViewModel hub** — `EobViewModel.kt` remains subscription/hub source of truth; do not reroute hybrid scan through alternate ViewModels.

## Protected Files

Read `hybrid-pipeline/HYBRID_PIPELINE_MANIFEST.json` before any edit. If a protected file hash would change:

- **STOP** unless the user explicitly authorized the pipeline change.
- Update manifest SHA-256 entries in the same PR.
- Run `HybridPipelineLockdownTest` and `hybridPipelineLockdown.test.js`.

## Full Charter

See `hybrid-pipeline/HYBRID_PIPELINE_CHARTER.md`.

## Verification Commands

```bash
cd functions && npm test
./gradlew :app:testDebugUnitTest --tests "app.eob.me.HybridPipelineLockdownTest"
```
