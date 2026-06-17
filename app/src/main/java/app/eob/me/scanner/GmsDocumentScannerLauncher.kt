package app.eob.me.scanner

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

object GmsDocumentScannerLauncher {
    fun scannerOptions(): GmsDocumentScannerOptions {
        return GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .build()
    }

    fun scannerClient() = GmsDocumentScanning.getClient(scannerOptions())

    fun buildScanRequest(
        activity: Activity,
        onReady: (IntentSenderRequest) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        scannerClient()
            .getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                onReady(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener(onFailure)
    }

    fun parseScanResult(resultCode: Int, data: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data) ?: return null
        scanResult.pdf?.uri?.let { return it }
        return scanResult.pages?.firstOrNull()?.imageUri
    }
}
