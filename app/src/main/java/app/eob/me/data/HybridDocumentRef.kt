package app.eob.me.data

/**
 * Shared document identifiers for hybrid validation tracks (client stream + Firebase upload).
 */
object HybridDocumentRef {
    /** Pathway 1 folder: users/{userId}/eobs/{fileName} */
    const val USER_ROOTED_EOB_FOLDER = "eobs"

    /** Pathway 2 root segment: eobs/{userId}/{fileName} */
    const val DOCUMENT_ROOTED_PREFIX = "eobs"

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

    /** Pathway 1 — user-rooted: users/{userId}/eobs/{fileName} */
    fun userRootedStoragePath(userId: String, fileName: String): String =
        normalizeStoragePath("users/$userId/$USER_ROOTED_EOB_FOLDER/$fileName")

    /** Pathway 2 — document-rooted: eobs/{userId}/{fileName} */
    fun documentRootedStoragePath(userId: String, fileName: String): String =
        normalizeStoragePath("$DOCUMENT_ROOTED_PREFIX/$userId/$fileName")

    /** Primary hybrid upload object name (Pathway 1). */
    fun storagePathForUpload(userId: String, fileName: String): String =
        userRootedStoragePath(userId, fileName)
}
