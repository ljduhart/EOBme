package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.ProfileFormValidator
import app.eob.me.data.RegistrationCredentials
import app.eob.me.data.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFormValidationTest {
    private val language = AppLanguage.English

    @Test
    fun validProfilePassesValidation() {
        val profile = UserProfile(
            firstName = "Jane",
            lastName = "Doe",
            email = "jane@example.com",
            city = "Austin",
            state = "TX",
            pcpCopay = "$25"
        )
        val credentials = RegistrationCredentials(email = "jane@example.com", password = "")
        val errors = ProfileFormValidator.validate(language, profile, credentials)
        assertFalse(errors.hasErrors)
    }

    @Test
    fun missingRequiredFieldsReturnErrors() {
        val errors = ProfileFormValidator.validate(
            language = language,
            profile = UserProfile(),
            credentials = RegistrationCredentials()
        )
        assertNotNull(errors.firstName)
        assertNotNull(errors.lastName)
        assertNotNull(errors.email)
        assertNotNull(errors.city)
        assertNotNull(errors.state)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun invalidEmailAndCopayAreRejected() {
        val profile = UserProfile(
            firstName = "Jane",
            lastName = "Doe",
            email = "bad-email",
            city = "Austin",
            state = "TX",
            pcpCopay = "abc"
        )
        val credentials = RegistrationCredentials(email = "bad-email", password = "short")
        val errors = ProfileFormValidator.validate(language, profile, credentials)
        assertNotNull(errors.email)
        assertNotNull(errors.pcpCopay)
        assertNotNull(errors.password)
    }

    @Test
    fun blankPasswordDoesNotTriggerPasswordError() {
        val profile = UserProfile(
            firstName = "Jane",
            lastName = "Doe",
            email = "jane@example.com",
            city = "Austin",
            state = "TX"
        )
        val credentials = RegistrationCredentials(email = "jane@example.com", password = "")
        val errors = ProfileFormValidator.validate(language, profile, credentials)
        assertNull(errors.password)
    }
}
