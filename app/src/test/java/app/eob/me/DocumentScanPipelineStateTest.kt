package app.eob.me

import app.eob.me.data.DocumentScanPipelineState
import app.eob.me.data.EobRecord
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentScanPipelineStateTest {
    @Test
    fun hybridPipelineStatesAreRepresented() {
        val states: List<DocumentScanPipelineState> = listOf(
            DocumentScanPipelineState.Idle,
            DocumentScanPipelineState.LocalScanning,
            DocumentScanPipelineState.OcrPreCheck,
            DocumentScanPipelineState.UploadingAndProcessing,
            DocumentScanPipelineState.Success(sampleRecord()),
            DocumentScanPipelineState.Error("Upload failed")
        )
        assertTrue(states.size == 6)
        assertTrue(states.any { it is DocumentScanPipelineState.Success })
        assertTrue(states.any { it is DocumentScanPipelineState.Error })
    }

    private fun sampleRecord(): EobRecord {
        return EobRecord(
            id = 1,
            sourceName = "scan",
            providerName = "Provider",
            insuranceName = "Insurance",
            serviceDate = "01/01/2026",
            serviceDateSortKey = 20260101,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = ""
        )
    }
}
