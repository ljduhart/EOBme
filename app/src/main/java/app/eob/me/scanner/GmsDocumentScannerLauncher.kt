package app.eob.me.scanner

import android.app.Activity
import android.content.Context
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

    fun parseScanResult(context: Context, resultCode: Int, data: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK || data == null) return null
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data) ?: return null
        val uri = scanResult.pdf?.uri ?: scanResult.pages?.firstOrNull()?.imageUri ?: return null
        retainReadPermission(context, data, uri)
        return uri
    }

    private fun retainReadPermission(context: Context, data: Intent, uri: Uri) {
        val readFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, readFlag)
        }
        data.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) {
                val clipUri = clip.getItemAt(index).uri ?: continue
                runCatching {
                    context.contentResolver.takePersistableUriPermission(clipUri, readFlag)
                }
            }
        }
    }
}
