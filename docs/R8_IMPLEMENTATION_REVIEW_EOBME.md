# EOBme Android — R8 Implementation Feasibility Review

**Document type:** Architecture & build review (no code changes)  
**Date:** 2026-07-08  
**App:** EOBme (`app.eob.me`) — version 15.3 (versionCode 144)  
**Reviewer scope:** Full codebase review against protected Veryfi/Firebase hybrid pipeline, vault receipt pipeline, and MVVM hub architecture  
**Constraint honored:** `EobViewModel.kt` and all 14 SHA-256–locked hybrid-pipeline manifest files remain untouched; splash/logo/intro screens not in scope for modification.

---

## 1. Executive Summary

**Verdict: R8 can be enabled for EOBme while preserving all features, UI flows, and architecture intact.**

R8 (code shrinking, obfuscation, and optimization) is a **build-time** step. It does **not** require edits to application logic, `EobViewModel.kt`, or any of the 14 protected files listed in `hybrid-pipeline/HYBRID_PIPELINE_MANIFEST.json`. Implementation is limited to:

1. Setting `isMinifyEnabled = true` (and optionally `isShrinkResources = true`) in the release build type.
2. Expanding `app/proguard-rules.pro` with targeted keep rules for Gson DTOs, Retrofit interfaces, WorkManager workers, and Crashlytics deobfuscation attributes.

The Veryfi/Firebase hybrid pipeline and the Tax Vault receipt pipeline are **low R8 risk** because they use direct Kotlin references, compile-time string constants, and `Map<String, Any?>` payloads—not reflection or annotation-driven serialization on the critical path.

**Current baseline:** Release builds run with **R8 disabled** (`isMinifyEnabled = false`). ProGuard rules file is the default Android Studio template with no project-specific keeps. **494 unit tests pass** with 0 failures; `HybridPipelineLockdownTest` enforces manifest integrity.

---

## 2. Current Build Configuration

| Setting | Location | Current value |
|---------|----------|---------------|
| Minification | `app/build.gradle.kts:39` | `isMinifyEnabled = false` |
| ProGuard files | `app/build.gradle.kts:40-42` | `proguard-android-optimize.txt` + `proguard-rules.pro` |
| Resource shrinking | Not configured | Disabled (implicit) |
| Multidex | Not configured | Default (minSdk 24) |
| AGP | `gradle/libs.versions.toml` | 9.2.0 (R8 is the default shrinker when minify is on) |
| Kotlin | 2.2.10 + Compose compiler plugin | Modern; R8-compatible |
| Desugaring | Enabled | `coreLibraryDesugaring` for Java 11 APIs |

Release builds already depend on `verifyGoogleServicesJson` before `assembleRelease` / `bundleRelease`, which is compatible with R8 enablement.

---

## 3. Architecture Map (What R8 Must Not Break)

### 3.1 Hub source of truth — `EobViewModel.kt`

`EobViewModel` is the authenticated hub state owner:

- EOB records, selection, appeals, news, uploads
- `processScannedDocument` → hybrid Veryfi pipeline via `EobRepository.processHybridScannedDocument`
- `processVaultReceiptScannedDocument` → OCR + Firestore receipt save
- Tax Vault evidence, export, FSA doomsday scheduling
- Feature gates, subscription usage, hub settings

**R8 impact:** None on source code. ViewModel methods are invoked directly from Compose/navigation; no reflection-based ViewModel factories (uses `viewModel()` default factory).

### 3.2 Navigation & onboarding (untouchable screens)

| Layer | Files | R8 notes |
|-------|-------|----------|
| Splash / logo | `SplashScreen.kt`, `EobSplashLogo.kt` | Pure Compose; no serialization |
| Intro | `IntroScreen.kt` | Pure Compose |
| Onboarding flow | `AppViewModel` + `Screen` sealed class | Route strings are compile-time constants |
| Hub navigation | `EobNavHost.kt`, `EobRoute` sealed class | Navigation Compose uses string routes; safe |

Onboarding path documented in `AndroidManifest.xml`: Splash → Language → Intro → AuthChoice → Auth → MainHub.

### 3.3 Data layer

```
UI (Compose) → EobViewModel → EobRepository (interface)
                                    ↓
                         FirebaseEobRemoteDataSource
                                    ↓
                         FirebaseEobRepository
                                    ↓
              DocumentScanPipelineRepository (hybrid scans)
              VeryfiDocumentClient (callable + Firestore)
```

