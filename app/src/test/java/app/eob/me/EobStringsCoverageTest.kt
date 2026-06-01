package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.File

class EobStringsCoverageTest {
    @Test
    fun allReferencedKeysExistInEnglishDictionary() {
        val sourceRoot = File("src/main/java/app/eob/me")
        require(sourceRoot.isDirectory) { "Run from :app module; missing ${sourceRoot.absolutePath}" }

        val keyRegex = Regex("""EobStrings\.t\([^,]+,\s*"([^"]+)"\)""")
        val tfKeyRegex = Regex("""EobStrings\.tf\([^,]+,\s*"([^"]+)"\)""")
        val firebaseKeyRegex = Regex("""t\(language,\s*"([^"]+)"\)""")

        val referenced = linkedSetOf<String>()
        sourceRoot.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                keyRegex.findAll(text).forEach { referenced += it.groupValues[1] }
                tfKeyRegex.findAll(text).forEach { referenced += it.groupValues[1] }
                if (file.name == "EobStrings.kt") {
                    firebaseKeyRegex.findAll(text).forEach { referenced += it.groupValues[1] }
                }
            }

        val missing = referenced.filter { it !in EobStrings.allEnglishKeys }.sorted()
        assertFalse("Missing EobStrings English keys: $missing", missing.isNotEmpty())
    }

    @Test
    fun firebaseStatusKeysResolveForEveryLanguage() {
        AppLanguage.entries.forEach { language ->
            listOf("firebaseNotConfigured", "firebaseActive", "firebaseConfigured").forEach { key ->
                val value = EobStrings.t(language, key)
                assertNotEquals(key, value)
            }
        }
    }
}
