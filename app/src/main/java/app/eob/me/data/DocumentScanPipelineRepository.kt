package app.eob.me.data

import android.content.Context
import android.net.Uri
import app.eob.me.network.VeryfiDocumentClient
import app.eob.me.util.EobDocumentOcrPreCheck
import app.eob.me.util.OcrProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class DocumentScanPipelineRepository(
    private val firebase: FirebaseEobRepository,
    private val veryfiClient: VeryfiDocumentClient = VeryfiDocumentClient()
) {
    suspend fun runOcrPreCheck(context: Context, uri: Uri): EobDocumentOcrPreCheck.Result {
        val recognizedText = OcrProcessor.recognizeFromUri(context, uri)
        return EobDocumentOcrPreCheck.validate(recognizedText)
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

    suspend fun processHybridDocument(
        context: Context,
        userId: String,
        uri: Uri,
        sourceName: String
    ): EobRecord = coroutineScope {
        val contentType = context.contentResolver.getType(uri)
            ?: if (uri.toString().endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
        val extension = HybridDocumentRef.extensionForContentType(contentType)
        val fileName = HybridDocumentRef.fileNameForUpload(extension)
        val documentRefId = HybridDocumentRef.documentRefId(fileName)
        val fileBytes = readUriBytes(context, uri)

        val uploadDeferred = async {
            uploadDocument(
                userId = userId,
                uri = uri,
                sourceName = sourceName,
                fileName = fileName
            )
        }
        val streamDeferred = async {
            veryfiClient.streamExtractDocument(
                userId = userId,
                documentRefId = documentRefId,
                fileBytes = fileBytes,
                fileName = fileName,
                contentType = contentType
            )
        }

        val upload = uploadDeferred.await()
        val veryfiPayload = runCatching { streamDeferred.await() }.getOrNull()
        if (veryfiPayload != null) {
            veryfiClient.writeReconciliationFindings(
                userId = userId,
                extraction = VeryfiStreamExtraction(
                    documentRefId = upload.documentRefId,
                    sourceFilePath = upload.storagePath,
                    payload = veryfiPayload
                )
            )
        }
        veryfiClient.awaitVeryfiExtraction(
            userId = userId,
            storagePath = upload.storagePath
        )
    }

    suspend fun uploadAndExtractDocument(
        context: Context,
        userId: String,
        uri: Uri,
        sourceName: String
    ): Result<EobRecord> {
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
