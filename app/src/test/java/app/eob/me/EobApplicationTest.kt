package app.eob.me

import android.app.Application
import org.junit.Assert.assertTrue
import org.junit.Test

class EobApplicationTest {
    @Test
    fun eobApplicationExtendsApplication() {
        assertTrue(Application::class.java.isAssignableFrom(EobApplication::class.java))
    }

    @Test
    fun eobApplicationOverridesOnCreate() {
        assertTrue(
            EobApplication::class.java.declaredMethods.any { method -> method.name == "onCreate" }
        )
    }
}
