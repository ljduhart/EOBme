package app.eob.me.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

data class FirebaseSyncStatus(
    val isConfigured: Boolean,
    val userId: String = "",
    val message: String = "Firebase not connected yet."
)

class FirebaseEobRepository(private val context: Context) {
    private val configured: Boolean by lazy { ensureConfigured() }

    fun status(): FirebaseSyncStatus {
        return if (configured) {
            FirebaseSyncStatus(
                isConfigured = true,
                userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                message = "Firebase is configured. Sign in to sync EOBme data."
            )
        } else {
            FirebaseSyncStatus(
                isConfigured = false,
                message = "Add app/google-services.json to enable live Firebase sync."
            )
        }
    }

    fun signInOrCreate(
        profile: UserProfile,
        credentials: RegistrationCredentials,
        onResult: (FirebaseSyncStatus) -> Unit
    ) {
        if (!configured) {
            onResult(status())
            return
        }
        if (credentials.email.isBlank() || credentials.password.isBlank()) {
            onResult(FirebaseSyncStatus(true, message = "Enter email and password to sync with Firebase."))
            return
        }

        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(credentials.email, credentials.password)
            .addOnSuccessListener {
                val userId = it.user?.uid.orEmpty()
                saveProfile(userId, profile) {}
                registerMessagingToken(userId)
                onResult(FirebaseSyncStatus(true, userId, "Firebase sync is active."))
            }
            .addOnFailureListener {
                auth.createUserWithEmailAndPassword(credentials.email, credentials.password)
                    .addOnSuccessListener { result ->
                        val userId = result.user?.uid.orEmpty()
                        saveProfile(userId, profile) {}
                        registerMessagingToken(userId)
                        onResult(FirebaseSyncStatus(true, userId, "Firebase account created and sync is active."))
                    }
                    .addOnFailureListener { createError ->
                        onResult(FirebaseSyncStatus(true, message = "Firebase sign-in failed: ${createError.localizedMessage}"))
                    }
            }
    }