R8 obfuscates private implementation details but preserves public entry points referenced from kept or non-stripped call graph roots (`Application`, `Activity`, `Worker`, Firebase SDK entry points).

---

## 4. Protected Veryfi / Firebase Hybrid Pipeline

### 4.1 Manifest lockdown

`hybrid-pipeline/HYBRID_PIPELINE_MANIFEST.json` locks **14 files** (7 Android `.kt`, 7 Cloud Functions `.js`) with SHA-256 hashes. `HybridPipelineLockdownTest` fails CI if any protected file changes without manifest update.

**Android protected files (tiers):**

| File | Tier |
|------|------|
| `DocumentScanPipelineRepository.kt` | hybrid-pipeline |
| `VeryfiDocumentClient.kt` | hybrid-pipeline |
| `VeryfiAnyDocRepository.kt` | hybrid-pipeline |
| `FirebaseEobMapper.kt` | hybrid-pipeline |
| `HybridDocumentRef.kt` | hybrid-pipeline |
| `VeryfiAnyDocConstants.kt` | hybrid-pipeline |
| `OcrProcessor.kt` | ocr |
| `EobDocumentOcrPreCheck.kt` | ocr |

### 4.2 Hybrid flow (Track A + Track B)

`DocumentScanPipelineRepository.processHybridDocument`:

1. **Parallel Task B:** Base64-encode local bytes → Firebase Callable `extractVeryfiHybridStream` with payload keys `fileBase64`, `fileName`, `contentType`, `documentRefId`, `blueprintName`, `documentType`, `categories`.
2. **Parallel Task A:** Firebase Storage upload to `users/{userId}/eobs/{fileName}`.
3. **Reconciliation:** `writeReconciliationFindings` → Firestore merge; `finalizeHybridReconciliation` after upload completes.

`EobViewModel.processScannedDocument` orchestrates OCR pre-check, image prep, then `repo.processHybridScannedDocument`.

### 4.3 Why hybrid pipeline is R8-safe without source edits

| Contract element | Mechanism | R8 risk |
|------------------|-----------|---------|
| Callable name | `VeryfiAnyDocConstants.EXTRACT_VERYFI_HYBRID_STREAM` → `"extractVeryfiHybridStream"` | **Low** — string literal inlined |
| Payload keys | Kotlin `hashMapOf("fileBase64" to …)` | **Low** — string literals preserved |
| Response parsing | `Map<String, Any?>` cast, `veryfiStringField` / `veryfiNumberField` extensions | **Low** — no Gson on stream path |
| Firestore field names | String literals in maps | **Low** |
| Credential barrier | Zero client-side Veryfi API keys (enforced by lockdown test) | **N/A** |
| OCR pre-check | Direct calls to protected `OcrProcessor`, `EobDocumentOcrPreCheck` | **Low** |

**No reflection** is used on the hybrid critical path (`VeryfiIndexedFieldReader.kt` explicitly documents "no reflection").

### 4.4 Gson usage near (but not on) hybrid stream path

`VeryfiAnyDocMapper` and `VeryfiInsuranceEobMapper` use Gson for **nested claims / blueprint DTO parsing** when normalizing insurance EOB payloads. These run **after** the callable returns a `Map`, when `veryfiPayloadToEobRecord` detects nested claims.

**R8 action required:** Keep Gson-annotated DTO classes (see Section 6). This is a **ProGuard rules change only**—not a protected file edit.

---

## 5. Add Receipt Pipeline (Tax Vault)

### 5.1 Flow

Triggered from Tax Vault via `EobViewModel.beginVaultReceiptScan()` → camera → `processVaultReceiptScannedDocument`:

1. `OcrProcessor.recognizeFromUri` (protected)
2. `EobDocumentOcrPreCheck.validateForScanType(Receipt)` (protected)
3. `OcrProcessor.prepareUriForUpload`
4. `VaultReceiptMapper.parseReceiptFromOcr`
5. `FirebaseEobRepository.uploadVaultReceiptAwaitDownload`
6. `VaultReceiptMapper.receiptToMap` → Firestore `saveVaultReceipt`

### 5.2 R8 assessment

| Component | Protected? | R8 risk |
|-----------|------------|---------|
| `VaultReceiptMapper` | No | **Low** — pure Kotlin, string map keys |
| `ReceiptRecord` data class | No | **Low** — constructed in ViewModel |
| Firestore observe/save | No | **Low** — string field names |
| OCR utilities | Yes (manifest) | **Low** — direct invocation |

