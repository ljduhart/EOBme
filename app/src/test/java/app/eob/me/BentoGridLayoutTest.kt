package app.eob.me

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BentoGridLayoutTest {

    @Test
    fun homeScreenUsesAdaptiveBentoGridSpacingAndWeights() {
        val source = readSource("ui/screens/HomeScreen.kt")
        listOf(
            "BentoGridLayout.spacing",
            "BentoGridLayout.aspectRatioForCellWidth",
            "Modifier.weight(1f)",
            "cellAspectRatio"
        ).forEach { snippet ->
            assertTrue("HomeScreen must contain: $snippet", source.contains(snippet))
        }
    }

    @Test
    fun bentoGridLayoutDefinesAdaptiveSpacingAndAspectRatio() {
        val source = readSource("ui/components/bento/BentoGridLayout.kt")
        listOf(
            "fun spacing",
            "fun aspectRatioForCellWidth",
            "BentoCellTitle",
            "TextOverflow.Ellipsis",
            "GRID_COLUMNS = 3"
        ).forEach { snippet ->
            assertTrue("BentoGridLayout must contain: $snippet", source.contains(snippet))
        }
    }

    @Test
    fun bentoCellsAcceptAdaptiveAspectRatioParameter() {
        val cells = listOf(
            "ui/components/bento/HistoryBentoCell.kt",
            "ui/components/bento/ProviderDirectoryBentoCell.kt",
            "ui/components/bento/CptTrackerBentoCell.kt",
            "ui/components/bento/YtdExpenseBentoCell.kt",
            "ui/components/bento/InsuranceNewsBentoCell.kt",
            "ui/components/bento/AppealGeneratorBentoCell.kt"
        )
        cells.forEach { path ->
            val source = readSource(path)
            assertTrue("$path must accept cellAspectRatio", source.contains("cellAspectRatio"))
            assertTrue("$path must apply cellAspectRatio", source.contains(".aspectRatio(cellAspectRatio)"))
        }
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
