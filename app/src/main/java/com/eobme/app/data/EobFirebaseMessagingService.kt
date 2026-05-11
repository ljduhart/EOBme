package com.eobme.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EobFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("devices")
            .document(token)
            .set(mapOf("token" to token, "updatedAt" to System.currentTimeMillis()))
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Firestore listeners update EOBme data directly; FCM is used as a wake-up/notification path.
    }
}
