package app.eob.me.network

import android.util.Base64
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobRecord
import app.eob.me.network.VeryfiAnyDocConstants
import app.eob.me.network.VeryfiAnyDocMapper
import app.eob.me.data.FirebaseEobMapper
import app.eob.me.data.HybridDocumentRef
import app.eob.me.data.VeryfiStreamExtraction
import com.google.firebase.firestore.DocumentReference
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
import java.util.concurrent.TimeUnit
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
        val response = withTimeout(VeryfiAnyDocConstants.HYBRID_STREAM_TIMEOUT_SECONDS * 1_000) {
            suspendCancellableCoroutine<Map<String, Any?>> { continuation ->
                val callable = functions.getHttpsCallable(EXTRACT_VERYFI_HYBRID_STREAM)
                    .withTimeout(VeryfiAnyDocConstants.HYBRID_STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                val payload = hashMapOf(
                    "fileBase64" to encoded,
                    "fileName" to fileName,
                    "contentType" to contentType,
                    "documentRefId" to documentRefId,
                    "blueprintName" to VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB
                )
                val task = callable.call(payload)
                task.addOnSuccessListener { result ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    val data = result.data as? Map<*, *> ?: emptyMap<Any?, Any?>()
                    continuation.resume(data.entries.associate { (key, value) -> key.toString() to value })
                }
                task.addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException(VeryfiHybridStreamErrorMapper.describe(error), error)
                        )
                    }
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        return response["veryfi"] as? Map<String, Any?> ?: response
    }

    /**
     * Commits the immediate (Track B) Veryfi stream extraction straight to Firestore as an
     * authoritative EOB document. Both hybrid tracks target [HybridDocumentRef.stableDocumentId] so
     * the later Storage-triggered backend write merges into the same document instead of duplicating.
     * Returns the parsed [EobRecord] so the upload resolves without waiting on the backend trigger.
     */
    suspend fun writeReconciliationFindings(
        userId: String,
        extraction: VeryfiStreamExtraction,
        sourceName: String = "Veryfi"
    ): EobRecord {
        if (userId.isBlank() || extraction.documentRefId.isBlank()) {
            throw IllegalArgumentException("User id and document reference id are required.")
        }
        val storagePath = HybridDocumentRef.normalizeStoragePath(extraction.sourceFilePath)
        val record = veryfiPayloadToEobRecord(
            payload = extraction.payload,
            documentRefId = extraction.documentRefId,
            sourceName = sourceName
        )
        val reconciliationPayload = buildReconciliationPayload(record, extraction.payload, storagePath)
        val userRef = firestore.collection(USERS).document(userId)
        setMergeAwait(userRef.collection(EOBS).document(record.firestoreId), reconciliationPayload)
        setMergeAwait(userRef.collection(EOB_RECORDS).document(record.firestoreId), reconciliationPayload)
        return record
    }

    /**
     * Synchronous barrier after Firebase Storage upload completes: persists the download URL and
     * marks hybrid reconciliation as fully reconciled so the Storage trigger skips duplicate Veryfi.
     */
    suspend fun finalizeHybridReconciliation(
        userId: String,
        record: EobRecord,
        downloadUrl: String,
        storagePath: String
    ) {
        if (userId.isBlank() || record.firestoreId.isBlank()) {
            throw IllegalArgumentException("User id and Firestore document id are required.")
        }
        if (downloadUrl.isBlank()) {
            throw IllegalArgumentException("Storage download URL is required to finalize hybrid reconciliation.")
        }
        val normalizedPath = HybridDocumentRef.normalizeStoragePath(storagePath)
        val finalizePayload = mapOf(
            "storageDownloadUrl" to downloadUrl,
            "sourceFilePath" to normalizedPath,
            "hybridReconciliationStatus" to "reconciled",
            "processedByStorageUpload" to true,
            "storageUploadConfirmedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val userRef = firestore.collection(USERS).document(userId)
        setMergeAwait(userRef.collection(EOBS).document(record.firestoreId), finalizePayload)
        setMergeAwait(userRef.collection(EOB_RECORDS).document(record.firestoreId), finalizePayload)
    }

    private suspend fun setMergeAwait(
        reference: DocumentReference,
        payload: Map<String, Any?>
    ) {
        suspendCancellableCoroutine { continuation ->
            reference.set(payload, SetOptions.merge())
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
                            val sourcePath = data.veryfiStringField("sourceFilePath", "source_file_path")
                            val processedBy = data.veryfiStringField("processedBy", "processed_by")
                            val normalizedTarget = HybridDocumentRef.normalizeStoragePath(storagePath)
                            if (
                                HybridDocumentRef.normalizeStoragePath(sourcePath) == normalizedTarget &&
                                processedBy.equals("veryfi", ignoreCase = true)
                            ) {
                                trySend(FirebaseEobMapper.eobFromMap(data, document.id))
                            }
                        }
                    }
                awaitClose { registration?.remove() }
            }.first()
        }
    }

    private fun buildReconciliationPayload(
        record: EobRecord,
        rawPayload: Map<String, Any?>,
        storagePath: String
    ): Map<String, Any?> {
        return FirebaseEobMapper.eobToMap(record) + mapOf(
            "sourceFilePath" to storagePath,
            "processedBy" to "veryfi",
            "processedAt" to FieldValue.serverTimestamp(),
            "processedByClientStream" to "veryfi_hybrid",
            "hybridValidationTrack" to "client_stream",
            "hybridReconciliationStatus" to "client_stream_committed",
            "veryfiClientStream" to sanitizeForFirestore(rawPayload),
            "veryfiClientStreamAt" to FieldValue.serverTimestamp(),
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

    private companion object {
        const val USERS = "users"
        const val EOBS = "eobs"
        const val EOB_RECORDS = "eob_records"
        const val EXTRACT_VERYFI_HYBRID_STREAM = VeryfiAnyDocConstants.EXTRACT_VERYFI_HYBRID_STREAM
        const val DEFAULT_TIMEOUT_MS = 120_000L
    }
}

/**
 * Mirrors the backend `veryfiToEobDocument` normalizer so the on-device stream produces an EOB
 * identical to the Storage-triggered Cloud Function. Field resolution and the shared document id
 * keep both hybrid tracks in lockstep. Kept top-level/internal so it is unit-testable without a
 * live Firebase instance.
 */
internal fun veryfiPayloadToEobRecord(
    payload: Map<String, Any?>,
    documentRefId: String,
    sourceName: String
): EobRecord {
    val mergedPayload = VeryfiAnyDocMapper.mergePayloadWithEobFields(payload, documentRefId)
    val ocrText = mergedPayload.veryfiStringField("ocr_text", "ocrText", "text")
    val rawText = ocrText.ifBlank { veryfiPayloadToJsonString(mergedPayload) }
    val providerName = mergedPayload.veryfiStringField("provider_name", "vendor_name")
        .ifBlank { (mergedPayload["vendor"] as? Map<*, *>)?.get("name")?.toString()?.trim().orEmpty() }
        .ifBlank { EobAnalyzer.findProviderName(rawText) }
    val insuranceName = mergedPayload.veryfiStringField(
        "insurance_company_name",
        "insurance_company",
        "insurance_name",
        "payer_name",
        "insurance"
    ).ifBlank { EobAnalyzer.findInsuranceName(rawText) }
    val documentId = HybridDocumentRef.stableDocumentId(documentRefId)
    val normalizedFields = mapOf(
        "id" to documentId,
        "sourceName" to sourceName.ifBlank { "Veryfi" },
        "provider_name" to providerName,
        "insurance_name" to insuranceName,
        "date_of_service" to mergedPayload.veryfiStringField("date_of_service", "service_date", "date"),
        "billed_amount" to mergedPayload.veryfiNumberField("billed_amount", "total_amount_billed", "total", "subtotal"),
        "insurance_paid" to mergedPayload.veryfiNumberField("insurance_paid", "amount_paid", "payment"),
        "contractual_adj" to mergedPayload.veryfiNumberField("contractual_adj", "contractual_adjustment", "discount"),
        "copay" to mergedPayload.veryfiNumberField("copay", "co_pay"),
        "deductible" to mergedPayload.veryfiNumberField("deductible"),
        "coinsurance" to mergedPayload.veryfiNumberField("coinsurance"),
        "patient_responsibility" to mergedPayload.veryfiNumberField(
            "patient_responsibility",
            "patientResponsibility"
        ),
        "member_name" to mergedPayload.veryfiStringField("member_name"),
        "member_id" to mergedPayload.veryfiStringField("member_id", "member_number"),
        "patient_name" to mergedPayload.veryfiStringField("patient_name"),
        "claim_id" to mergedPayload.veryfiStringField("claim_id", "claim_number"),
        "in_network_out_of_pocket_balance" to mergedPayload.veryfiNumberField(
            "in_network_out_of_pocket_balance",
            "in_network_out_of_pocket"
        ),
        "out_of_network_out_of_pocket_balance" to mergedPayload.veryfiNumberField(
            "out_of_network_out_of_pocket_balance",
            "out_of_network_out_of_pocket"
        ),
        "blueprint_name" to mergedPayload.veryfiStringField("blueprint_name")
            .ifBlank { VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB },
        "cptCodes" to veryfiExtractCptCodes(mergedPayload),
        "rawText" to rawText
    )
    return FirebaseEobMapper.eobFromMap(normalizedFields, documentId)
}

private fun veryfiExtractCptCodes(payload: Map<String, Any?>): String {
    val explicit = listOf("cptCodes", "cpt_codes", "cptCode", "cpt_code")
        .firstNotNullOfOrNull { key -> payload[key] }
    val explicitText = when (explicit) {
        is String -> explicit
        is List<*> -> explicit.mapNotNull { it?.toString() }.joinToString(",")
        else -> ""
    }
    val lineItems = payload["line_items"] ?: payload["lineItems"]
    val lineItemText = when (lineItems) {
        is String -> lineItems
        is List<*> -> lineItems.joinToString(" ") { item ->
            when (item) {
                is Map<*, *> -> listOfNotNull(
                    item["description"]?.toString(),
                    item["cpt_code"]?.toString(),
                    item["cptCode"]?.toString()
                ).joinToString(" ")
                else -> item?.toString().orEmpty()
            }
        }
        else -> ""
    }
    val fromLineItems = EobAnalyzer.validCptCodes(lineItemText).joinToString(",")
    return listOf(explicitText, fromLineItems)
        .filter { it.isNotBlank() }
        .joinToString(",")
}

private fun veryfiPayloadToJsonString(value: Any?): String {
    return when (value) {
        null -> "null"
        is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        is Boolean, is Number -> value.toString()
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { (key, nestedValue) ->
            "\"${key.toString().replace("\"", "\\\"")}\":${veryfiPayloadToJsonString(nestedValue)}"
        }
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { veryfiPayloadToJsonString(it) }
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { veryfiPayloadToJsonString(it) }
        else -> "\"${value.toString().replace("\"", "\\\"")}\""
    }
}

private fun Map<String, Any?>.veryfiStringField(vararg keys: String): String {
    keys.forEach { key ->
        val value = this[key]?.toString()?.trim()
        if (!value.isNullOrBlank()) return value
    }
    return ""
}

private fun Map<String, Any?>.veryfiNumberField(vararg keys: String): Double {
    keys.forEach { key ->
        when (val value = this[key]) {
            is Number -> return value.toDouble()
            is String -> value.replace("$", "").replace(",", "").toDoubleOrNull()?.let { return it }
            else -> Unit
        }
    }
    return 0.0
}
