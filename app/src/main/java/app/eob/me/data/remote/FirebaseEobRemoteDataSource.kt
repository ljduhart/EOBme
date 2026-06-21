package app.eob.me.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import app.eob.me.data.CameraScanDocumentType
import app.eob.me.data.EobRecord
import app.eob.me.data.DocumentScanPipelineRepository
import app.eob.me.data.DocumentUploadResult
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.FirebaseSyncStatus
import app.eob.me.data.NewsRelease
import app.eob.me.data.UserProfile
import app.eob.me.data.repository.EobRepository
import app.eob.me.data.VeryfiAnyDocExtractionResult
import app.eob.me.util.EobDocumentOcrPreCheck
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow

/**
 * Firebase / Firestore implementation of [EobRepository].
 */
class FirebaseEobRemoteDataSource(
    private val firebase: FirebaseEobRepository
) : EobRepository {

    private val documentScanPipeline = DocumentScanPipelineRepository(firebase)

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

    override fun observeRegionalNews(userState: String): Flow<List<NewsRelease>> =
        firebase.observeRegionalNews(userState)

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

    override suspend fun uploadEobFileAwaitDownload(
        userId: String,
        uri: Uri,
        sourceName: String,
        fileName: String?
    ): DocumentUploadResult = firebase.uploadEobFileAwaitDownload(userId, uri, sourceName, fileName)

    override suspend fun runDocumentOcrPreCheck(
        context: Context,
        uri: Uri,
        scanType: CameraScanDocumentType
    ): EobDocumentOcrPreCheck.Result = documentScanPipeline.runOcrPreCheck(context, uri, scanType)

    override suspend fun extractUploadedDocument(
        userId: String,
        upload: DocumentUploadResult
    ): EobRecord = documentScanPipeline.extractUploadedDocument(userId, upload)

    override suspend fun processHybridScannedDocument(
        context: Context,
        userId: String,
        uri: Uri,
        sourceName: String
    ): VeryfiAnyDocExtractionResult = documentScanPipeline.processHybridDocument(context, userId, uri, sourceName)

    override suspend fun uploadAndExtractDocument(
        context: Context,
        userId: String,
        uri: Uri,
        sourceName: String
    ): Result<VeryfiAnyDocExtractionResult> = documentScanPipeline.uploadAndExtractDocument(context, userId, uri, sourceName)

    override fun deleteAccount(userId: String, onComplete: (String) -> Unit) {
        firebase.deleteAccount(userId, onComplete)
    }
}
