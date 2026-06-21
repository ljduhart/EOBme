package app.eob.me.data

/**
 * Shared document identifiers for hybrid validation tracks (client stream + Firebase upload).
 */
object HybridDocumentRef {
    fun fileNameForUpload(extension: String): String = "eob_${System.currentTimeMillis()}.$extension"

    fun documentRefId(fileName: String): String =
        fileName.replace(Regex("[^A-Za-z0-9_-]"), "_")

    /**
     * Firestore document id shared by both hybrid tracks so the client stream write and the
     * Storage-triggered Cloud Function ([processUploadedEobWithVeryfi]) converge on the same EOB
     * document instead of creating duplicates. Mirrors the backend `stableId` (`Math.abs(hash) || 1`)
     * which relies on Java/JS-identical string hashing.
     */
    fun stableDocumentId(documentRefId: String): String {
        val hash = documentRefId.hashCode().toLong()
        val positive = kotlin.math.abs(hash)
        return positive.takeUnless { it == 0L }?.toString() ?: "1"
    }

    fun extensionForContentType(contentType: String): String =
        if (contentType == "application/pdf") "pdf" else "jpg"

    /** Firebase Storage [Reference.path] may include a leading slash; Cloud Functions use object names without one. */
    fun normalizeStoragePath(path: String): String = path.trim().removePrefix("/")
}
