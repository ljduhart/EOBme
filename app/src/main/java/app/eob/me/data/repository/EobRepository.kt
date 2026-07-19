package app.eob.me.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import app.eob.me.data.CameraScanDocumentType
import app.eob.me.data.EobRecord
import app.eob.me.data.ReceiptRecord
import app.eob.me.data.DocumentUploadResult
import app.eob.me.data.VeryfiAnyDocExtractionResult
import app.eob.me.util.EobDocumentOcrPreCheck
import app.eob.me.data.FirebaseSyncStatus
import app.eob.me.data.NewsRelease
import app.eob.me.data.InsuranceCardNotesMetadata
import app.eob.me.data.UserProfile
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow

/**
 * Remote data contract for EOB hub features. Firestore is the system of record.
 *
 * EOB scan uploads use Firebase Storage Pathway 1: users/{userId}/eobs/{fileName}
 * (see [app.eob.me.data.HybridDocumentRef.userRootedStoragePath]). Pathway 2
 * eobs/{userId}/{fileName} is supported by storage rules and Cloud Functions triggers.
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

    fun observeInsuranceCardMetadata(
        userId: String,
        onMetadata: (InsuranceCardNotesMetadata) -> Unit
    ): ListenerRegistration?

    fun saveEob(userId: String, record: EobRecord, onComplete: (String) -> Unit)

    fun deleteEob(userId: String, record: EobRecord, onComplete: (String) -> Unit)

    fun uploadEobFile(userId: String, uri: Uri, sourceName: String, onComplete: (String) -> Unit)

    fun uploadEobBitmap(userId: String, bitmap: Bitmap, sourceName: String, onComplete: (String) -> Unit)

    suspend fun uploadEobFileAwaitDownload(
        userId: String,
        uri: Uri,
        sourceName: String,
        fileName: String? = null
    ): DocumentUploadResult

    suspend fun runDocumentOcrPreCheck(
        context: Context,
        uri: Uri,
        scanType: CameraScanDocumentType = CameraScanDocumentType.Eob
    ): EobDocumentOcrPreCheck.Result

    suspend fun extractUploadedDocument(
        userId: String,
        upload: DocumentUploadResult
    ): EobRecord

    suspend fun processHybridScannedDocument(
        context: Context,
        userId: String,
        uri: Uri,
        sourceName: String
    ): VeryfiAnyDocExtractionResult

    suspend fun uploadAndExtractDocument(
        context: Context,
        userId: String,
        uri: Uri,
        sourceName: String
    ): Result<VeryfiAnyDocExtractionResult>

    fun observeVaultReceipts(
        userId: String,
        onReceipts: (List<ReceiptRecord>) -> Unit,
        onError: (String) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration?

    suspend fun uploadVaultReceiptAwaitDownload(
        userId: String,
        uri: Uri,
        sourceName: String
    ): DocumentUploadResult

    fun saveVaultReceipt(userId: String, receipt: ReceiptRecord, onComplete: (String) -> Unit)

    fun deleteAccount(userId: String, onComplete: (String) -> Unit)
}
