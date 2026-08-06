package app.eob.me.data.repository

import android.content.Context
import app.eob.me.data.dx.CptCategory
import app.eob.me.data.dx.DxCptEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

class DxCptRepository(context: Context) {
    private val appContext = context.applicationContext
    private val loadMutex = Mutex()
    @Volatile
    private var cache: Map<String, DxCptEntry>? = null

    suspend fun getDxDetails(dxCode: String): DxCptEntry? = withContext(Dispatchers.Default) {
        val map = ensureLoaded()
        val normalized = normalizeDxCode(dxCode)
        if (normalized.isEmpty()) return@withContext null
        map[normalized]
            ?: map.entries.firstOrNull { normalizeDxCode(it.key) == normalized }?.value
    }

    private suspend fun ensureLoaded(): Map<String, DxCptEntry> {
        cache?.let { return it }
        return loadMutex.withLock {
            cache?.let { return it }
            val parsed = parseAssetMap()
            cache = parsed
            parsed
        }
    }

    private fun parseAssetMap(): Map<String, DxCptEntry> {
        return runCatching {
            val jsonText = appContext.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            val result = linkedMapOf<String, DxCptEntry>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val dxCode = keys.next()
                val entryObject = root.getJSONObject(dxCode)
                val description = entryObject.getString("description")
                val totalMatches = entryObject.getInt("totalPotentialMatches")
                val categoriesArray = entryObject.getJSONArray("categories")
                val categories = buildList {
                    for (index in 0 until categoriesArray.length()) {
                        val categoryObject = categoriesArray.getJSONObject(index)
                        add(
                            CptCategory(
                                name = categoryObject.getString("name"),
                                range = categoryObject.getString("range")
                            )
                        )
                    }
                }
                val entry = DxCptEntry(
                    dxCode = dxCode,
                    description = description,
                    categories = categories,
                    totalPotentialMatches = totalMatches
                )
                result[normalizeDxCode(dxCode)] = entry
            }
            result
        }.getOrElse { emptyMap() }
    }

    private fun normalizeDxCode(raw: String): String = raw.trim().uppercase(Locale.US)

    companion object {
        private const val ASSET_FILE = "dx_cpt_map.json"
    }
}
