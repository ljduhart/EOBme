package app.eob.me

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import app.eob.me.scanner.GmsDocumentScannerLauncher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GmsDocumentScannerLauncherTest {
    @Test
    fun parseScanResultReturnsNullForCanceledResult() {
        val uri = GmsDocumentScannerLauncher.parseScanResult(
            context = ApplicationProvider.getApplicationContext(),
            resultCode = Activity.RESULT_CANCELED,
            data = null
        )
        assertNull(uri)
    }

    @Test
    fun scannerOptionsRequestJpegOnlySinglePage() {
        val source = readSource("scanner/GmsDocumentScannerLauncher.kt")
        assertTrue(source.contains(".setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)"))
        assertFalse(source.contains("RESULT_FORMAT_PDF"))
        assertTrue(source.contains(".setPageLimit(1)"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
