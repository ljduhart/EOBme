package app.eob.me

import app.eob.me.data.HybridDocumentRef
import org.junit.Assert.assertEquals
import org.junit.Test

class HybridDocumentRefTest {
    @Test
    fun normalizeStoragePathStripsLeadingSlashForCloudFunctionParity() {
        val objectName = "users/u1/eob_uploads/eob_123.jpg"
        assertEquals(objectName, HybridDocumentRef.normalizeStoragePath("/$objectName"))
        assertEquals(objectName, HybridDocumentRef.normalizeStoragePath(objectName))
        assertEquals(objectName, HybridDocumentRef.normalizeStoragePath("  /$objectName  "))
    }

    @Test
    fun storagePathForUploadMatchesFirebaseObjectName() {
        val path = HybridDocumentRef.storagePathForUpload("uid123", "eob_123.jpg")
        assertEquals("users/uid123/eob_uploads/eob_123.jpg", path)
    }
}
