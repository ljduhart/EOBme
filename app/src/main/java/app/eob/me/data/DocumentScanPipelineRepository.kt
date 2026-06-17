package app.eob.me.data

import android.net.Uri
import app.eob.me.network.VeryfiDocumentClient

class DocumentScanPipelineRepository(
    private val firebase: FirebaseEobRepository,
    private val veryfiClient: VeryfiDocumentClient = VeryfiDocumentClient()
) {
    suspend fun uploadDocument(
        userId: String,
        uri: Uri,
        sourceName: String
    ): DocumentUploadResult = firebase.uploadEobFileAwaitDownload(userId, uri, sourceName)

    suspend fun extractUploadedDocument(
        userId: String,
        upload: DocumentUploadResult
    ): EobRecord {
        return veryfiClient.extractDocument(
            userId = userId,
            storagePath = upload.storagePath,
            downloadUrl = upload.downloadUrl
        )
    }

    suspend fun uploadAndExtractDocument(
        userId: String,
        uri: Uri,
        sourceName: String
    ): Result<EobRecord> {
        return runCatching {
            val upload = firebase.uploadEobFileAwaitDownload(userId, uri, sourceName)
            veryfiClient.extractDocument(
                userId = userId,
                storagePath = upload.storagePath,
                downloadUrl = upload.downloadUrl
            )
        }
    }
}
