package app.eob.me

import app.eob.me.util.DeviceCallingUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCallingUtilsTest {
    @Test
    fun filterPhoneInputAllowsOnlyDigitsParenthesesAndDashes() {
        assertEquals("(555)555-5555", DeviceCallingUtils.filterPhoneInput("a(555) x555-5555!"))
    }

    @Test
    fun extractPhoneDigitsKeepsOnlyTenUsDigits() {
        assertEquals("5551234567", DeviceCallingUtils.extractPhoneDigits("(555) 123-4567"))
        assertEquals("5551234567", DeviceCallingUtils.extractPhoneDigits("abc5551234567xyz89"))
    }

    @Test
    fun applyPhoneInputChangeFormatsUsPhoneNumber() {
        assertEquals("(555) 555-5555", DeviceCallingUtils.applyPhoneInputChange("5555555555"))
    }

    @Test
    fun careTeamPhoneVisualTransformationFormatsDigitsWithoutMutatingValue() {
        val transformation = DeviceCallingUtils.careTeamPhoneVisualTransformation()
        val transformed = transformation.filter(androidx.compose.ui.text.AnnotatedString("5551234567"))
        assertEquals("(555) 123-4567", transformed.text.text)
    }

    @Test
    fun applyPhoneInputChangeCapsAtTenDigits() {
        assertEquals("(555) 555-5555", DeviceCallingUtils.applyPhoneInputChange("55555555551234"))
    }

    @Test
    fun dialUriForReturnsTelSchemeWithDigitsOnly() {
        assertEquals("tel:5555555555", DeviceCallingUtils.dialUriFor("(555) 555-5555"))
    }

    @Test
    fun dialUriForReturnsNullWhenNoDigits() {
        assertNull(DeviceCallingUtils.dialUriFor("()-"))
    }

    @Test
    fun hasDialablePhoneRequiresDigits() {
        assertTrue(DeviceCallingUtils.hasDialablePhone("(555) 555-5555"))
        assertFalse(DeviceCallingUtils.hasDialablePhone("---"))
    }
}
