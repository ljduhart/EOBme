package app.eob.me.data

import java.util.Locale

enum class AppLanguage(val displayName: String) {
    English("English"),
    Spanish("Español"),
    French("Français"),
    Chinese("中文");

    fun locale(): Locale = when (this) {
        English -> Locale.ENGLISH
        Spanish -> Locale.forLanguageTag("es")
        French -> Locale.FRENCH
        Chinese -> Locale.SIMPLIFIED_CHINESE
    }
}

enum class CptCategory(val displayName: String) {
    OfficeVisit("OVs"),
    Lab("Labs"),
    Hospital("Hospital"),
    Dme("DME"),
    Injection("Injections"),
    Other("Other")
}

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val city: String = "",
    val state: String = "",
    val insuranceName: String = "",
    val insuranceId: String = "",
    val groupName: String = "",
    val insuranceCardDownloadUrl: String = "",
    val annualDeductibleLimit: Double = 0.0,
    val annualOutOfPocketMax: Double = 0.0,
    val hsaAllocation: Double = 0.0,
    val fsaAllocation: Double = 0.0,
    val pcpCopay: String = "",
    val specialistCopay: String = ""
) {
    val insuranceCompany: String get() = insuranceName
    val memberId: String get() = insuranceId
    val groupNumber: String get() = groupName

    val isComplete: Boolean
        get() = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            city.isNotBlank() &&
            state.isNotBlank()

    val fullName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    fun locationLine(): String = listOf(city, state).filter { it.isNotBlank() }.joinToString(", ")

    fun verificationFingerprint(): String {
        val seed = buildString {
            append(insuranceId)
            append('|')
            append(insuranceName)
            append('|')
            append(groupName)
            append('|')
            append(city)
            append('|')
            append(state)
        }
        return seed.hashCode().toUInt().toString(16).takeLast(7).padStart(7, '0')
    }

    /** Clamps plan limits for safe deductible-tracker math (ignores invalid stored values). */
    fun sanitizedPlanLimits(): UserProfile {
        return copy(
            annualDeductibleLimit = annualDeductibleLimit.coerceIn(0.0, 100_000.0),
            annualOutOfPocketMax = annualOutOfPocketMax.coerceIn(0.0, 200_000.0),
            hsaAllocation = hsaAllocation.coerceIn(0.0, 100_000.0),
            fsaAllocation = fsaAllocation.coerceIn(0.0, 100_000.0)
        )
    }
}

data class InsuranceCardDisplay(
    val insuranceName: String,
    val memberId: String,
    val groupNumber: String,
    val pcpCopay: String,
    val specialistCopay: String,
    val footerLocation: String,
    val verificationCode: String
)

data class RegistrationCredentials(val email: String = "", val password: String = "") {
    val isPasswordValid: Boolean
        get() = password.length >= 8 && password.any { it.isDigit() }

    fun isReadyForSignIn(): Boolean = email.isNotBlank() && password.isNotBlank()

    fun isReadyForSignUp(profile: UserProfile): Boolean =
        profile.isComplete && profile.email.isNotBlank() && isPasswordValid
}

data class CptInfo(
    val code: String,
    val description: String,
    val category: CptCategory
)

data class EobCharge(
    val cptCode: String,
    val cptDescription: String,
    val category: CptCategory,
    val billedAmount: Double,
    val insurancePaidAmount: Double,
    val contractualAdjustmentAmount: Double,
    val copayAmount: Double,
    val deductibleAmount: Double,
    val coinsuranceAmount: Double,
    val serviceDate: String
)

data class EobRecord(
    val id: Int,
    /** Firestore document id when loaded from or saved to Firebase (Veryfi uploads use hash ids). */
    val firestoreId: String = "",
    val sourceName: String,
    val providerName: String,
    val insuranceName: String,
    val serviceDate: String,
    val serviceDateSortKey: Int,
    val charges: List<EobCharge>,
    val duplicateChargeWarnings: List<String>,
    val rawText: String,
    val totalBilledAmount: Double = 0.0,
    val totalInsurancePaidAmount: Double = 0.0,
    val totalContractualAdjustmentAmount: Double = 0.0,
    val totalCopayAmount: Double = 0.0,
    val totalDeductibleAmount: Double = 0.0,
    val totalCoinsuranceAmount: Double = 0.0,
    val isHsaEligible: Boolean = false,
    val isFsaEligible: Boolean = false
) {
    val insuranceCompany: String
        get() = insuranceName

    val totalPatientResponsibility: Double
        get() = totalCopayAmount + totalDeductibleAmount + totalCoinsuranceAmount

    /** Stable LazyColumn key — never use numeric [id] alone (Firestore/Veryfi can collide). */
    fun historyListKey(): String {
        if (firestoreId.isNotBlank()) return "fs:$firestoreId"
        return "local:$id:$serviceDateSortKey:${providerName.trim().lowercase(Locale.US)}"
    }

    fun matchesHistoryRecord(other: EobRecord): Boolean = historyListKey() == other.historyListKey()
}

