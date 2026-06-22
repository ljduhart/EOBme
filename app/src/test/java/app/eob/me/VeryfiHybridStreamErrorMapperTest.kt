package app.eob.me

import app.eob.me.network.VeryfiHybridStreamErrorMapper
import org.junit.Assert.assertTrue
import org.junit.Test

class VeryfiHybridStreamErrorMapperTest {
    @Test
    fun describeIncludesUnderlyingMessage() {
        val message = VeryfiHybridStreamErrorMapper.describe(
            IllegalStateException("Callable transport timeout")
        )
        assertTrue(message.contains("Callable transport timeout"))
    }
}
