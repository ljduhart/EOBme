package app.eob.me

import app.eob.me.data.CptCategory
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.FirebaseEobMapper
import app.eob.me.data.UserProfile
import app.eob.me.data.AppealLetterGenerator
import app.eob.me.data.BillingIssueType
import app.eob.me.data.ProviderNetworkAssurance
import app.eob.me.app.EobViewModel
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.navigation.EobRoute
import app.eob.me.navigation.primaryRoutes
import java.io.File
import org.junit.Test

import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun recognizesMajorInsuranceAndAmounts() {
        val record = EobAnalyzer.analyze(
            """
                Aetna Explanation of Benefits
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00 copay $30.00 deductible $25.00 coinsurance $20.00
            """.trimIndent(),
            "library",
            1
        )

        assertEquals("Aetna", record.insuranceName)
        assertEquals("Downtown Medical Group", record.providerName)
        assertEquals("02/03/2025", record.serviceDate)
        assertEquals(300.0, record.totalBilledAmount, 0.001)
        assertEquals(125.0, record.totalInsurancePaidAmount, 0.001)
        assertEquals(100.0, record.totalContractualAdjustmentAmount, 0.001)
        assertEquals(30.0, record.totalCopayAmount, 0.001)
        assertEquals(25.0, record.totalDeductibleAmount, 0.001)
        assertEquals(20.0, record.totalCoinsuranceAmount, 0.001)
    }

    @Test
    fun storesOnlyValidFiveCharacterCptCodes() {
        val codes = EobAnalyzer.validCptCodes("99215 01234 Z9999 A0425 J3301 123456")

        assertEquals(listOf("99215", "A0425", "J3301"), codes)
    }

    @Test
    fun cptUsageCountsByYearAndCategory() {
        val first = EobAnalyzer.analyze(
            """
                United Healthcare
                Provider: Lakeside Clinic
                Date of Service: 01/10/2025
                99215 billed $200.00 paid $80.00 adjustment $50.00
                80053 billed $40.00 paid $20.00 adjustment $10.00
            """.trimIndent(),
            "camera",
            1
        )
        val second = EobAnalyzer.analyze(
            """
                Blue Cross BlueShield
                Provider: Lakeside Clinic
                Date of Service: 03/15/2025
                99215 billed $210.00 paid $90.00 adjustment $60.00
            """.trimIndent(),
            "library",
            2
        )

        val usage = EobAnalyzer.cptUsage(listOf(first, second), 2025)
        val officeVisit = usage.first { it.info.code == "99215" }
        val lab = usage.first { it.info.code == "80053" }

        assertEquals(2, officeVisit.count)
        assertEquals(CptCategory.OfficeVisit, officeVisit.info.category)
        assertEquals(1, lab.count)
        assertEquals(CptCategory.Lab, lab.info.category)
    }

    @Test
    fun accuracyReviewFlagsMathMismatchAndMissingFields() {
        val record = EobAnalyzer.analyze(
            """
                Aetna
                Date of Service: 02/03/2025
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00 copay $10.00 deductible $10.00 coinsurance $0.00
            """.trimIndent(),
            "library",
            20
        )

        val review = EobAnalyzer.accuracyReview(record)

        assertFalse(review.mathValidation.isBalanced)
        assertTrue(review.warnings.any { it.contains("Billing math differs") })
        assertTrue(review.fields.any { it.fieldName == "Provider" && it.needsReview })
    }

    @Test
    fun smartBillingIssuesDetectMissingPaymentAndDenial() {
        val record = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                Claim denied as not covered.
                99215 billed $300.00 contractual adjustment $0.00 copay $0.00 deductible $300.00 coinsurance $0.00
            """.trimIndent(),
            "library",
            30
        )

        val issueTypes = EobAnalyzer.detectBillingIssues(record).map { it.type }.toSet()

        assertTrue(issueTypes.contains(BillingIssueType.MissingInsurancePayment))
        assertTrue(issueTypes.contains(BillingIssueType.PossibleDenial))
        assertTrue(issueTypes.contains(BillingIssueType.HighPatientResponsibility))
    }

    @Test
    fun appealLetterIncludesDetectedIssueTemplateSection() {
        val profile = UserProfile(
            firstName = "Lester",
            lastName = "Duhart",
            email = "member@example.com",
            password = "password1",
            city = "Atlanta",
            state = "GA"
        )
        val record = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                Claim denied as not covered.
                99215 billed $300.00 contractual adjustment $0.00 copay $0.00 deductible $300.00 coinsurance $0.00
            """.trimIndent(),
            "library",
            31
        )

        val letter = AppealLetterGenerator.generate(profile, record)

        assertTrue(letter.contains("Issues identified for review"))
        assertTrue(letter.contains("Possible denial language"))
    }

    @Test
    fun yearlyHealthCostSummaryTotalsLatestYear() {
        val first = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2026
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00 copay $30.00 deductible $25.00 coinsurance $20.00
            """.trimIndent(),
            "library",
            21
        )
        val previousYear = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                99215 billed $100.00 insurance paid $50.00 contractual adjustment $25.00 copay $10.00 deductible $10.00 coinsurance $5.00
            """.trimIndent(),
            "library",
            22
        )

        val summary = EobAnalyzer.yearlyHealthCostSummary(listOf(previousYear, first))

        assertEquals(2026, summary.year)
        assertEquals(1, summary.eobCount)
        assertEquals(300.0, summary.totalBilled, 0.001)
        assertEquals(75.0, summary.totalPatientResponsibility, 0.001)
    }

    @Test
    fun providerDirectorySummarizesProviders() {
        val first = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2026
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00 copay $30.00 deductible $25.00 coinsurance $20.00
            """.trimIndent(),
            "library",
            32
        )
        val second = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 03/03/2026
                99214 billed $200.00 insurance paid $100.00 contractual adjustment $50.00 copay $25.00 deductible $15.00 coinsurance $10.00
            """.trimIndent(),
            "library",
            33
        )

        val provider = EobAnalyzer.providerDirectory(listOf(first, second)).first()

        assertEquals("Downtown Medical Group", provider.providerName)
        assertEquals(2, provider.eobCount)
        assertEquals(500.0, provider.totalBilled, 0.001)
        assertEquals("03/03/2026", provider.lastServiceDate)
    }

    @Test
    fun libraryUploadDoesNotCreateFalseDuplicateWarning() {
        val record = EobAnalyzer.analyze(
            """
                Cigna
                Provider: North Hospital
                Date of Service: 04/01/2025
                99213 billed $160.00 paid $75.00 adjustment $50.00
                99214 billed $220.00 paid $100.00 adjustment $80.00
            """.trimIndent(),
            "library",
            3
        )

        assertTrue(record.duplicateChargeWarnings.isEmpty())
    }

    @Test
    fun duplicateWarningsRequireSameDateCodeAndBilledAmount() {
        val record = EobAnalyzer.analyze(
            """
                Humana
                Provider: North Hospital
                Date of Service: 04/01/2025
                99213 billed $160.00 paid $75.00 adjustment $50.00
                99213 billed $160.00 paid $75.00 adjustment $50.00
            """.trimIndent(),
            "camera",
            4
        )

        assertEquals(1, record.duplicateChargeWarnings.size)
    }

    @Test
    fun eobsSortByDateOfServiceFromBeginningOfYear() {
        val dates = listOf("03/01/2025", "01/02/2025", "02/10/2025")
            .sortedBy { EobAnalyzer.serviceDateSortKey(it) }

        assertEquals(listOf("01/02/2025", "02/10/2025", "03/01/2025"), dates)
    }

    @Test
    fun commonIcd10MemoryContainsDescriptions() {
        assertEquals("Essential hypertension.", EobKnowledgeBase.commonIcd10Codes["I10"])
        assertTrue(EobKnowledgeBase.commonIcd10Codes.containsKey("E11.9"))
    }

    @Test
    fun profileRequiresCityStateAndStrongPassword() {
        val weakProfile = UserProfile(
            firstName = "Lester",
            lastName = "Duhart",
            email = "member@example.com",
            password = "password",
            city = "Atlanta",
            state = "GA"
        )
        val completeProfile = weakProfile.copy(password = "password1")

        assertFalse(weakProfile.isComplete)
        assertTrue(completeProfile.isComplete)
    }

    @Test
    fun currentNewsUsesCurrentYear() {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()

        assertTrue(EobKnowledgeBase.currentNewsReleases().all { it.date.contains(currentYear) })
    }

    @Test
    fun firebaseMapperRoundTripsProfileAndEobData() {
        val profile = UserProfile(
            firstName = "Lester",
            lastName = "Duhart",
            email = "member@example.com",
            password = "private",
            city = "Atlanta",
            state = "GA",
            insuranceName = "Aetna",
            insuranceId = "SUB123",
            groupName = "GRP456",
            insuranceCardDownloadUrl = "https://firebasestorage.example/card.jpg"
        )
        val profileMap = FirebaseEobMapper.profileToMap(profile)
        val restoredProfile = FirebaseEobMapper.profileFromMap(profileMap, currentPassword = profile.password)
        val record = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00
            """.trimIndent(),
            "library",
            10
        )
        val restoredRecord = FirebaseEobMapper.eobFromMap(FirebaseEobMapper.eobToMap(record))

        assertEquals(profile, restoredProfile)
        assertEquals(record.insuranceName, restoredRecord.insuranceName)
        assertEquals(record.providerName, restoredRecord.providerName)
        assertEquals(record.serviceDateSortKey, restoredRecord.serviceDateSortKey)
        assertEquals(record.charges.first().cptCode, restoredRecord.charges.first().cptCode)
    }

    @Test
    fun firebaseMapperReadsLegacyEobRecordsFieldsWithAmountsAndCpts() {
        val legacyRecord = mapOf(
            "provider_name" to "Downtown Medical Group",
            "insurance_name" to "Aetna",
            "date_of_service" to "2026-02-03",
            "billed_amount" to 310.0,
            "insurance_paid" to 130.0,
            "contractual_adj" to 105.0,
            "copay" to 25.0,
            "deductible" to 40.0,
            "coinsurance" to 10.0,
            "cptCodes" to "99215,80053",
            "rawText" to "Aetna Provider: Downtown Medical Group 99215 80053"
        )

        val record = FirebaseEobMapper.eobFromMap(legacyRecord, documentId = "firebase-doc-1")

        assertEquals("Downtown Medical Group", record.providerName)
        assertEquals("Aetna", record.insuranceName)
        assertEquals("02/03/2026", record.serviceDate)
        assertEquals(310.0, record.totalBilledAmount, 0.001)
        assertEquals(130.0, record.totalInsurancePaidAmount, 0.001)
        assertEquals(105.0, record.totalContractualAdjustmentAmount, 0.001)
        assertEquals(25.0, record.totalCopayAmount, 0.001)
        assertEquals(40.0, record.totalDeductibleAmount, 0.001)
        assertEquals(10.0, record.totalCoinsuranceAmount, 0.001)
        assertEquals(listOf("99215", "80053"), record.charges.map { it.cptCode })
    }

    @Test
    fun cptUsageWorksForFirebaseSynthesizedCharges() {
        val record = FirebaseEobMapper.eobFromMap(
            mapOf(
                "provider_name" to "Downtown Medical Group",
                "insurance_name" to "Aetna",
                "date_of_service" to "02/03/2026",
                "billed_amount" to "$310.00",
                "insurance_paid" to "$130.00",
                "contractual_adj" to "$105.00",
                "cptCodes" to listOf("99215", "J3301")
            ),
            documentId = "firebase-doc-2"
        )

        val usage = EobAnalyzer.cptUsage(listOf(record), 2026)

        assertEquals(setOf("99215", "J3301"), usage.map { it.info.code }.toSet())
        assertEquals(1, usage.first { it.info.code == "99215" }.count)
        assertEquals(CptCategory.Injection, usage.first { it.info.code == "J3301" }.info.category)
    }

    @Test
    fun duplicateEobsMatchSameInsuranceProviderDateAndCpts() {
        val original = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00
            """.trimIndent(),
            "camera",
            1
        )
        val replacement = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                99215 billed $310.00 insurance paid $130.00 contractual adjustment $105.00
            """.trimIndent(),
            "library",
            2
        )
        val differentVisit = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/04/2025
                99215 billed $310.00 insurance paid $130.00 contractual adjustment $105.00
            """.trimIndent(),
            "library",
            3
        )

        assertTrue(EobAnalyzer.isSameEob(original, replacement))
        assertFalse(EobAnalyzer.isSameEob(original, differentVisit))
    }

    @Test
    fun providerNetworkAssuranceFlagsOutOfNetworkText() {
        val record = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Out of Network Clinic
                Date of Service: 02/03/2025
                Out-of-network provider billed $400.00 insurance paid $0.00
            """.trimIndent(),
            "library",
            40
        )

        assertEquals(ProviderNetworkAssurance.OutOfNetwork, EobAnalyzer.providerNetworkAssurance(record))
    }

    @Test
    fun providerDirectoryIncludesNetworkAssurance() {
        val inNetwork = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2026
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00
            """.trimIndent(),
            "library",
            41
        )

        val provider = EobAnalyzer.providerDirectory(listOf(inNetwork)).first()

        assertEquals(ProviderNetworkAssurance.InNetwork, provider.networkAssurance)
    }

    @Test
    fun appealGeneratorNavigationWiringUsesViewModelAndAppealRoute() {
        val navHostSource = File("src/main/java/app/eob/me/navigation/EobNavHost.kt")
            .takeIf { it.isFile }
            ?: File("app/src/main/java/app/eob/me/navigation/EobNavHost.kt")
        val homeSource = File("src/main/java/app/eob/me/ui/screens/HomeScreen.kt")
            .takeIf { it.isFile }
            ?: File("app/src/main/java/app/eob/me/ui/screens/HomeScreen.kt")
        val text = navHostSource.readText()
        val homeText = homeSource.readText()

        assertTrue(EobRoute.Appeal in primaryRoutes)
        assertTrue(text.contains("appealGeneratorSnapshot = viewModel.appealGeneratorSnapshot(language)"))
        assertTrue(text.contains("providers = viewModel.providerDirectory()"))
        assertTrue(text.contains("EobRoute.Appeal.route"))
        assertTrue(text.contains("popUpTo(EobRoute.Home.route)"))
        assertTrue(homeText.contains("AppealGeneratorCard"))
        assertTrue(text.contains("selectFlaggedClaim"))
        assertTrue(text.contains("appealLetterEditingEnabled"))
        assertTrue(text.contains("regenerateAppeal"))
        assertTrue(homeText.contains("onFlaggedClaimSelected"))
        val appealSource = File("src/main/java/app/eob/me/ui/screens/MainScreens.kt")
            .takeIf { it.isFile }
            ?: File("app/src/main/java/app/eob/me/ui/screens/MainScreens.kt")
        assertTrue(appealSource.readText().contains("autoFillAppeal"))
        assertTrue(appealSource.readText().contains("onAutoFill"))
    }

    @Test
    fun flaggedClaimSelectionPrefillsAppealAndLocksEditing() {
        val viewModel = EobViewModel()
        val flagged = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                Claim denied as not covered.
                99215 billed $300.00 contractual adjustment $0.00 copay $0.00 deductible $300.00 coinsurance $0.00
            """.trimIndent(),
            "library",
            60
        )
        val profile = UserProfile(
            firstName = "Lester",
            lastName = "Duhart",
            email = "member@example.com",
            password = "password1",
            city = "Atlanta",
            state = "GA"
        )
        viewModel.replaceRecords(listOf(flagged), profile)

        val snapshot = viewModel.appealGeneratorSnapshot(AppLanguage.English)
        assertEquals(1, snapshot.flaggedClaims.size)

        val selected = viewModel.selectFlaggedClaim(flagged.id, profile)
        assertTrue(selected)
        assertFalse(viewModel.appealLetterEditingEnabled)
        assertTrue(viewModel.appealLetter.contains("Downtown Medical Group"))
        assertTrue(viewModel.appealLetter.contains("Possible denial language"))
    }

    @Test
    fun regenerateAppealAutoFillsAndResetsEditing() {
        val viewModel = EobViewModel()
        val profile = UserProfile(
            firstName = "Lester",
            lastName = "Duhart",
            email = "member@example.com",
            password = "password1",
            city = "Atlanta",
            state = "GA"
        )
        viewModel.enableAppealLetterEditing()
        viewModel.updateAppeal("Manual edits in progress")
        viewModel.regenerateAppeal(profile)

        assertFalse(viewModel.appealLetterEditingEnabled)
        assertFalse(viewModel.appealLetter.contains("Manual edits in progress"))
        assertTrue(viewModel.appealLetter.contains("Lakeside Family Medical Clinic"))
    }

    @Test
    fun updateAppealIgnoredUntilEditEnabled() {
        val viewModel = EobViewModel()
        val original = viewModel.appealLetter
        viewModel.updateAppeal("Should not apply")
        assertEquals(original, viewModel.appealLetter)
    }

    @Test
    fun appealLetterEditingRequiresExplicitEditAndSave() {
        val viewModel = EobViewModel()
        val profile = UserProfile(
            firstName = "A",
            lastName = "B",
            email = "a@b.com",
            password = "password1",
            city = "X",
            state = "Y"
        )
        assertFalse(viewModel.appealLetterEditingEnabled)
        viewModel.enableAppealLetterEditing()
        assertTrue(viewModel.appealLetterEditingEnabled)
        viewModel.updateAppeal("Edited draft body")
        viewModel.saveAppealLetter()
        assertFalse(viewModel.appealLetterEditingEnabled)
        assertEquals("Edited draft body", viewModel.appealLetter)
    }

    @Test
    fun deleteRecordRoutesThroughReplaceRecordsAndClaimScan() {
        val viewModel = EobViewModel()
        val first = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Alpha Clinic
                Date of Service: 02/03/2026
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00
            """.trimIndent(),
            "library",
            50
        )
        val second = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Beta Clinic
                Date of Service: 03/03/2026
                99214 billed $200.00 insurance paid $100.00 contractual adjustment $50.00
            """.trimIndent(),
            "library",
            51
        )
        val profile = UserProfile(
            firstName = "A",
            lastName = "B",
            email = "a@b.com",
            password = "password1",
            city = "X",
            state = "Y"
        )
        viewModel.replaceRecords(listOf(first, second), profile)
        viewModel.deleteRecord(first, profile)

        assertEquals(1, viewModel.records.size)
        assertTrue(viewModel.claimScanComplete)
        assertEquals(second.id, viewModel.selectedRecord?.id)
    }

    @Test
    fun viewModelAppealGeneratorSnapshotReflectsDisputableClaims() {
        val viewModel = EobViewModel()
        val snapshot = viewModel.appealGeneratorSnapshot(AppLanguage.English)

        assertTrue(snapshot.claimScanComplete)
        assertTrue(snapshot.statusLine.isNotBlank())
        assertTrue(snapshot.summaryLine.isNotBlank())
    }

    @Test
    fun authLegalNoticeStringsExistForAllLanguages() {
        val keys = listOf(
            "privacyPolicy",
            "termsOfUse",
            "authLegalNoticePrefix",
            "authLegalNoticeMiddle",
            "authLegalNoticeSuffix"
        )
        AppLanguage.entries.forEach { language ->
            keys.forEach { key ->
                assertTrue(EobStrings.t(language, key).isNotBlank())
            }
        }
    }

    @Test
    fun compactDuplicateEobsKeepsOneReplacementCopy() {
        val original = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                99215 billed $300.00 insurance paid $125.00 contractual adjustment $100.00
            """.trimIndent(),
            "camera",
            1
        )
        val replacement = EobAnalyzer.analyze(
            """
                Aetna
                Provider: Downtown Medical Group
                Date of Service: 02/03/2025
                99215 billed $310.00 insurance paid $130.00 contractual adjustment $105.00
            """.trimIndent(),
            "library",
            2
        )

        val compacted = EobAnalyzer.compactDuplicateEobs(listOf(original, replacement))

        assertEquals(1, compacted.size)
        assertEquals(original.id, compacted.first().id)
        assertEquals(310.0, compacted.first().totalBilledAmount, 0.001)
    }
}