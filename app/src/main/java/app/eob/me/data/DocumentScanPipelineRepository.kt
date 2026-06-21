package app.eob.me.data

import android.content.Context
import android.net.Uri
import app.eob.me.data.CameraScanDocumentType
import app.eob.me.network.VeryfiDocumentClient
import app.eob.me.network.VeryfiAnyDocConstants
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

    suspend fun processHybridDocument(
        context: Context,
        userId: String,
        uri: Uri,
        sourceName: String
    ): VeryfiAnyDocExtractionResult = coroutineScope {
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

        val upload = uploadDeferred.await()
        val anyDocResult = veryfiAnyDocRepository.extractHealthInsuranceEob(
            userId = userId,
            documentRefId = upload.documentRefId,
            fileBytes = fileBytes,
            fileName = fileName,
            contentType = contentType,
            sourceName = sourceName
        ).getOrNull()

        if (anyDocResult != null) {
            val streamedRecord = runCatching {
                veryfiClient.writeReconciliationFindings(
                    userId = userId,
                    extraction = VeryfiStreamExtraction(
                        documentRefId = upload.documentRefId,
                        sourceFilePath = upload.storagePath,
                        payload = anyDocResult.rawPayload
                    ),
                    sourceName = sourceName
                )
            }.getOrNull()
            if (streamedRecord != null) {
                return@coroutineScope anyDocResult.copy(record = streamedRecord)
            }
            return@coroutineScope anyDocResult
        }

        val fallbackRecord = veryfiClient.awaitVeryfiExtraction(
            userId = userId,
            storagePath = upload.storagePath
        )
        VeryfiAnyDocExtractionResult(
            extraction = VeryfiHealthInsuranceEob(
                documentId = upload.documentRefId,
                blueprintName = VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB,
                insuranceCompanyName = fallbackRecord.insuranceName,
                memberName = "",
                memberId = "",
                patientName = "",
                claimId = "",
                inNetworkOutOfPocketBalance = 0.0,
                outOfNetworkOutOfPocketBalance = 0.0,
                dateOfService = fallbackRecord.serviceDate,
                providerName = fallbackRecord.providerName
            ),
            record = fallbackRecord,
            rawPayload = emptyMap()
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

    private suspend fun readUriBytes(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalArgumentException("Unable to read scanned document bytes.")
    }
}