data class NewsRelease(
    val company: String,
    val headline: String,
    val summary: String,
    val date: String,
    val targetTags: List<String> = emptyList(),
    val baseRelevance: Int = 1,
    val articleUrl: String = ""
) {
    fun resolvedArticleUrl(): String {
        if (articleUrl.isNotBlank()) return articleUrl
        return summary.lineSequence()
            .map { it.trim() }
            .lastOrNull { line -> line.startsWith("https://") || line.startsWith("http://") }
            .orEmpty()
    }

    fun displaySummary(): String {
        val url = resolvedArticleUrl()
        if (url.isBlank()) return summary
        return summary.replace("\n\n$url", "")
            .replace(url, "")
            .trim()
    }
}

data class InsuranceNewsCarrierHubItem(
    val carrier: MajorInsuranceCarrier,
    val monthlyBriefingCount: Int,
    val featuredArticle: InsuranceArticle?
)

data class CptUsage(
    val info: CptInfo,
    val year: Int,
    val count: Int
)

enum class CareTeamProviderType {
    Pcp,
    Dentist,
    Specialist,
    Therapist;

    companion object {
        val displayOrder: List<CareTeamProviderType> = listOf(Pcp, Dentist, Specialist, Therapist)
    }
}

data class PreferredDoctor(
    val type: CareTeamProviderType,
    val name: String = "",
    val specialty: String = "",
    val address: String = "",
    val phone: String = ""
) {
    val isAssigned: Boolean
        get() = name.isNotBlank()
}

/** Network assurance for provider directory and care-team cards (derived from EOB + upload state). */
enum class NetworkAssuranceState {
    FullyAssured,
    VerificationPending,
    OutOfNetworkAlert
}

enum class TherapistNetworkStatus {
    Unknown,
    InNetwork,
    OutOfNetwork
}

/** Compact metrics shown on each gold care-team card. */
data class CareTeamMicroMetrics(
    val relatedEobCount: Int = 0,
    val upcomingAppointments: Int = 0,
    val flaggedIssueCount: Int = 0
)

/**
 * UI-ready care-team card model built by [CareTeamStateExtractor] from hub state + EOB records.
 */
data class CareTeamCardDisplayState(
    val type: CareTeamProviderType,
    val isAssigned: Boolean,
    val assuranceState: NetworkAssuranceState,
    val metrics: CareTeamMicroMetrics,
    val primaryLine: String,
    val secondaryLine: String,
    val tertiaryLine: String? = null,
    val phoneDialUri: String? = null,
    val specialistReferralActive: Boolean = false,
    val therapistNetworkStatus: TherapistNetworkStatus = TherapistNetworkStatus.Unknown,
    val therapistCopayAmount: Double? = null
)

enum class YtdBentoViewMode {
    CostOverview,
    DeductibleTracker
}

data class CptBentoSnapshot(
    val dominantCode: String,
    val translatorLine: String,
    val ringProgress: Float,
    val priceGaugePosition: Float,
    val priceTrendPoints: List<Float>,
    val trendDirection: PriceTrendDirection
)

data class CptCodeEntry(
    val code: String,
    val category: String,
    val shortName: String,
    val definition: String,
    val totalBilled: String
)

enum class PriceTrendDirection {
    BelowFair,
    NearFair,
    AboveFair
}

data class InsuranceNewsBentoSnapshot(
    val tickerHeadlines: List<String>,
    val previewHeadline: String,
    val previewCompany: String,
    val criticalAlertActive: Boolean,
    val actionSummary: String
)

data class YtdDeductibleBentoSnapshot(
    val year: Int,
    val totalBilled: Double,
    val totalPatientResponsibility: Double,
    val deductiblePaidYtd: Double,
    val copayPaidYtd: Double,
    val coinsurancePaidYtd: Double,
    val deductibleLimit: Double,
    val outOfPocketMax: Double,
    val deductibleProgress: Float,
    val outOfPocketProgress: Float,
    val monthlyBilledNormalized: List<Float>,
    val trajectoryNormalized: List<Float>,
    val spendingVelocity: Float,
    val onTrackEarlyDeductible: Boolean
)

