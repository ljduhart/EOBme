package app.eob.me.data

import java.util.Calendar
import java.util.Locale

object EobKnowledgeBase {
    val insuranceNames = listOf(
        "Aetna",
        "United Healthcare",
        "UnitedHealthcare",
        "Blue Cross BlueShield",
        "Blue Cross Blue Shield",
        "BCBS",
        "Anthem",
        "Cigna",
        "Humana",
        "Kaiser Permanente",
        "Centene",
        "Molina Healthcare",
        "Elevance Health",
        "Oscar Health",
        "Ambetter",
        "WellCare",
        "CareFirst",
        "Highmark",
        "Independence Blue Cross",
        "Health Net",
        "EmblemHealth",
        "AmeriHealth",
        "Medica",
        "Premera Blue Cross",
        "Regence",
        "Florida Blue",
        "Moda Health",
        "Tufts Health Plan",
        "Point32Health",
        "Geisinger Health Plan",
        "UPMC Health Plan",
        "Priority Health",
        "Blue Shield of California",
        "Medical Mutual",
        "Oscar",
        "Tricare",
        "Medicare",
        "Medicaid",
        "Optum",
        "Meritain Health",
        "Alliant Health Plans",
        "Devoted Health",
        "Bright Health",
        "CareSource",
        "Quartz",
        "Dean Health Plan",
        "Security Health Plan",
        "Voya",
        "Guardian",
        "MetLife",
        "Delta Dental",
        "VSP",
        "EyeMed"
    )

    val cptDescriptions = listOf(
        CptInfo("99202", "New patient office visit, straightforward medical decision making.", CptCategory.OfficeVisit),
        CptInfo("99203", "New patient office visit, low complexity.", CptCategory.OfficeVisit),
        CptInfo("99204", "New patient office visit, moderate complexity.", CptCategory.OfficeVisit),
        CptInfo("99205", "New patient office visit, high complexity.", CptCategory.OfficeVisit),
        CptInfo("99211", "Established patient office visit, minimal problem.", CptCategory.OfficeVisit),
        CptInfo("99212", "Established patient office visit, straightforward medical decision making.", CptCategory.OfficeVisit),
        CptInfo("99213", "Established patient office visit, low complexity.", CptCategory.OfficeVisit),
        CptInfo("99214", "Established patient office visit, moderate complexity.", CptCategory.OfficeVisit),
        CptInfo("99215", "Established patient office visit, high complexity.", CptCategory.OfficeVisit),
        CptInfo("99381", "Preventive medicine visit for a new infant patient.", CptCategory.OfficeVisit),
        CptInfo("99385", "Preventive medicine visit for a new adult patient.", CptCategory.OfficeVisit),
        CptInfo("99395", "Preventive medicine visit for an established adult patient.", CptCategory.OfficeVisit),
        CptInfo("99221", "Initial hospital inpatient care, low complexity.", CptCategory.Hospital),
        CptInfo("99222", "Initial hospital inpatient care, moderate complexity.", CptCategory.Hospital),
        CptInfo("99223", "Initial hospital inpatient care, high complexity.", CptCategory.Hospital),
        CptInfo("80035", "Thyroid panel.", CptCategory.Lab),
        CptInfo("80053", "Comprehensive metabolic panel.", CptCategory.Lab),
        CptInfo("80061", "Lipid panel.", CptCategory.Lab),
        CptInfo("81001", "Urinalysis with microscopy.", CptCategory.Lab),
        CptInfo("81004", "Urinalysis without microscopy.", CptCategory.Lab),
        CptInfo("83036", "Hemoglobin A1c test.", CptCategory.Lab),
        CptInfo("84443", "Thyroid stimulating hormone test.", CptCategory.Lab),
        CptInfo("85025", "Complete blood count with automated differential.", CptCategory.Lab),
        CptInfo("85610", "Prothrombin time blood test.", CptCategory.Lab),
        CptInfo("87086", "Urine culture.", CptCategory.Lab),
        CptInfo("93000", "Electrocardiogram with interpretation and report.", CptCategory.OfficeVisit),
        CptInfo("36415", "Routine venipuncture blood draw.", CptCategory.Lab),
        CptInfo("71046", "Chest X-ray, two views.", CptCategory.XRay),
        CptInfo("73562", "Knee X-ray, three views.", CptCategory.XRay),
        CptInfo("74177", "CT abdomen and pelvis with contrast.", CptCategory.XRay),
        CptInfo("70553", "MRI brain without and with contrast.", CptCategory.XRay),
        CptInfo("72148", "MRI lumbar spine without contrast.", CptCategory.XRay),
        CptInfo("45378", "Diagnostic colonoscopy.", CptCategory.Hospital),
        CptInfo("43239", "Upper GI endoscopy with biopsy.", CptCategory.Hospital),
        CptInfo("A0425", "Ground mileage for ambulance transport.", CptCategory.Dme),
        CptInfo("A0427", "Advanced life support emergency transport.", CptCategory.Dme),
        CptInfo("A4253", "Blood glucose test strips.", CptCategory.Dme),
        CptInfo("A7030", "Full face CPAP mask.", CptCategory.Dme),
        CptInfo("A7034", "Nasal CPAP mask.", CptCategory.Dme),
        CptInfo("A7035", "Headgear used with positive airway pressure device.", CptCategory.Dme),
        CptInfo("J1100", "Dexamethasone sodium phosphate injection.", CptCategory.Injection),
        CptInfo("J1885", "Ketorolac tromethamine injection.", CptCategory.Injection),
        CptInfo("J3301", "Triamcinolone acetonide injection.", CptCategory.Injection),
        CptInfo("J3420", "Vitamin B-12 injection.", CptCategory.Injection),
        CptInfo("J0696", "Ceftriaxone sodium injection.", CptCategory.Injection),
        CptInfo("J0013", "Epinephrine injection.", CptCategory.Injection),
        CptInfo("J0081", "Phenylephrine hydrochloride injection.", CptCategory.Injection)
    )

