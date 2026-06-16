package app.eob.me

import app.eob.me.data.HubSettingsStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HubSettingsStoreTest {
    @Test
    fun darkModeEnabledDefaultsToFalseAndPersists() {
        val context = RuntimeEnvironment.getApplication()
        val store = HubSettingsStore(context)

        assertFalse(store.read().darkModeEnabled)

        store.write(store.read().copy(darkModeEnabled = true))
        assertTrue(store.read().darkModeEnabled)

        store.write(store.read().copy(darkModeEnabled = false))
        assertFalse(store.read().darkModeEnabled)
    }

    @Test
    fun appPinSaveVerifyAndClear() {
        val context = RuntimeEnvironment.getApplication()
        val store = HubSettingsStore(context)

        assertFalse(store.hasAppPin())
        store.saveAppPin("12345")
        assertTrue(store.hasAppPin())
        assertTrue(store.verifyAppPin("12345"))
        assertFalse(store.verifyAppPin("54321"))

        store.clearAppPin()
        assertFalse(store.hasAppPin())
        assertFalse(store.verifyAppPin("12345"))
    }
}