Receipt pipeline does **not** call Veryfi callable or Gson DTOs. Enabling R8 does not require receipt pipeline source changes.

---

## 6. Required ProGuard / R8 Keep Rules (Build Files Only)

The following rules belong in `app/proguard-rules.pro` when enabling R8. **None of these modify application `.kt` sources.**

### 6.1 Crashlytics & debugging

```proguard
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

Enables readable stack traces when mapping files are uploaded to Firebase Crashlytics.

### 6.2 Gson — Veryfi & RSS DTOs

Keep classes with `@SerializedName` used by Gson parsers:

```proguard
-keepattributes Signature
-keepattributes *Annotation*

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class app.eob.me.network.VeryfiAnyDocDto { *; }
-keep class app.eob.me.network.VeryfiAnyDocLineItemDto { *; }
-keep class app.eob.me.network.dto.** { *; }
-keep class app.eob.me.network.RssNetworkModels** { *; }
```

**Affected features if omitted:** Insurance EOB nested-claims parsing; live insurance news RSS feed in `EobViewModel.fetchLiveInsuranceNews`.

### 6.3 Retrofit

```proguard
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-keep interface app.eob.me.network.NewsApiService { *; }
-keep interface app.eob.me.network.VeryfiAnyDocApiService { *; }
```

`RetrofitClient` serves Becker's / Healthcare Dive RSS. `VeryfiAnyDocApiService` is a contract for AnyDocs (production uses Firebase proxy, but mapper tests and fallback paths reference it).

### 6.4 WorkManager

```proguard
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class app.eob.me.work.FsaDoomsdayNotificationWorker { *; }
```

**Affected feature if omitted:** FSA doomsday periodic notifications (`FsaDoomsdayScheduler`).

### 6.5 Firebase, Play Billing, RevenueCat

These SDKs ship **consumer ProGuard rules** via Maven artifacts:

- `firebase-bom` 34.13.0 (Auth, Firestore, Storage, Functions, Crashlytics, Messaging)
- `billing:8.3.0`
- `purchases:10.8.0` (RevenueCat)

**Action:** Verify merged `build/outputs/mapping/release/configuration.txt` after first R8 release build. Typically no extra keeps beyond SDK defaults.

`EobApplication` configures `Purchases.configure(PurchasesConfiguration.Builder…)` — Application class is automatically kept as manifest entry point.

### 6.6 Compose, Navigation, CameraX, ML Kit, Coil

| Library | Notes |
|---------|-------|
| Jetpack Compose BOM 2026.02.01 | Compiler-generated code; no `@Keep` on Composables required for runtime |
| Navigation Compose 2.9.8 | String routes from `EobRoute` / `Screen` — safe |
| CameraX 1.5.1 | Consumer rules included |
| ML Kit text recognition 16.0.1 | Consumer rules included |
| Play Services document scanner 16.0.0-beta1 | Google Play Services rules |
| Coil 2.7.0 | Generally R8-safe; image loading for evidence thumbnails |

### 6.7 Kotlin metadata (recommended)

```proguard
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
```

Preserves coroutines and sealed-class behavior under aggressive optimization.

---

## 7. Feature-by-Feature Risk Matrix

| Feature area | Primary classes | R8 risk | Mitigation |
|--------------|-----------------|---------|------------|
| Hybrid EOB scan | `DocumentScanPipelineRepository`, `VeryfiDocumentClient` | **Low** | No extra keeps; string/map contracts |
| OCR pre-check | `OcrProcessor`, `EobDocumentOcrPreCheck` | **Low** | Direct calls |
| Vault receipt scan | `VaultReceiptMapper`, `processVaultReceiptScannedDocument` | **Low** | No Gson |
| Tax Vault export / evidence | `TaxVaultClaimPackager`, Coil URLs | **Low** | Standard keeps |
| Appeal generator | `AppealLetterGenerator` | **Low** | Pure Kotlin |
| Insurance news RSS | `RetrofitClient`, `RssNewsMapper` | **Medium** | Gson + Retrofit keeps |
| Veryfi nested claims parsing | `VeryfiInsuranceEobMapper`, DTOs | **Medium** | Gson DTO keeps |
| Subscriptions | `BillingRepository`, `RevenueCatBillingRepository` | **Low** | SDK consumer rules |
| FSA notifications | `FsaDoomsdayNotificationWorker` | **Medium** | Worker keep |
| Firebase Auth / Firestore sync | `FirebaseEobRepository` | **Low** | Firebase BOM rules |
| Onboarding / splash / intro | `SplashScreen`, `IntroScreen` | **None** | Compose-only |
| Hub bento / CPT / history | Compose screens + `EobViewModel` | **Low** | No reflection |
| Camera capture | `CameraCaptureViewModel`, CameraX | **Low** | CameraX rules |
| Cloud Functions backend | `functions/*` | **N/A** | Server-side; unaffected by Android R8 |

---

## 8. Phased Implementation Plan

### Phase 0 — Baseline (no logic changes)

1. Build release AAB with `isMinifyEnabled = false`; record APK/AAB size and method count.
2. Confirm `google-services.json` present for `app.eob.me`.

### Phase 1 — Enable R8 with diagnostics

In `app/build.gradle.kts` release block only:

```kotlin
isMinifyEnabled = true
isShrinkResources = true  // optional; test thoroughly for drawable/string refs
```

Add Section 6 rules to `proguard-rules.pro`.

### Phase 2 — Build & mapping

```bash
./gradlew :app:assembleRelease
```

Inspect:

- `app/build/outputs/mapping/release/mapping.txt`
- `app/build/outputs/mapping/release/configuration.txt` (merged rules)
- `app/build/outputs/mapping/release/usage.txt` (removed code)

Upload mapping to Firebase Crashlytics (Gradle plugin handles when configured).

### Phase 3 — Automated verification

| Check | Command / test |
|-------|----------------|
| Unit tests (debug) | `./gradlew :app:testDebugUnitTest` — expect **494** pass |
| Hybrid lockdown | `HybridPipelineLockdownTest` — manifest SHA-256 unchanged |
| Protected file integrity | No edits to 14 manifest paths |
| Cloud Functions tests | `functions/` test suite (32 tests per prior baseline) |

**Note:** Unit tests run against debug builds; R8 issues appear only in **release** builds. Add release smoke tests (manual or Firebase Test Lab).

### Phase 4 — Release smoke test checklist

| # | Scenario | Pass criteria |
|---|----------|---------------|
| 1 | Cold start → Splash (5s) → Intro → Auth | No crash; screens render |
| 2 | Scan EOB (camera) | Hybrid extraction; record in history |
| 3 | Tax Vault → Add receipt | OCR + upload + thumbnail in vault |
| 4 | Insurance news hub | RSS articles load |
| 5 | Appeal generator | Letter generates from selected EOB |
| 6 | Paywall / Gold gate | RevenueCat + Play Billing flow opens |
| 7 | FSA profile + schedule | Worker enqueues without crash |
| 8 | Firestore sync | Profile / records persist across restart |
| 9 | Crashlytics | Test crash deobfuscates with mapping file |

### Phase 5 — Optional hardening

- Add a **release-only** instrumented test lane (Firebase Test Lab).
- Add `minifyEnabled` CI job that builds `assembleRelease` on every main merge.
- Consider `-printconfiguration` diff review when upgrading AGP or Firebase BOM.

---

## 9. What Stays Unchanged

Per owner constraints and architectural analysis:

| Asset | Change required for R8? |
|-------|-------------------------|
| `EobViewModel.kt` | **No** |
| 14 hybrid-pipeline manifest files | **No** |
| `SplashScreen.kt`, `EobSplashLogo.kt`, `IntroScreen.kt` | **No** |
| `functions/index.js` and lib/*.js | **No** (server-side) |
| Navigation structure / MVVM layering | **No** |
| Veryfi callable contract / base64 stream | **No** |
| Feature set (bento hub, tax vault, appeals, news, billing) | **No** |

Only **build configuration** and **ProGuard rules** change.

---

## 10. Known Risks & Mitigations

### 10.1 Gson field stripping (Medium)

**Symptom:** Veryfi nested claims or RSS items parse with null fields.  
**Mitigation:** Section 6.2 keep rules; verify with `VeryfiInsuranceEobMapperTest`, `RssNewsMapperTest`, and live news fetch smoke test.

### 10.2 WorkManager worker removal (Medium)

**Symptom:** FSA notifications never fire.  
**Mitigation:** Section 6.4 worker keeps; verify `FsaDoomsdayScheduler` enqueue after profile save.

### 10.3 Resource shrinking (Low–Medium)

**Symptom:** Missing drawables or strings at runtime.  
**Mitigation:** Enable `isShrinkResources` only after code shrinking is stable; test all hub bento icons and tax vault assets.

### 10.4 Obfuscated stack traces (Operational)

**Symptom:** Unreadable Crashlytics reports.  
**Mitigation:** Upload `mapping.txt` per release; keep `-keepattributes SourceFile,LineNumberTable`.

### 10.5 Play Console pre-launch report

**Symptom:** R8-specific crashes on obscure devices.  
**Mitigation:** Run internal testing track with R8 build before production rollout.

---

## 11. Dependencies & Consumer Rules Inventory

| Dependency | Version | R8 consumer rules |
|------------|---------|-------------------|
| Android Gradle Plugin | 9.2.0 | R8 built-in |
| Firebase BOM | 34.13.0 | Yes (per artifact) |
| Play Billing | 8.3.0 | Yes |
| RevenueCat Purchases | 10.8.0 | Yes |
| Retrofit + Gson converter | 2.9.0 | Partial — add interface keeps |
| CameraX | 1.5.1 | Yes |
| ML Kit text recognition | 16.0.1 | Yes |
| WorkManager | 2.10.1 | Worker keeps recommended |
| Coil | 2.7.0 | Minimal |
| Navigation Compose | 2.9.8 | Yes |
| Compose BOM | 2026.02.01 | Compiler handles |

---

## 12. Expected Benefits

With R8 enabled on a typical Compose + Firebase app of this size:

- **APK/AAB size reduction:** Often 25–45% smaller DEX footprint (exact figure requires Phase 0 baseline build).
- **Dead code removal:** Unused Retrofit paths, test-only utilities stripped from release.
- **Obfuscation:** Harder reverse-engineering of billing and feature-gate logic (not a substitute for server-side security).
- **Performance:** Minor startup improvement from smaller DEX load.

Security note: R8 does **not** replace the existing hybrid pipeline security model (zero client Veryfi credentials, Firebase Auth callable gate, Firestore sanitization barrier).

---

## 13. Conclusion

**R8 implementation is feasible and compatible with the EOBme architecture.**

- The **Veryfi/Firebase hybrid pipeline** and **vault receipt pipeline** can remain **fully intact** without modifying protected source files or `EobViewModel.kt`.
- Implementation is isolated to **release build flags** and **`proguard-rules.pro`**.
- Primary engineering effort is **validation** (release smoke tests + Crashlytics mapping), not architectural refactoring.
- **494** existing unit tests and **HybridPipelineLockdownTest** continue to guard logic and manifest integrity; supplemental **release-build** smoke tests are required because R8 only affects release artifacts.

---

## Appendix A — Protected File Paths (SHA-256 locked)

```
app/src/main/java/app/eob/me/data/DocumentScanPipelineRepository.kt
app/src/main/java/app/eob/me/network/VeryfiDocumentClient.kt
app/src/main/java/app/eob/me/data/VeryfiAnyDocRepository.kt
app/src/main/java/app/eob/me/data/FirebaseEobMapper.kt
app/src/main/java/app/eob/me/data/HybridDocumentRef.kt
app/src/main/java/app/eob/me/network/VeryfiAnyDocConstants.kt
app/src/main/java/app/eob/me/util/OcrProcessor.kt
app/src/main/java/app/eob/me/util/EobDocumentOcrPreCheck.kt
functions/index.js
functions/lib/veryfiAnyDocClient.js
functions/lib/hybridReconciliation.js
functions/lib/veryfiAnyDocConstants.js
functions/lib/veryfiInsuranceEobNormalizer.js
functions/lib/eobNormalizer.js
```

## Appendix B — Hybrid Callable Payload Contract (unchanged by R8)

| Key | Source |
|-----|--------|
| `fileBase64` | Base64.NO_WRAP encoded bytes |
| `fileName` | `HybridDocumentRef.fileNameForUpload` |
| `contentType` | ContentResolver MIME |
| `documentRefId` | `HybridDocumentRef.documentRefId` |
| `blueprintName` | `health_insurance_eob` |
| `documentType` | EOB constant |
| `categories` | Insurance categories constant |
| Callable | `extractVeryfiHybridStream` |

## Appendix C — Document Conversion to PDF

This file is Markdown. To produce a PDF locally:

```bash
pandoc docs/R8_IMPLEMENTATION_REVIEW_EOBME.md -o docs/R8_IMPLEMENTATION_REVIEW_EOBME.pdf --pdf-engine=pdflatex
```

Or open in any Markdown viewer / IDE and export to PDF.

---

*End of review. No application source code was modified in the preparation of this document.*
