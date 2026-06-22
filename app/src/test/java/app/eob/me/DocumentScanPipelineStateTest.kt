package app.eob.me

import app.eob.me.data.DocumentScanPipelineState
import app.eob.me.data.EobProcessedResult
import app.eob.me.data.VeryfiExtractedData
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
            DocumentScanPipelineState.Success(sampleProcessedResult()),
            DocumentScanPipelineState.Error("Upload failed")
        )
        assertTrue(states.size == 6)
        assertTrue(states.any { it is DocumentScanPipelineState.Success })
        assertTrue(states.any { it is DocumentScanPipelineState.Error })
    }

    private fun sampleProcessedResult(): EobProcessedResult {
        return EobProcessedResult(
            fileUrl = "https://storage.example/eob.pdf",
            veryfiData = VeryfiExtractedData(
                dateOfService = "01/01/2026",
                cptCodes = listOf("99213"),
                patientResponsibility = 35.0,
                copay = 35.0,
                providerName = "Provider",
                insuranceCompanyName = "Insurance"
            )
        )
    }
}