/** Provider-directory bento network badge snapshot. */
data class ProviderDirectoryAssurance(
    val state: NetworkAssuranceState,
    val statusLabel: String,
    val showWarningDot: Boolean
)

data class DoctorAppointment(
    val id: Int,
    val date: String,
    val providerName: String,
    val time: String,
    val notes: String,
    val providerType: CareTeamProviderType = CareTeamProviderType.Pcp
)

enum class TaxVaultFilterState {
    OFF,
    HSA,
    FSA
}

enum class TaxVaultVisibilityMode {
    ALL,
    GATED,
    OPEN;

    fun labelKey(): String = when (this) {
        ALL -> "taxVaultVisibilityAll"
        GATED -> "taxVaultVisibilityGated"
        OPEN -> "taxVaultVisibilityOpen"
    }
}

data class TaxVaultBudgetSummary(
    val eligibleAmount: Double,
    val allocationLimit: Double,
    val savedAmount: Double = 0.0
)

enum class HistoryBentoFilter {
    All,
    Flagged
}

enum class EobHistoryPaymentFilter {
    All,
    Paid,
    Pending
}

data class HistoryTimelineRow(
    val record: EobRecord,
    val isFirstInMonth: Boolean,
    val isLastInMonth: Boolean
)

enum class InvoiceProcessingPhase {
    Idle,
    Processing,
    FileDropReveal
}

enum class CameraScanDocumentType {
    Eob,
    Receipt
}

sealed class DocumentScanPipelineState {
    data object Idle : DocumentScanPipelineState()
    data object LocalScanning : DocumentScanPipelineState()
    data object OcrPreCheck : DocumentScanPipelineState()
    data object UploadingAndProcessing : DocumentScanPipelineState()
    data class Success(val result: EobProcessedResult) : DocumentScanPipelineState()
    data class Error(val message: String) : DocumentScanPipelineState()
}

data class DocumentUploadResult(
    val storagePath: String,
    val downloadUrl: String,
    val contentType: String,
    val fileName: String,
    val documentRefId: String
)

data class VeryfiStreamExtraction(
    val documentRefId: String,
    val sourceFilePath: String,
    val payload: Map<String, Any?>
)

data class HistoryBentoSnapshot(
    val monthlySpend: List<Double>,
    val cornerstoneQuadrants: List<Float>,
    val flaggedBillingErrorCount: Int
)

data class ProviderAvatarPreview(
    val initials: String,
    val displayName: String,
    val specialtyLabel: String
)

data class EobFieldConfidence(
    val fieldName: String,
    val value: String,
    val confidencePercent: Int,
    val needsReview: Boolean
)

data class EobMathValidation(
    val expectedPatientResponsibility: Double,
    val extractedPatientResponsibility: Double,
    val difference: Double,
    val isBalanced: Boolean
)

data class EobAccuracyReview(
    val overallConfidencePercent: Int,
    val fields: List<EobFieldConfidence>,
    val mathValidation: EobMathValidation,
    val warnings: List<String>
)

data class YearlyHealthCostSummary(
    val year: Int,
    val eobCount: Int,
    val totalBilled: Double,
    val totalInsurancePaid: Double,
    val totalContractualAdjustment: Double,
    val totalCopay: Double,
    val totalDeductible: Double,
    val totalCoinsurance: Double
) {
    val totalPatientResponsibility: Double
        get() = totalCopay + totalDeductible + totalCoinsurance
}

data class YtdExpenseData(
    val year: Int,
    val eobCount: Int,
    val totalBilled: Double,
    val insurancePaid: Double,
    val adjustments: Double,
    val patientResponsibility: Double,
    val copays: Double,
    val deductibles: Double,
    val coinsurance: Double,
    val deductibleMax: Double,
    val outOfPocketMax: Double
) {
    val deductibleProgress: Float
        get() = (deductibles / deductibleMax.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

    val outOfPocketProgress: Float
        get() = (patientResponsibility / outOfPocketMax.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
}

enum class BillingIssueType {
    DuplicateCharge,
    MathMismatch,
    MissingInsurancePayment,
    HighPatientResponsibility,
    MissingProvider,
    MissingInsurance,
    MissingDateOfService,
    MissingCptCode,
    PossibleDenial
}

enum class BillingIssueSeverity {
    Info,
    Warning,
    Critical
}

data class BillingIssue(
    val type: BillingIssueType,
    val severity: BillingIssueSeverity,
    val title: String,
    val explanation: String,
    val recommendedAction: String
)

data class ProviderSummary(
    val providerName: String,
    val eobCount: Int,
    val totalBilled: Double,
    val totalInsurancePaid: Double,
    val totalPatientResponsibility: Double,
    val lastServiceDate: String
)
