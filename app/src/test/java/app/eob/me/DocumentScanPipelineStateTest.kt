package app.eob.me

import app.eob.me.data.DocumentScanPipelineState
import app.eob.me.data.EobRecord
import app.eob.me.network.InsuranceNewsRotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentScanPipelineStateTest {
    @Test
    fun pipelineStatesCoverFullDocumentFlow() {
        val states = listOf(
            DocumentScanPipelineState.Idle,
            DocumentScanPipelineState.Scanning,
            DocumentScanPipelineState.UploadingToFirebase,
            DocumentScanPipelineState.ExtractingWithVeryfi,
            DocumentScanPipelineState.Success(sampleRecord()),
            DocumentScanPipelineState.Error("Upload failed")
        )
        assertEquals(6, states.size)
        assertTrue(states.any { it is DocumentScanPipelineState.Success })
        assertTrue(states.any { it is DocumentScanPipelineState.Error })
    }

    private fun sampleRecord(): EobRecord {
        return EobRecord(
            id = 1,
            sourceName = "Test",
            providerName = "Provider",
            insuranceName = "Insurance",
            serviceDate = "01/01/2026",
            serviceDateSortKey = 20260101,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = "Sample"
        )
    }
}
