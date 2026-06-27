package app.eob.me.data

import app.eob.me.network.VeryfiAnyDocMapper
import app.eob.me.network.VeryfiDocumentClient
import app.eob.me.network.VeryfiHybridStreamErrorMapper
import app.eob.me.network.VeryfiInsuranceEobPayloadParser
import app.eob.me.network.veryfiPayloadToEobRecord
import kotlinx.coroutines.CancellationException

/**
 * Repository for Veryfi AnyDocs `health_insurance_eob` extractions. Network I/O is proxied through
 * [VeryfiDocumentClient] (Firebase Cloud Function) so credentials remain server-side.
 */
class VeryfiAnyDocRepository(
    private val veryfiClient: VeryfiDocumentClient = VeryfiDocumentClient()
) {
    suspend fun extractHealthInsuranceEob(
        userId: String,
        documentRefId: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        sourceName: String
    ): Result<VeryfiAnyDocExtractionResult> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User id is required for Veryfi AnyDocs extraction."))
        }
        if (documentRefId.isBlank()) {
            return Result.failure(IllegalArgumentException("Document reference id is required."))
        }
        if (fileBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Document bytes are required for Veryfi AnyDocs extraction."))
        }
        if (fileName.isBlank()) {
            return Result.failure(IllegalArgumentException("File name is required for Veryfi AnyDocs extraction."))
        }

        return try {
            val rawPayload = veryfiClient.streamExtractDocument(
                userId = userId,
                documentRefId = documentRefId,
                fileBytes = fileBytes,
                fileName = fileName,
                contentType = contentType
            )
            val mergedPayload = VeryfiAnyDocMapper.mergePayloadWithEobFields(rawPayload, documentRefId)
            val nestedPayload = when {
                VeryfiInsuranceEobPayloadParser.isNestedClaimsPayload(rawPayload) -> rawPayload
                VeryfiInsuranceEobPayloadParser.isNestedClaimsPayload(mergedPayload) -> mergedPayload
                else -> null
            }
            val effectivePayload = nestedPayload ?: mergedPayload
            val extraction = VeryfiAnyDocMapper.mapFromUntypedPayload(effectivePayload, documentRefId)
            val record = veryfiPayloadToEobRecord(
                payload = effectivePayload,
                documentRefId = documentRefId,
                sourceName = sourceName
            )
            Result.success(
                VeryfiAnyDocExtractionResult(
                    extraction = extraction,
                    record = record,
                    rawPayload = effectivePayload
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(
                IllegalStateException(VeryfiHybridStreamErrorMapper.describe(error), error)
            )
        }
    }
}
