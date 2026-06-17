package app.eob.me

import app.eob.me.scanner.GmsDocumentScannerLauncher
import org.junit.Assert.assertNull
import org.junit.Test

class GmsDocumentScannerLauncherTest {
    @Test
    fun parseScanResultReturnsNullForCanceledResult() {
        val uri = GmsDocumentScannerLauncher.parseScanResult(
            resultCode = android.app.Activity.RESULT_CANCELED,
            data = null
        )
        assertNull(uri)
    }
}
