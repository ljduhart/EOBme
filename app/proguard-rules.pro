# =============================================================================
# EOBme (app.eob.me) — R8 / ProGuard release rules
# Hybrid Veryfi stream + Firebase Firestore/Auth + Gson RSS/AnyDocs DTOs
# UI and business logic are obfuscated; network/cloud model names are preserved.
# =============================================================================

# -----------------------------------------------------------------------------
# Global optimization & diagnostics
# -----------------------------------------------------------------------------
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# -----------------------------------------------------------------------------
# Kotlin runtime & coroutines
# -----------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# -----------------------------------------------------------------------------
# @Keep annotation shield (androidx.annotation.Keep)
# -----------------------------------------------------------------------------
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# -----------------------------------------------------------------------------
# Gson serialization — preserve @SerializedName fields for JSON mapping
# -----------------------------------------------------------------------------
-keepattributes Signature
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# -----------------------------------------------------------------------------
# Veryfi AnyDocs + nested insurance EOB Gson DTOs (names + members preserved)
# Package: app.eob.me.network / app.eob.me.network.dto
# -----------------------------------------------------------------------------
-keep class app.eob.me.network.VeryfiAnyDocResponseDto { *; }
-keep class app.eob.me.network.VeryfiAnyDocLineItemDto { *; }
-keep class app.eob.me.network.VeryfiAnyDocRequestDto { *; }
-keep class app.eob.me.network.dto.VeryfiInsuranceEobResponseDto { *; }
-keep class app.eob.me.network.dto.VeryfiInsuranceClaimDto { *; }

# -----------------------------------------------------------------------------
# Insurance News RSS Gson models (RetrofitClient / NewsApiService)
# -----------------------------------------------------------------------------
-keep class app.eob.me.network.RssResponse { *; }
-keep class app.eob.me.network.FeedInfo { *; }
-keep class app.eob.me.network.RssItem { *; }

# -----------------------------------------------------------------------------
# Retrofit service contracts
# -----------------------------------------------------------------------------
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keep interface app.eob.me.network.NewsApiService { *; }
-keep interface app.eob.me.network.VeryfiAnyDocApiService { *; }
-keep class app.eob.me.network.RetrofitClient { *; }
-keep class app.eob.me.network.VeryfiApiClient { *; }

# -----------------------------------------------------------------------------
# Firebase domain / cloud models — preserve class names (Firestore map keys are literals)
# Package: app.eob.me.data
# -----------------------------------------------------------------------------
-keepnames class app.eob.me.data.EobRecord { *; }
-keepnames class app.eob.me.data.EobCharge { *; }
-keepnames class app.eob.me.data.ReceiptRecord { *; }
-keepnames class app.eob.me.data.ServiceLine { *; }
-keepnames class app.eob.me.data.InsuranceClaimTotals { *; }
-keepnames class app.eob.me.data.InsuranceClaim { *; }
-keepnames class app.eob.me.data.NormalizedInsuranceEob { *; }
-keepnames class app.eob.me.data.NormalizedInsuranceEobResult { *; }
-keepnames class app.eob.me.data.VeryfiHealthInsuranceEob { *; }
-keepnames class app.eob.me.data.VeryfiExtractedData { *; }
-keepnames class app.eob.me.data.VeryfiAnyDocExtractionResult { *; }
-keepnames class app.eob.me.data.VeryfiStreamExtraction { *; }
-keepnames class app.eob.me.data.DocumentUploadResult { *; }
-keepnames class app.eob.me.data.EobProcessedResult { *; }
-keepnames class app.eob.me.data.UserProfile { *; }
-keepnames class app.eob.me.data.FirebaseSyncStatus { *; }