    fun signIn(email: String, password: String, onResult: (FirebaseSyncStatus) -> Unit) {
        if (!configured) {
            onResult(status())
            return
        }
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val userId = it.user?.uid.orEmpty()
                registerMessagingToken(userId)
                onResult(FirebaseSyncStatus(true, userId, "Firebase sync is active."))
            }
            .addOnFailureListener {
                onResult(FirebaseSyncStatus(true, message = "Firebase sign-in failed: ${it.localizedMessage}"))
            }
    }

    fun sendPasswordReset(email: String, onResult: (String) -> Unit) {
        if (!configured) {
            onResult("Firebase is not configured.")
            return
        }
        if (email.isBlank()) {
            onResult("Enter your email address first.")
            return
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener { onResult("Password reset email sent if the account exists.") }
            .addOnFailureListener { onResult("Password reset failed: ${it.localizedMessage}") }
    }

    fun createAccount(
        profile: UserProfile,
        credentials: RegistrationCredentials,
        onResult: (FirebaseSyncStatus) -> Unit
    ) {
        if (!configured) {
            onResult(status())
            return
        }
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(credentials.email, credentials.password)
            .addOnSuccessListener {
                val userId = it.user?.uid.orEmpty()
                saveProfile(userId, profile) {}
                registerMessagingToken(userId)
                onResult(FirebaseSyncStatus(true, userId, "Firebase account created and sync is active."))
            }
            .addOnFailureListener {
                onResult(FirebaseSyncStatus(true, message = "Firebase account creation failed: ${it.localizedMessage}"))
            }
    }

    fun signOut() {
        if (configured) FirebaseAuth.getInstance().signOut()
    }

    fun deleteAccount(userId: String, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) {
            onComplete("Please sign in to delete your account.")
            return
        }
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser == null || authUser.uid != userId) {
            onComplete("No signed-in account to delete.")
            return
        }
        val userRef = firestore().collection(USERS).document(userId)
        deleteSubcollectionDocuments(userRef.collection(EOBS)) { eobError ->
            if (eobError != null) {
                onComplete("Account deletion failed: $eobError")
                return@deleteSubcollectionDocuments
            }
            deleteSubcollectionDocuments(userRef.collection("insurance-cards")) { cardError ->
                if (cardError != null) {
                    onComplete("Account deletion failed: $cardError")
                    return@deleteSubcollectionDocuments
                }
                deleteSubcollectionDocuments(userRef.collection(VAULT_RECEIPTS)) { receiptError ->
                    if (receiptError != null) {
                        onComplete("Account deletion failed: $receiptError")
                        return@deleteSubcollectionDocuments
                    }
                    deleteSubcollectionDocuments(userRef.collection(DEVICES)) { deviceError ->
                        if (deviceError != null) {
                            onComplete("Account deletion failed: $deviceError")
                            return@deleteSubcollectionDocuments
                        }
                        userRef.delete()
                            .addOnSuccessListener {
                                authUser.delete()
                                    .addOnSuccessListener { onComplete("Account deleted.") }
                                    .addOnFailureListener { error ->
                                        onComplete("Auth account deletion failed: ${error.localizedMessage}")
                                    }
                            }
                            .addOnFailureListener { error ->
                                onComplete("Firestore account deletion failed: ${error.localizedMessage}")
                            }
                    }
                }
            }
        }
    }

    private fun deleteSubcollectionDocuments(
        collectionRef: com.google.firebase.firestore.CollectionReference,
        onComplete: (String?) -> Unit
    ) {
        collectionRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onComplete(null)
                    return@addOnSuccessListener
                }
                val batch = firestore().batch()
                snapshot.documents.forEach { document -> batch.delete(document.reference) }
                batch.commit()
                    .addOnSuccessListener { onComplete(null) }
                    .addOnFailureListener { error -> onComplete(error.localizedMessage) }
            }
            .addOnFailureListener { error -> onComplete(error.localizedMessage) }
    }

    fun observeProfile(
        userId: String,
        onProfile: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        if (!configured || userId.isBlank()) return null
        return firestore().collection(USERS).document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError("Profile sync failed: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                val data = snapshot?.data ?: return@addSnapshotListener
                onProfile(FirebaseEobMapper.profileFromMap(data))
            }
    }

    fun observeEobs(
        userId: String,
        onRecords: (List<EobRecord>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        if (!configured || userId.isBlank()) return null
        return firestore().collection(USERS).document(userId).collection(EOBS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError("EOB sync failed: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                val records = snapshot?.documents
                    ?.mapNotNull { document ->
                        document.data?.let { data -> FirebaseEobMapper.eobFromMap(data, document.id) }
                    }
                    .orEmpty()
                    .sortedByDescending { it.serviceDateSortKey }
                onRecords(records)
            }
    }

    fun observeInsuranceNews(
        onNews: (List<NewsRelease>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        if (!configured) return null
        return firestore().collection(NEWS)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError("News sync failed: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                val news = snapshot?.documents
                    ?.mapNotNull { document -> document.data?.let(FirebaseEobMapper::newsFromMap) }
                    .orEmpty()
                onNews(news)
            }
    }

    fun observeRegionalNews(userState: String): Flow<List<NewsRelease>> = callbackFlow {
        if (!configured || userState.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = firestore().collection(NEWS_RELEASES)
            .whereArrayContainsAny("targetTags", listOf(userState, "National"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val news = snapshot?.documents
                    ?.mapNotNull { document -> document.data?.let(FirebaseEobMapper::newsFromMap) }
                    .orEmpty()
                trySend(news)
            }
        awaitClose { registration.remove() }
    }

    fun saveProfile(userId: String, profile: UserProfile, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) {
            onComplete(if (userId.isBlank()) "Please sign in to save your profile." else status().message)
            return
        }
        firestore().collection(USERS).document(userId)
            .set(FirebaseEobMapper.profileToMap(profile))
            .addOnSuccessListener { onComplete("Profile saved to Firebase.") }
            .addOnFailureListener { onComplete("Profile save failed: ${it.localizedMessage}") }
    }

    fun saveInsuranceCardMetadata(userId: String, profile: UserProfile, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) {
            onComplete("")
            return
        }
        firestore().collection(USERS).document(userId).collection("insurance-cards")
            .document("current")
            .set(
                mapOf(
                    "insuranceName" to profile.insuranceName,
                    "insuranceId" to profile.insuranceId,
                    "groupName" to profile.groupName,
                    "insuranceCardDownloadUrl" to profile.insuranceCardDownloadUrl,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener { onComplete("Insurance card metadata saved.") }
            .addOnFailureListener { onComplete("Insurance card metadata save failed: ${it.localizedMessage}") }
    }

    fun saveEob(userId: String, record: EobRecord, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) {
            onComplete("Please sign in to save EOBs.")
            return
        }
        val payload = FirebaseEobMapper.eobToMap(record)
        val docId = record.firestoreId.takeIf { it.isNotBlank() } ?: record.id.toString()
        firestore().collection(USERS).document(userId).collection(EOBS)
            .document(docId)
            .set(payload)
            .addOnSuccessListener { onComplete("EOB saved to Firebase.") }
            .addOnFailureListener { onComplete("EOB save failed: ${it.localizedMessage}") }
    }

    fun deleteEob(userId: String, record: EobRecord, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) {
            onComplete("Please sign in to delete EOBs.")
            return
        }
        val docId = record.firestoreId.takeIf { it.isNotBlank() } ?: record.id.toString()
        firestore().collection(USERS).document(userId).collection(EOBS).document(docId)
            .delete()
            .addOnSuccessListener { onComplete("EOB deleted.") }
            .addOnFailureListener { onComplete("EOB delete failed: ${it.localizedMessage}") }
    }

    fun uploadEobFile(userId: String, uri: Uri, sourceName: String, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) {
            onComplete("Please sign in before uploading an EOB.")
            return
        }
        val contentType = context.contentResolver.getType(uri)
            ?: if (uri.toString().endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
        val extension = if (contentType == "application/pdf") "pdf" else "jpg"
        val fileName = "eob_${System.currentTimeMillis()}.$extension"
        val ref = FirebaseStorage.getInstance().reference
            .child("users").child(userId).child(HybridDocumentRef.USER_ROOTED_EOB_FOLDER).child(fileName)
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("sourceName", sourceName)
            .build()
        ref.putFile(uri, metadata)
            .addOnSuccessListener { onComplete("EOB uploaded. Veryfi processing started.") }
            .addOnFailureListener { onComplete("EOB upload failed: ${it.localizedMessage}") }
    }

    suspend fun uploadEobFileAwaitDownload(
        userId: String,
        uri: Uri,
        sourceName: String,
        fileName: String? = null
    ): DocumentUploadResult {
        if (!configured || userId.isBlank()) {
            throw IllegalStateException("Please sign in before uploading an EOB.")
        }
        val contentType = context.contentResolver.getType(uri)
            ?: if (uri.toString().endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
        val extension = HybridDocumentRef.extensionForContentType(contentType)
        val resolvedFileName = fileName?.takeIf { it.isNotBlank() }
            ?: HybridDocumentRef.fileNameForUpload(extension)
        val documentRefId = HybridDocumentRef.documentRefId(resolvedFileName)
        val ref = FirebaseStorage.getInstance().reference
            .child("users").child(userId).child(HybridDocumentRef.USER_ROOTED_EOB_FOLDER).child(resolvedFileName)
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("sourceName", sourceName)
            .build()
        return suspendCancellableCoroutine { continuation ->
            val uploadTask = ref.putFile(uri, metadata)
            uploadTask
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception ?: IllegalStateException("EOB upload failed.")
                    }
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUrl ->
                    continuation.resume(
                        DocumentUploadResult(
                            storagePath = HybridDocumentRef.normalizeStoragePath(ref.path),
                            downloadUrl = downloadUrl.toString(),
                            contentType = contentType,
                            fileName = resolvedFileName,
                            documentRefId = documentRefId
                        )
                    )
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            continuation.invokeOnCancellation { uploadTask.cancel() }
        }
    }

    fun uploadEobBitmap(userId: String, bitmap: Bitmap, sourceName: String, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) {
            onComplete("Please sign in before scanning an EOB.")
            return
        }
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        val ref = FirebaseStorage.getInstance().reference
            .child("users").child(userId).child(HybridDocumentRef.USER_ROOTED_EOB_FOLDER)
            .child("eob_${System.currentTimeMillis()}.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("sourceName", sourceName)
            .build()
        ref.putBytes(output.toByteArray(), metadata)
            .addOnSuccessListener { onComplete("EOB uploaded. Veryfi processing started.") }
            .addOnFailureListener { onComplete("EOB upload failed: ${it.localizedMessage}") }
    }

    fun insuranceCardStoragePath(userId: String, fileName: String): String {
        return FirebaseStorage.getInstance()
            .reference
            .child("users/$userId/insurance-cards/$fileName")
            .path
    }

    private fun registerMessagingToken(userId: String) {
        if (!configured || userId.isBlank()) return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                firestore().collection(USERS).document(userId).collection(DEVICES).document(token)
                    .set(mapOf("token" to token, "updatedAt" to System.currentTimeMillis()))
            }
    }

    fun observeVaultReceipts(
        userId: String,
        onReceipts: (List<ReceiptRecord>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        if (!configured || userId.isBlank()) return null
        return firestore().collection(USERS).document(userId).collection(VAULT_RECEIPTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError("Vault receipt sync failed: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                val receipts = snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.data?.let { data ->
                        VaultReceiptMapper.receiptFromMap(data, document.id)
                    }
                }.sortedByDescending { it.createdAtMillis }
                onReceipts(receipts)
            }
    }

    suspend fun uploadVaultReceiptAwaitDownload(
        userId: String,
        uri: Uri,
        sourceName: String
    ): DocumentUploadResult {
        if (!configured || userId.isBlank()) {
            throw IllegalStateException("Please sign in before uploading a vault receipt.")
        }
        val contentType = context.contentResolver.getType(uri)
            ?: if (uri.toString().endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
        val extension = HybridDocumentRef.extensionForContentType(contentType)
        val resolvedFileName = HybridDocumentRef.vaultReceiptFileNameForUpload(extension)
        val ref = FirebaseStorage.getInstance().reference
            .child("users").child(userId).child(HybridDocumentRef.USER_ROOTED_VAULT_RECEIPT_FOLDER)
            .child(resolvedFileName)
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("sourceName", sourceName)
            .setCustomMetadata("scanType", CameraScanDocumentType.Receipt.name)
            .setCustomMetadata("vaultRecord", "true")
            .build()
        return suspendCancellableCoroutine { continuation ->
            ref.putFile(uri, metadata)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception ?: IllegalStateException("Vault receipt upload failed.")
                    }
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUrl ->
                    continuation.resume(
                        DocumentUploadResult(
                            storagePath = HybridDocumentRef.vaultReceiptStoragePath(userId, resolvedFileName),
                            downloadUrl = downloadUrl.toString(),
                            contentType = contentType,
                            fileName = resolvedFileName,
                            documentRefId = HybridDocumentRef.documentRefId(resolvedFileName)
                        )
                    )
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
        }
    }

    fun saveVaultReceipt(userId: String, receipt: ReceiptRecord, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) {
            onComplete("Please sign in to save vault receipts.")
            return
        }
        val docId = receipt.firestoreId.ifBlank { receipt.storagePath.hashCode().toString() }
        firestore().collection(USERS).document(userId).collection(VAULT_RECEIPTS)
            .document(docId)
            .set(VaultReceiptMapper.receiptToMap(receipt))
            .addOnSuccessListener { onComplete("Vault receipt saved.") }
            .addOnFailureListener { onComplete("Vault receipt save failed: ${it.localizedMessage}") }
    }

    private fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun ensureConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
        } catch (_: IllegalStateException) {
            false
        }
    }

    private companion object {
        const val USERS = "users"
        const val EOBS = "eobs"
        const val VAULT_RECEIPTS = "vault_receipts"
        const val EOB_RECORDS = "eob_records"
        const val DEVICES = "devices"
        const val NEWS = "insuranceNews"
        const val NEWS_RELEASES = "news_releases"
    }
}

