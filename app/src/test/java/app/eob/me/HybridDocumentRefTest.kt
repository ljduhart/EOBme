package app.eob.me

import app.eob.me.data.HybridDocumentRef
import org.junit.Assert.assertEquals
import org.junit.Test

class HybridDocumentRefTest {
    @Test
    fun normalizeStoragePathStripsLeadingSlashForCloudFunctionParity() {
        val objectName = "users/u1/eobs/eob_123.jpg"
        assertEquals(objectName, HybridDocumentRef.normalizeStoragePath("/$objectName"))
        assertEquals(objectName, HybridDocumentRef.normalizeStoragePath(objectName))
        assertEquals(objectName, HybridDocumentRef.normalizeStoragePath("  /$objectName  "))
    }

    @Test
    fun userRootedStoragePathMatchesPathwayOne() {
        val path = HybridDocumentRef.userRootedStoragePath("uid123", "eob_scan.jpg")
        assertEquals("users/uid123/eobs/eob_scan.jpg", path)
    }

    @Test
    fun documentRootedStoragePathMatchesPathwayTwo() {
        val path = HybridDocumentRef.documentRootedStoragePath("uid123", "eob_scan.jpg")
        assertEquals("eobs/uid123/eob_scan.jpg", path)
    }

    @Test
    fun storagePathForUploadUsesUserRootedPathway() {
        val path = HybridDocumentRef.storagePathForUpload("uid123", "eob_123.jpg")
        assertEquals("users/uid123/eobs/eob_123.jpg", path)
    }
}
