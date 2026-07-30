package app.eob.me

import app.eob.me.data.BillingIssueType
import app.eob.me.data.CptCategory
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr196Test {
    @Test
    fun billingDetectorsUseMergedIssuePipelineInAnalyzer() {
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(analyzerSource.contains("fun detectBillingIssuesForRecord(record: EobRecord, allRecords: List<EobRecord>)"))
        assertTrue(analyzerSource.contains("CptGlobalPeriodCalculator.billingIssuesFor(record, allRecords)"))
        assertTrue(analyzerSource.contains("NcciBundlingCalculator.billingIssuesFor(record)"))
        assertTrue(viewModelSource.contains("EobAnalyzer.detectBillingIssuesForRecord(record, allRecords)"))
        assertTrue(viewModelSource.contains("fun bundlingAlertForCharge"))
    }

    @Test
    fun flaggedCountsAndCareTeamUseMergedBillingDetection() {
        val careTeamSource = readSource("data/CareTeamStateExtractor.kt")
        val bentoSource = readSource("data/BentoSnapshotExtractor.kt")
        assertTrue(careTeamSource.contains("detectBillingIssuesForRecord(record, records)"))
        assertTrue(bentoSource.contains("detectBillingIssuesForRecord(record, records)"))
    }

    @Test
    fun historyScreenAlertsUserForNcciAndBillingIssues() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(historySource.contains("historyBillingIssuesDetected"))
        assertTrue(historySource.contains("NcciUnbundlingAlertBubble"))
        assertTrue(historySource.contains("HistoryBillingIssuesBanner"))
        assertTrue(navSource.contains("bundlingAlertForCharge = eobViewModel::bundlingAlertForCharge"))
        assertTrue(navSource.contains("billingIssuesForRecord = eobViewModel::detectBillingIssuesForRecord"))
        assertTrue(navSource.contains("syncHubLanguage"))
        assertTrue(historySource.contains("BillingIssueFormatter.title"))
        assertTrue(navSource.contains("recordUpcodingVerificationAffirmed"))
        assertTrue(navSource.contains("recordUpcodingVerificationDisputed"))
    }

    @Test
    fun releaseBuildKeepsR8Disabled() {
        val gradleSource = readProjectFile("app/build.gradle.kts")
        assertTrue(gradleSource.contains("isMinifyEnabled = false"))
        assertTrue(gradleSource.contains("isShrinkResources = false"))
    }

    @Test
    fun manifestDeclaresPlayStorePermissionsWithoutCleartextOverrides() {
        val manifest = readProjectFile("app/src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android.permission.CAMERA"))
        assertTrue(manifest.contains("com.android.vending.BILLING"))
        assertFalse(manifest.contains("usesCleartextTraffic=\"true\""))
    }

    @Test
    fun mergedAnalyzerDetectionIncludesNcciUnbundling() {
        val record = EobRecord(
            id = 1,
            sourceName = "test",
            providerName = "Clinic",
            insuranceName = "Aetna",
            serviceDate = "03/15/2026",
            serviceDateSortKey = 20260315,
            charges = listOf(
                EobCharge(
                    cptCode = "99213",
                    cptDescription = "Office visit",
                    category = CptCategory.OfficeVisit,
                    billedAmount = 120.0,
                    insurancePaidAmount = 80.0,
                    contractualAdjustmentAmount = 10.0,
                    copayAmount = 20.0,
                    deductibleAmount = 10.0,
                    coinsuranceAmount = 0.0,
                    serviceDate = "03/15/2026"
                ),
                EobCharge(
                    cptCode = "36415",
                    cptDescription = "Venipuncture",
                    category = CptCategory.Lab,
                    billedAmount = 25.0,
                    insurancePaidAmount = 0.0,
                    contractualAdjustmentAmount = 0.0,
                    copayAmount = 25.0,
                    deductibleAmount = 0.0,
                    coinsuranceAmount = 0.0,
                    serviceDate = "03/15/2026"
                )
            ),
            duplicateChargeWarnings = emptyList(),
            rawText = "{}"
        )
        val issues = EobAnalyzer.detectBillingIssuesForRecord(record, listOf(record))
        assertTrue(issues.any { it.type == BillingIssueType.PossibleUnbundling })
        assertTrue(
            EobStrings.t(AppLanguage.English, "historyBillingIssuesDetected").contains("%d")
        )
    }

    @Test
    fun flaggedHistoryAndBentoUseFullHubCorpusForGlobalPeriod() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        assertTrue(viewModelSource.contains("recordsWithFlaggedBillingErrors(sorted, hubRecords)"))
        assertTrue(viewModelSource.contains("allRecords = _eobRecords.value"))
        assertTrue(analyzerSource.contains("allRecords: List<EobRecord> = records"))
    }

    @Test
    fun cameraSystemBackClearsReceiptScanState() {
        val cameraSource = readSource("ui/screens/CameraCaptureScreen.kt")
        assertTrue(cameraSource.contains("BackHandler(onBack = onClose)"))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr196() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("detectBillingIssuesForRecord"))
        assertFalse(pipelineSource.contains("NcciUnbundlingAlertBubble"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readProjectFile(relativePath: String): String {
        val candidates = listOf(
            File(relativePath),
            File("../$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
