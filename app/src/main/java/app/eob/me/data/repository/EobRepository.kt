package app.eob.me.data.repository

import android.graphics.Bitmap
import android.net.Uri
import app.eob.me.data.EobRecord
import app.eob.me.data.DocumentUploadResult
import app.eob.me.data.FirebaseSyncStatus
import app.eob.me.data.NewsRelease
import app.eob.me.data.UserProfile
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow

/**
 * Remote data contract for EOB hub features. Firestore is the system of record.
 */
interface EobRepository {
    fun status(): FirebaseSyncStatus

    fun observeProfile(
        userId: String,
        onProfile: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration?

    fun observeEobs(
        userId: String,
        onRecords: (List<EobRecord>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration?

    fun observeInsuranceNews(
        onNews: (List<NewsRelease>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration?

    fun observeRegionalNews(userState: String): Flow<List<NewsRelease>>

    fun saveProfile(userId: String, profile: UserProfile, onComplete: (String) -> Unit)

    fun saveInsuranceCardMetadata(userId: String, profile: UserProfile, onComplete: (String) -> Unit)

    fun saveEob(userId: String, record: EobRecord, onComplete: (String) -> Unit)

    fun deleteEob(userId: String, record: EobRecord, onComplete: (String) -> Unit)

    fun uploadEobFile(userId: String, uri: Uri, sourceName: String, onComplete: (String) -> Unit)

    fun uploadEobBitmap(userId: String, bitmap: Bitmap, sourceName: String, onComplete: (String) -> Unit)

    suspend fun uploadEobFileAwaitDownload(
        userId: String,
        uri: Uri,
        sourceName: String
    ): DocumentUploadResult

    suspend fun extractUploadedDocument(
        userId: String,
        upload: DocumentUploadResult
    ): EobRecord

    suspend fun uploadAndExtractDocument(
        userId: String,
        uri: Uri,
        sourceName: String
    ): Result<EobRecord>

    fun deleteAccount(userId: String, onComplete: (String) -> Unit)
}
