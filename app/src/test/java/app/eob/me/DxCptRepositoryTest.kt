package app.eob.me

import app.eob.me.data.repository.DxCptRepository
import app.eob.me.viewmodel.ReverseDxViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DxCptRepositoryTest {
    @Test
    fun loadsDxEntryFromAssetMap() = runBlocking {
        val repository = DxCptRepository(RuntimeEnvironment.getApplication())
        val entry = repository.getDxDetails("i10")
        assertNotNull(entry)
        assertEquals("I10", entry!!.dxCode)
        assertTrue(entry.totalPotentialMatches >= ReverseDxViewModel.MATCH_THRESHOLD)
    }

    @Test
    fun returnsNullForUnknownDxCode() = runBlocking {
        val repository = DxCptRepository(RuntimeEnvironment.getApplication())
        assertNull(repository.getDxDetails("ZZZ.999"))
    }

    @Test
    fun hypothyroidismEntryBelowThreshold() = runBlocking {
        val repository = DxCptRepository(RuntimeEnvironment.getApplication())
        val entry = repository.getDxDetails("E03.9")
        assertNotNull(entry)
        assertTrue(entry!!.totalPotentialMatches < ReverseDxViewModel.MATCH_THRESHOLD)
    }
}
