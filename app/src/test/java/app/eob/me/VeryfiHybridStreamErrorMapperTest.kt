package app.eob.me

import app.eob.me.network.VeryfiHybridStreamErrorMapper
import org.junit.Assert.assertTrue
import org.junit.Test

class VeryfiHybridStreamErrorMapperTest {
    @Test
    fun mapsGenericThrowablesToMessage() {
        val message = VeryfiHybridStreamErrorMapper.describe(
            IllegalStateException("Callable transport timeout")
        )
        assertTrue(message.contains("Callable transport timeout"))
    }

    @Test
    fun unwrapsNestedCauseMessages() {
        val message = VeryfiHybridStreamErrorMapper.describe(
            IllegalStateException(
                "Veryfi hybrid stream failed",
                RuntimeException("Veryfi AnyDocs extraction failed with status 404")
            )
        )
        assertTrue(message.contains("Veryfi hybrid stream failed"))
    }
}
