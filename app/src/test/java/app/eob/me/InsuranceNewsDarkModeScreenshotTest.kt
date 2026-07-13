package app.eob.me

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.InsuranceArticle
import app.eob.me.data.InsuranceNewsCarrierHubItem
import app.eob.me.data.MajorInsuranceCarrier
import app.eob.me.data.NewsRelease
import app.eob.me.ui.screens.NewsScreen
import app.eob.me.ui.theme.EOBmeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w360dp-h900dp")
class InsuranceNewsDarkModeScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleCarrierHubItems = MajorInsuranceCarrier.entries.map { carrier ->
        InsuranceNewsCarrierHubItem(
            carrier = carrier,
            monthlyBriefingCount = 1,
            featuredArticle = InsuranceArticle(
                id = "briefing-${carrier.name}",
                carrier = carrier,
                monthIndex = 5,
                monthLabel = "June",
                year = 2026,
                headline = "${carrier.displayName} monthly briefing",
                body = "Sample briefing body for ${carrier.displayName}."
            )
        )
    }

    private val sampleNewsItems = listOf(
        NewsRelease(
            company = "Becker's Payer Issues",
            headline = "Payer transparency rules reshape prior authorization",
            summary = "National payers are accelerating digital prior authorization and member-facing cost tools.",
            date = "06/18/2026",
            articleUrl = "https://example.com/beckers-story"
        ),
        NewsRelease(
            company = "Healthcare Dive",
            headline = "Employer-sponsored plans focus on affordability programs",
            summary = "Health plans are expanding navigation support and preventive care incentives for members.",
            date = "06/17/2026",
            articleUrl = "https://example.com/healthcare-dive-story"
        )
    )

    @Test
    fun insuranceNewsDarkModeScreenshot() {
        val language = AppLanguage.English
        composeRule.setContent {
            EOBmeTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A1628)
                ) {
                    NewsScreen(
                        language = language,
                        carrierHubItems = sampleCarrierHubItems,
                        selectedCarrier = MajorInsuranceCarrier.UnitedHealthcare,
                        onCarrierSelected = {},
                        selectedInsuranceArticle = null,
                        onDismissInsuranceArticle = {},
                        newsItems = sampleNewsItems,
                        onDeleteNews = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            EobStrings.t(language, "insuranceIntelligence")
        ).assertIsDisplayed()
        composeRule.onNodeWithText("BECKER", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("HEALTHCARE DIVE", substring = true).assertIsDisplayed()

        saveScreenshot("insurance_news_dark_mode_pr170.png")
    }

    private fun saveScreenshot(fileName: String) {
        val outputDir = File("/opt/cursor/artifacts/screenshots").also { it.mkdirs() }
        val outputFile = File(outputDir, fileName)
        val rootView = composeRule.activity.findViewById<ViewGroup>(android.R.id.content)
            ?.getChildAt(0)
            ?: error("Compose root view not found")
        val width = rootView.width.coerceAtLeast(1)
        val height = rootView.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        rootView.draw(canvas)
        FileOutputStream(outputFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assert(outputFile.exists() && outputFile.length() > 0L) {
            "Screenshot was not written: ${outputFile.absolutePath}"
        }
    }
}
