package app.eob.me.network

import app.eob.me.data.EobRecord
import app.eob.me.data.FirebaseEobMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
/**
 * Awaits server-side Veryfi extraction triggered by Firebase Storage uploads.
 * Veryfi API credentials remain in Cloud Functions; this client observes Firestore results.
 */
class VeryfiDocumentClient(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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
        const val DEFAULT_TIMEOUT_MS = 120_000L
    }
}