# -----------------------------------------------------------------------------
# Hybrid pipeline orchestration (protected manifest files — direct Kotlin refs)
# -----------------------------------------------------------------------------
-keepnames class app.eob.me.data.DocumentScanPipelineRepository { *; }
-keepnames class app.eob.me.data.VeryfiAnyDocRepository { *; }
-keepnames class app.eob.me.data.FirebaseEobMapper { *; }
-keepnames class app.eob.me.data.HybridDocumentRef { *; }
-keepnames class app.eob.me.network.VeryfiDocumentClient { *; }
-keepnames class app.eob.me.network.VeryfiAnyDocConstants { *; }
-keepnames class app.eob.me.util.OcrProcessor { *; }
-keepnames class app.eob.me.util.EobDocumentOcrPreCheck { *; }
-keepnames class app.eob.me.data.VaultReceiptMapper { *; }

# Veryfi mappers & parsers used by nested claims pipeline
-keepnames class app.eob.me.network.VeryfiInsuranceEobMapper { *; }
-keep class app.eob.me.network.VeryfiInsuranceEobPayloadParser { *; }
-keepnames class app.eob.me.network.VeryfiAnyDocMapper { *; }
-keepnames class app.eob.me.network.VeryfiIndexedFieldReader { *; }
-keepnames class app.eob.me.network.VeryfiOcrFieldExtractor { *; }
-keepnames class app.eob.me.network.VeryfiHybridStreamErrorMapper { *; }
-keepnames class app.eob.me.data.InsuranceEobRecordBridge { *; }

# -----------------------------------------------------------------------------
# Enums — prevent runtime name/valueOf crashes (CameraScanDocumentType, FsaDoomsdayPhase, etc.)
# -----------------------------------------------------------------------------
-keepclassmembers enum app.eob.me.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# -----------------------------------------------------------------------------
# Parcelable & Serializable
# -----------------------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# -----------------------------------------------------------------------------
# WorkManager — FSA doomsday periodic notifications
# -----------------------------------------------------------------------------
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class app.eob.me.work.FsaDoomsdayNotificationWorker { *; }
-keep class app.eob.me.work.FsaDoomsdayScheduler { *; }

# -----------------------------------------------------------------------------
# Application entry points & Firebase messaging
# -----------------------------------------------------------------------------
-keep class app.eob.me.EobApplication { *; }
-keep class app.eob.me.MainActivity { *; }
-keep class app.eob.me.data.EobFirebaseMessagingService { *; }

# -----------------------------------------------------------------------------
# ViewModels (referenced by Compose viewModel() factory)
# -----------------------------------------------------------------------------
-keep class app.eob.me.viewmodel.EobViewModel { *; }
-keep class app.eob.me.viewmodel.AppViewModel { *; }
-keep class app.eob.me.viewmodel.SubscriptionViewModel { *; }
-keep class app.eob.me.viewmodel.CameraCaptureViewModel { *; }

# -----------------------------------------------------------------------------
# Google Firebase SDK — suppress warnings; consumer rules ship with firebase-bom
# -----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# -----------------------------------------------------------------------------
# Veryfi SDK defensive shield (no client SDK today; guards future integrations)
# -----------------------------------------------------------------------------
-keep class com.veryfi.** { *; }
-dontwarn com.veryfi.**

# -----------------------------------------------------------------------------
# RevenueCat Purchases SDK (billing — consumer rules also merged from AAR)
# -----------------------------------------------------------------------------
-keep class com.revenuecat.purchases.** { *; }
-dontwarn com.revenuecat.purchases.**

# -----------------------------------------------------------------------------
# Google Play Billing Library 8.x
# -----------------------------------------------------------------------------
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# -----------------------------------------------------------------------------
# CameraX & ML Kit (document scan / OCR)
# -----------------------------------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# -----------------------------------------------------------------------------
# Jetpack Compose & Navigation (compiler-generated; no Composable keep required)
# -----------------------------------------------------------------------------
-dontwarn androidx.compose.**

# -----------------------------------------------------------------------------
# Coil image loading (Tax Vault evidence thumbnails)
# -----------------------------------------------------------------------------
-dontwarn coil.**

# -----------------------------------------------------------------------------
# OkHttp (Retrofit transport)
# -----------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**

# -----------------------------------------------------------------------------
# R8 full-mode Kotlin metadata
# -----------------------------------------------------------------------------
-if class androidx.compose.runtime.Composable
-keep class androidx.compose.runtime.** { *; }
