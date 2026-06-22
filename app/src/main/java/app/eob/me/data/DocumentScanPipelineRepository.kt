package app.eob.me.data

import android.content.Context
import android.net.Uri
import app.eob.me.network.VeryfiDocumentClient
import app.eob.me.network.VeryfiHybridStreamErrorMapper
import app.eob.me.util.EobDocumentOcrPreCheck
import app.eob.me.util.OcrProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

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
     * Parallel hybrid split:
     * - Task A: Firebase Storage upload (background, does not block Veryfi).
     * - Task B: Base64 AnyDocs extraction via authenticated Cloud Function (immediate).
     *
     * Veryfi starts as soon as local bytes are available — no Firebase download URL required.
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
        val fileBytes = readUriBytes(context, uri)

        return coroutineScope {
            val uploadDeferred = async(Dispatchers.IO) {
                uploadDocument(
                    userId = userId,
                    uri = uri,
                    sourceName = sourceName,
                    fileName = fileName
                )
            }
            val extractionDeferred = async(Dispatchers.IO) {
                veryfiAnyDocRepository.extractHealthInsuranceEob(
                    userId = userId,
                    documentRefId = documentRefId,
                    fileBytes = fileBytes,
                    fileName = fileName,
                    contentType = contentType,
                    sourceName = sourceName
                ).getOrElse { error ->
                    throw IllegalStateException(
                        VeryfiHybridStreamErrorMapper.describe(error),
                        error
                    )
                }
            }

            val anyDocResult = try {
                extractionDeferred.await()
            } catch (error: Throwable) {
                uploadDeferred.cancel()
                throw error
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
                uploadDeferred.cancel()
                throw IllegalStateException(
                    "Veryfi extraction succeeded but Firestore reconciliation failed: " +
                        VeryfiHybridStreamErrorMapper.describe(error),
                    error
                )
            }

            runCatching { uploadDeferred.await() }

            anyDocResult.copy(record = streamedRecord)
        }
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

    private suspend fun readUriBytes(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalArgumentException("Unable to read scanned document bytes.")
    }
}
