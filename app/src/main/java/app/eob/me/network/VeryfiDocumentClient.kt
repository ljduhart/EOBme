package app.eob.me.network

import android.util.Base64
import app.eob.me.data.EobRecord
import app.eob.me.data.FirebaseEobMapper
import app.eob.me.data.VeryfiStreamExtraction
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Hybrid validation client:
 * - Track B: immediate Veryfi extraction via authenticated Cloud Function proxy.
 * - Track A reconciliation: awaits server-side Storage-triggered analysis results.
 *
 * Veryfi API credentials remain in Cloud Functions; this client never embeds secrets.
 */
class VeryfiDocumentClient(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    suspend fun extractDocument(
        userId: String,
        storagePath: String,
        downloadUrl: String,
        localFileBytes: ByteArray? = null
    ): EobRecord {
        if (userId.isBlank()) {
            throw IllegalArgumentException("User id is required for Veryfi extraction.")
        }
        if (storagePath.isBlank()) {
            throw IllegalArgumentException("Storage path is required for Veryfi extraction.")
        }
        if (downloadUrl.isBlank() && localFileBytes == null) {
            throw IllegalArgumentException("Download URL or local file bytes are required.")
        }
        return awaitVeryfiExtraction(userId = userId, storagePath = storagePath)
    }

    suspend fun streamExtractDocument(
        userId: String,
        documentRefId: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String
    ): Map<String, Any?> {
        if (userId.isBlank()) {
            throw IllegalArgumentException("User id is required for Veryfi stream extraction.")
        }
        if (documentRefId.isBlank()) {
            throw IllegalArgumentException("Document reference id is required.")
        }
        if (fileBytes.isEmpty()) {
            throw IllegalArgumentException("Document bytes are required for Veryfi stream extraction.")
        }
        if (fileName.isBlank()) {
            throw IllegalArgumentException("File name is required for Veryfi stream extraction.")
        }

        val encoded = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
        val response = suspendCancellableCoroutine<Map<String, Any?>> { continuation ->
            val callable = functions.getHttpsCallable(EXTRACT_VERYFI_HYBRID_STREAM)
            val payload = hashMapOf(
                "fileBase64" to encoded,
                "fileName" to fileName,
                "contentType" to contentType,
                "documentRefId" to documentRefId
            )
            val task = callable.call(payload)
            task.addOnSuccessListener { result ->
                if (!continuation.isActive) return@addOnSuccessListener
                val data = result.data as? Map<*, *> ?: emptyMap<Any?, Any?>()
                continuation.resume(data.entries.associate { (key, value) -> key.toString() to value })
            }
            task.addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return response["veryfi"] as? Map<String, Any?> ?: response
    }

    suspend fun writeReconciliationFindings(
        userId: String,
        extraction: VeryfiStreamExtraction
    ) {
        if (userId.isBlank() || extraction.documentRefId.isBlank()) {
            throw IllegalArgumentException("User id and document reference id are required.")
        }
        val reconciliationPayload = buildReconciliationPayload(extraction)
        val userRef = firestore.collection(USERS).document(userId)
        suspendCancellableCoroutine { continuation ->
            userRef.collection(EOBS)
                .document(extraction.documentRefId)
                .set(reconciliationPayload, SetOptions.merge())
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
        suspendCancellableCoroutine { continuation ->
            userRef.collection(EOB_RECORDS)
                .document(extraction.documentRefId)
                .set(reconciliationPayload, SetOptions.merge())
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
    }

    suspend fun awaitVeryfiExtraction(
        userId: String,
        storagePath: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): EobRecord {
        return withTimeout(timeoutMs) {
            callbackFlow {
                var registration: ListenerRegistration? = null
                registration = firestore.collection(USERS)
                    .document(userId)
                    .collection(EOBS)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        snapshot?.documents.orEmpty().forEach { document ->
                            val data = document.data ?: return@forEach
                            val sourcePath = data.stringField("sourceFilePath", "source_file_path")
                            val processedBy = data.stringField("processedBy", "processed_by")
                            if (sourcePath == storagePath && processedBy.equals("veryfi", ignoreCase = true)) {
                                trySend(FirebaseEobMapper.eobFromMap(data, document.id))
                            }
                        }
                    }
                awaitClose { registration?.remove() }
            }.first()
        }
    }

    private fun buildReconciliationPayload(extraction: VeryfiStreamExtraction): Map<String, Any?> {
        return mapOf(
            "veryfiClientStream" to sanitizeForFirestore(extraction.payload),
            "veryfiClientStreamAt" to FieldValue.serverTimestamp(),
            "sourceFilePath" to extraction.sourceFilePath,
            "hybridValidationTrack" to "client_stream",
            "hybridReconciliationStatus" to "pending_backend_review",
            "processedByClientStream" to "veryfi_hybrid",
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun sanitizeForFirestore(value: Any?): Any? {
        return when (value) {
            null -> null
            is String, is Boolean, is Long, is Int, is Double, is Float -> value
            is Map<*, *> -> value.entries
                .mapNotNull { (key, nestedValue) ->
                    val fieldName = key as? String ?: return@mapNotNull null
                    fieldName to sanitizeForFirestore(nestedValue)
                }
                .toMap()
            is List<*> -> value.map { sanitizeForFirestore(it) }
            else -> value.toString()
        }
    }

    private fun Map<String, Any?>.stringField(vararg keys: String): String {
        keys.forEach { key ->
            val value = this[key] as? String
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }

    private companion object {
        const val USERS = "users"
        const val EOBS = "eobs"
        const val EOB_RECORDS = "eob_records"
        const val EXTRACT_VERYFI_HYBRID_STREAM = "extractVeryfiHybridStream"
        const val DEFAULT_TIMEOUT_MS = 120_000L
    }
}
