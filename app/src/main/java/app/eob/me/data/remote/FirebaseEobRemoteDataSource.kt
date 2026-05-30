package app.eob.me.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import app.eob.me.data.EobRecord
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.FirebaseSyncStatus
import app.eob.me.data.NewsRelease
import app.eob.me.data.UserProfile
import app.eob.me.data.repository.EobRepository
import com.google.firebase.firestore.ListenerRegistration

/**
 * Firebase / Firestore implementation of [EobRepository].
 */
class FirebaseEobRemoteDataSource(context: Context) : EobRepository {
    private val firebase = FirebaseEobRepository(context.applicationContext)

    override fun status(): FirebaseSyncStatus = firebase.status()

    override fun observeProfile(
        userId: String,
        onProfile: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? = firebase.observeProfile(userId, onProfile, onError)

    override fun observeEobs(
        userId: String,
        onRecords: (List<EobRecord>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? = firebase.observeEobs(userId, onRecords, onError)

    override fun observeInsuranceNews(
        onNews: (List<NewsRelease>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? = firebase.observeInsuranceNews(onNews, onError)

    override fun saveProfile(userId: String, profile: UserProfile, onComplete: (String) -> Unit) {
        firebase.saveProfile(userId, profile, onComplete)
    }

    override fun saveInsuranceCardMetadata(userId: String, profile: UserProfile, onComplete: (String) -> Unit) {
        firebase.saveInsuranceCardMetadata(userId, profile, onComplete)
    }

    override fun saveEob(userId: String, record: EobRecord, onComplete: (String) -> Unit) {
        firebase.saveEob(userId, record, onComplete)
    }

    override fun deleteEob(userId: String, record: EobRecord, onComplete: (String) -> Unit) {
        firebase.deleteEob(userId, record, onComplete)
    }

    override fun uploadEobFile(userId: String, uri: Uri, sourceName: String, onComplete: (String) -> Unit) {
        firebase.uploadEobFile(userId, uri, sourceName, onComplete)
    }

    override fun uploadEobBitmap(userId: String, bitmap: Bitmap, sourceName: String, onComplete: (String) -> Unit) {
        firebase.uploadEobBitmap(userId, bitmap, sourceName, onComplete)
    }
}
