package app.eob.me.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream

data class FirebaseSyncStatus(
    val isConfigured: Boolean,
    val userId: String = "",
    val message: String = "Firebase not connected yet.",
    val requiresEmailCodeVerification: Boolean = false
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
        onResult: (FirebaseSyncStatus) -> Unit
    ) {
        if (!configured) {
            onResult(status())
            return
        }
        if (profile.email.isBlank() || profile.password.isBlank()) {
            onResult(FirebaseSyncStatus(true, message = "Enter email and password to sync with Firebase."))
            return
        }

        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(profile.email, profile.password)
            .addOnSuccessListener {
                val userId = it.user?.uid.orEmpty()
                registerMessagingToken(userId)
                onResult(FirebaseSyncStatus(true, userId, "Firebase sync is active."))
            }
            .addOnFailureListener {
                createAccount(profile, onResult)
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

    fun createAccount(profile: UserProfile, onResult: (FirebaseSyncStatus) -> Unit) {
        if (!configured) {
            onResult(status())
            return
        }
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(profile.email, profile.password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid.orEmpty()
                saveProfile(userId, profile.copy(accountSetupVerified = false)) {}
                requestAccountVerificationCode { status ->
                    onResult(
                        FirebaseSyncStatus(
                            isConfigured = true,
                            userId = userId,
                            message = status.message,
                            requiresEmailCodeVerification = true
                        )
                    )
                }
            }
            .addOnFailureListener {
                onResult(FirebaseSyncStatus(true, message = "Firebase account creation failed: ${it.localizedMessage}"))
            }
    }

    fun requestAccountVerificationCode(onResult: (FirebaseSyncStatus) -> Unit) {
        if (!configured) {
            onResult(status())
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(FirebaseSyncStatus(true, message = "Create or sign in to an account before requesting a code."))
            return
        }
        functions().getHttpsCallable(SEND_ACCOUNT_VERIFICATION_CODE)
            .call()
            .addOnSuccessListener {
                onResult(
                    FirebaseSyncStatus(
                        isConfigured = true,
                        userId = user.uid,
                        message = "Verification code sent to ${user.email.orEmpty()}.",
                        requiresEmailCodeVerification = true
                    )
                )
            }
            .addOnFailureListener {
                onResult(FirebaseSyncStatus(true, message = "Verification code send failed: ${it.localizedMessage}"))
            }
    }

    fun verifyAccountCreationCode(code: String, onResult: (FirebaseSyncStatus) -> Unit) {
        if (!configured) {
            onResult(status())
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(FirebaseSyncStatus(true, message = "Sign in before entering the verification code."))
            return
        }
        functions().getHttpsCallable(VERIFY_ACCOUNT_VERIFICATION_CODE)
            .call(mapOf("code" to code.trim()))
            .addOnSuccessListener {
                registerMessagingToken(user.uid)
                onResult(FirebaseSyncStatus(true, user.uid, "Email verified. Account setup is complete."))
            }
            .addOnFailureListener {
                onResult(FirebaseSyncStatus(true, message = "Verification failed: ${it.localizedMessage}"))
            }
    }

    fun signOut() {
        if (configured) FirebaseAuth.getInstance().signOut()
    }

    fun observeProfile(
        userId: String,
        currentPassword: String,
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
                onProfile(FirebaseEobMapper.profileFromMap(data, currentPassword))
            }
    }

    fun loadProfile(userId: String, currentPassword: String, onProfile: (UserProfile) -> Unit) {
        if (!configured || userId.isBlank()) return
        firestore().collection(USERS).document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.data?.let { onProfile(FirebaseEobMapper.profileFromMap(it, currentPassword)) }
            }
    }

    fun observeEobs(
        userId: String,
        onRecords: (List<EobRecord>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        if (!configured || userId.isBlank()) return null
        val recordsByCollection = mutableMapOf<String, List<EobRecord>>()
        fun publishRecords() {
            onRecords(
                recordsByCollection.values
                    .flatten()
                    .distinctBy { "${it.insuranceName}|${it.providerName}|${it.serviceDate}|${it.charges.joinToString { charge -> charge.cptCode }}" }
                    .sortedBy { it.serviceDateSortKey }
            )
        }

        val listeners = listOf(EOBS, EOB_RECORDS).map { collectionName ->
            firestore().collection(USERS).document(userId).collection(collectionName)
                .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError("$collectionName sync failed: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                val records = snapshot?.documents
                    ?.mapNotNull { document -> document.data?.let { data -> FirebaseEobMapper.eobFromMap(data, document.id) } }
                    .orEmpty()
                recordsByCollection[collectionName] = records
                publishRecords()
                }
        }
        return CombinedListenerRegistration(listeners)
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

    fun saveProfile(userId: String, profile: UserProfile, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) return
        firestore().collection(USERS).document(userId)
            .set(FirebaseEobMapper.profileToMap(profile))
            .addOnSuccessListener { onComplete("Profile saved to Firebase.") }
            .addOnFailureListener { onComplete("Profile save failed: ${it.localizedMessage}") }
    }

    fun saveEob(userId: String, record: EobRecord, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) return
        val payload = FirebaseEobMapper.eobToMap(record)
        firestore().collection(USERS).document(userId).collection(EOBS)
            .document(record.id.toString())
            .set(payload)
        firestore().collection(USERS).document(userId).collection(EOB_RECORDS)
            .document(record.id.toString())
            .set(payload)
            .addOnSuccessListener { onComplete("EOB saved to Firebase.") }
            .addOnFailureListener { onComplete("EOB save failed: ${it.localizedMessage}") }
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
        val ref = FirebaseStorage.getInstance().reference.child("users/$userId/eob_uploads/$fileName")
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("sourceName", sourceName)
            .build()
        ref.putFile(uri, metadata)
            .addOnSuccessListener { onComplete("EOB uploaded. Veryfi processing started.") }
            .addOnFailureListener { onComplete("EOB upload failed: ${it.localizedMessage}") }
    }

    fun uploadEobBitmap(userId: String, bitmap: Bitmap, sourceName: String, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) {
            onComplete("Please sign in before scanning an EOB.")
            return
        }
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        val ref = FirebaseStorage.getInstance().reference.child("users/$userId/eob_uploads/eob_${System.currentTimeMillis()}.jpg")
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

    private fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun functions(): FirebaseFunctions = FirebaseFunctions.getInstance()

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
        const val EOB_RECORDS = "eob_records"
        const val DEVICES = "devices"
        const val NEWS = "insuranceNews"
        const val SEND_ACCOUNT_VERIFICATION_CODE = "sendAccountVerificationCode"
        const val VERIFY_ACCOUNT_VERIFICATION_CODE = "verifyAccountVerificationCode"
    }
}

private class CombinedListenerRegistration(
    private val listeners: List<ListenerRegistration>
) : ListenerRegistration {
    override fun remove() {
        listeners.forEach { it.remove() }
    }
}
