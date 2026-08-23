package app.eob.me

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import app.eob.me.scanner.GmsDocumentScannerLauncher
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
