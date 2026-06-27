package app.eob.me.data

import android.content.Context
import android.net.Uri
import app.eob.me.network.VeryfiDocumentClient
import app.eob.me.network.VeryfiHybridStreamErrorMapper
import app.eob.me.util.EobDocumentOcrPreCheck
import app.eob.me.util.OcrProcessor

class DocumentScanPipelineRepository(
    private val firebase: FirebaseEobRepository,
    private val veryfiClient: VeryfiDocumentClient = VeryfiDocumentClient(),
    private val veryfiAnyDocRepository: VeryfiAnyDocRepository = VeryfiAnyDocRepository(veryfiClient)
) {
    suspend fun runOcrPreCheck(
        context: Context,
        uri: Uri,
        scanType: CameraScanDocumentType = CameraScanDocumentType.Eob
    ): EobDocumentOcrPreCheck.Result {
        val recognizedText = OcrProcessor.recognizeFromUri(context, uri)
        return EobDocumentOcrPreCheck.validateForScanType(recognizedText, scanType)
    }

    suspend fun uploadDocument(
        userId: String,
        uri: Uri,
        sourceName: String,
        fileName: String
    ): DocumentUploadResult = firebase.uploadEobFileAwaitDownload(
        userId = userId,
        uri = uri,
        sourceName = sourceName,
        fileName = fileName
    )

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

    /**
     * Hybrid pipeline:
     * 1. Upload to Firebase Storage (users/{userId}/eobs/{fileName}).
     * 2. Call Veryfi via Cloud Function using the Storage download URL (`file_url`).
     * 3. Commit Track B findings to Firestore and finalize hybrid reconciliation.
     */
    suspend fun processHybridDocument(
        context: Context,
        userId: String,
        uri: Uri,
        sourceName: String
    ): VeryfiAnyDocExtractionResult {
        val contentType = context.contentResolver.getType(uri)
            ?: if (uri.toString().endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
        val extension = HybridDocumentRef.extensionForContentType(contentType)
        val fileName = HybridDocumentRef.fileNameForUpload(extension)
        val documentRefId = HybridDocumentRef.documentRefId(fileName)
        val storagePath = HybridDocumentRef.storagePathForUpload(userId, fileName)

        val upload = uploadDocument(
            userId = userId,
            uri = uri,
            sourceName = sourceName,
            fileName = fileName
        )

        val anyDocResult = veryfiAnyDocRepository.extractHealthInsuranceEob(
            userId = userId,
            documentRefId = documentRefId,
            fileName = fileName,
            contentType = contentType,
            sourceName = sourceName,
            fileUrl = upload.downloadUrl
        ).getOrElse { error ->
            throw IllegalStateException(
                VeryfiHybridStreamErrorMapper.describe(error),
                error
            )
        }

        val streamedRecord = runCatching {
            veryfiClient.writeReconciliationFindings(
                userId = userId,
                extraction = VeryfiStreamExtraction(
                    documentRefId = documentRefId,
                    sourceFilePath = storagePath,
                    payload = anyDocResult.rawPayload
                ),
                sourceName = sourceName
            )
        }.getOrElse { error ->
            throw IllegalStateException(
                "Veryfi extraction succeeded but Firestore reconciliation failed: " +
                    VeryfiHybridStreamErrorMapper.describe(error),
                error
            )
        }

        veryfiClient.finalizeHybridReconciliation(
            userId = userId,
            record = streamedRecord,
            downloadUrl = upload.downloadUrl,
            storagePath = storagePath
        )

        veryfiClient.writeSupplementalClaimRecords(
            userId = userId,
            claimRecords = anyDocResult.claimRecords,
            primaryFirestoreId = streamedRecord.firestoreId,
            sourceName = sourceName
        )

        return anyDocResult.copy(
            record = streamedRecord,
            downloadUrl = upload.downloadUrl
        )
    }

    suspend fun uploadAndExtractDocument(
        context: Context,
        userId: String,
        uri: Uri,
        sourceName: String
    ): Result<VeryfiAnyDocExtractionResult> {
        return runCatching {
            processHybridDocument(
                context = context,
                userId = userId,
                uri = uri,
                sourceName = sourceName
            )
        }
    }
}