    val commonIcd10Codes = mapOf(
        "A09" to "Infectious gastroenteritis and colitis.",
        "B34.9" to "Viral infection, unspecified.",
        "E03.9" to "Hypothyroidism, unspecified.",
        "E11.9" to "Type 2 diabetes mellitus without complications.",
        "E66.9" to "Obesity, unspecified.",
        "E78.5" to "Hyperlipidemia, unspecified.",
        "F32.A" to "Depression, unspecified.",
        "F41.9" to "Anxiety disorder, unspecified.",
        "G43.909" to "Migraine, unspecified, not intractable.",
        "I10" to "Essential hypertension.",
        "I25.10" to "Atherosclerotic heart disease without angina.",
        "J01.90" to "Acute sinusitis, unspecified.",
        "J06.9" to "Acute upper respiratory infection.",
        "J20.9" to "Acute bronchitis, unspecified.",
        "J30.9" to "Allergic rhinitis, unspecified.",
        "J45.909" to "Asthma, uncomplicated.",
        "K21.9" to "Gastro-esophageal reflux disease without esophagitis.",
        "M25.50" to "Pain in unspecified joint.",
        "M54.2" to "Cervicalgia.",
        "M54.50" to "Low back pain, unspecified.",
        "N39.0" to "Urinary tract infection, site not specified.",
        "R05.9" to "Cough, unspecified.",
        "R07.9" to "Chest pain, unspecified.",
        "R10.9" to "Unspecified abdominal pain.",
        "R21" to "Rash and other nonspecific skin eruption.",
        "R42" to "Dizziness and giddiness.",
        "R50.9" to "Fever, unspecified.",
        "R51.9" to "Headache, unspecified.",
        "R53.83" to "Other fatigue.",
        "Z00.00" to "General adult medical examination without abnormal findings.",
        "Z01.419" to "Gynecological examination without abnormal findings.",
        "Z12.11" to "Screening for malignant neoplasm of colon.",
        "Z23" to "Encounter for immunization.",
        "Z79.899" to "Other long term drug therapy."
    )

    val newsReleases = listOf(
        NewsRelease(
            company = "UnitedHealthcare",
            headline = "Prior authorization simplification efforts continue",
            summary = "UnitedHealthcare has continued publishing updates about reducing administrative burden and improving digital prior authorization workflows.",
            date = "2025"
        ),
        NewsRelease(
            company = "Blue Cross Blue Shield companies",
            headline = "Member affordability and transparency remain major initiatives",
            summary = "BCBS plans have highlighted programs focused on cost transparency, preventive care access, and digital member tools.",
            date = "2025"
        ),
        NewsRelease(
            company = "Aetna",
            headline = "Expanded digital navigation and value-based care messaging",
            summary = "Aetna and CVS Health updates have emphasized connected care, plan navigation, and lower-friction access to coverage information.",
            date = "2025"
        ),
        NewsRelease(
            company = "Cigna Healthcare",
            headline = "Behavioral health and affordability programs highlighted",
            summary = "Cigna Healthcare has released updates focused on care access, transparency, and whole-person health support.",
            date = "2025"
        ),
        NewsRelease(
            company = "Humana",
            headline = "Medicare Advantage member support updates",
            summary = "Humana communications have focused on Medicare Advantage benefits, member education, and coordinated care programs.",
            date = "2025"
        )
    )

    fun currentNewsReleases(source: List<NewsRelease> = newsReleases): List<NewsRelease> {
        val calendar = Calendar.getInstance()
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.US) ?: "Current"
        val year = calendar.get(Calendar.YEAR).toString()
        return source.map { release ->
            release.copy(
                date = "$month $year",
                headline = "${release.company}: ${release.headline}",
                summary = "${release.summary} Updated for $month so users see a current monthly insurance news feed."
            )
        }
    }

    fun cptInfoFor(code: String): CptInfo {
        val normalized = code.trim().uppercase(Locale.US)
        if (normalized.isBlank()) {
            return CptInfo(
                code = "",
                description = "",
                category = CptCategory.Other
            )
        }
        return cptDescriptions.firstOrNull { it.code.equals(normalized, ignoreCase = true) }
            ?: CptInfo(
                code = normalized,
                description = "CPT/HCPCS code recognized; review the EOB for payer-specific details.",
                category = inferCategoryFromCodePrefix(normalized)
            )
    }

    internal fun inferCategoryFromCodePrefix(code: String): CptCategory {
        return when {
            code.startsWith("J") -> CptCategory.Injection
            code.startsWith("A") -> CptCategory.Dme
            code.startsWith("D") -> CptCategory.Other
            else -> {
                val numeric = code.filter { it.isDigit() }.take(5).toIntOrNull()
                if (numeric != null && numeric in 70000..79999) {
                    CptCategory.XRay
                } else {
                    CptCategory.Other
                }
            }
        }
    }
}
