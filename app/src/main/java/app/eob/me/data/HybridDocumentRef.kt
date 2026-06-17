package app.eob.me.data

/**
 * Shared document identifiers for hybrid validation tracks (client stream + Firebase upload).
 */
object HybridDocumentRef {
    fun fileNameForUpload(extension: String): String = "eob_${System.currentTimeMillis()}.$extension"

    fun documentRefId(fileName: String): String =
        fileName.replace(Regex("[^A-Za-z0-9_-]"), "_")

    fun extensionForContentType(contentType: String): String =
        if (contentType == "application/pdf") "pdf" else "jpg"
}
