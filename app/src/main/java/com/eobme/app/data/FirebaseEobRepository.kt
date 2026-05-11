package com.eobme.app.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

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
                saveProfile(userId, profile) {}
                registerMessagingToken(userId)
                onResult(FirebaseSyncStatus(true, userId, "Firebase sync is active."))
            }
            .addOnFailureListener {
                auth.createUserWithEmailAndPassword(profile.email, profile.password)
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

    fun observeEobs(
        userId: String,
        onRecords: (List<EobRecord>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        if (!configured || userId.isBlank()) return null
        return firestore().collection(USERS).document(userId).collection(EOBS)
            .orderBy("serviceDateSortKey", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError("EOB sync failed: ${error.localizedMessage}")
                    return@addSnapshotListener
                }
                val records = snapshot?.documents
                    ?.mapNotNull { document -> document.data?.let(FirebaseEobMapper::eobFromMap) }
                    .orEmpty()
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

    fun saveProfile(userId: String, profile: UserProfile, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) return
        firestore().collection(USERS).document(userId)
            .set(FirebaseEobMapper.profileToMap(profile))
            .addOnSuccessListener { onComplete("Profile saved to Firebase.") }
            .addOnFailureListener { onComplete("Profile save failed: ${it.localizedMessage}") }
    }

    fun saveEob(userId: String, record: EobRecord, onComplete: (String) -> Unit) {
        if (!configured || userId.isBlank()) return
        firestore().collection(USERS).document(userId).collection(EOBS)
            .document(record.id.toString())
            .set(FirebaseEobMapper.eobToMap(record))
            .addOnSuccessListener { onComplete("EOB saved to Firebase.") }
            .addOnFailureListener { onComplete("EOB save failed: ${it.localizedMessage}") }
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
        const val DEVICES = "devices"
        const val NEWS = "insuranceNews"
    }
}
