package app.eob.me

import app.eob.me.network.VeryfiAnyDocConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards AnyDocs endpoint URL and blueprint strings against drift that would cause HTTP 404s.
 */
class VeryfiAnyDocConstantsTest {
    @Test
    fun documentsUrlComposesToPartnerDocumentsEndpoint() {
        val resolved = VeryfiAnyDocConstants.BASE_URL + VeryfiAnyDocConstants.ANY_DOCUMENTS_PATH
        assertEquals("https://api.veryfi.com/api/v8/partner/documents/", resolved)
        assertTrue(resolved.startsWith("https://api.veryfi.com/api/v8/"))
        assertTrue(resolved.endsWith("partner/documents/"))
    }

    @Test
    fun healthInsuranceEobBlueprintMatchesAnyDocsContract() {
        assertEquals("health_insurance_eob", VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB)
        assertFalse(VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB.isBlank())
    }

    @Test
    fun hybridStreamCallableMatchesCloudFunctionExport() {
        assertEquals("extractVeryfiHybridStream", VeryfiAnyDocConstants.EXTRACT_VERYFI_HYBRID_STREAM)
    }

    @Test
    fun eobDocumentTypeAndInsuranceCategoriesMatchAnyDocsContract() {
        assertEquals("eob", VeryfiAnyDocConstants.DOCUMENT_TYPE_EOB)
        assertEquals(listOf("insurance"), VeryfiAnyDocConstants.CATEGORIES_INSURANCE)
    }
}
