package app.eob.me.data

/**
 * Local CMS global surgical period lookup — no Firebase required.
 * Drives EOB History thought-bubble alerts and CPT tracker category sync.
 */
data class CptGlobalPeriodEntry(
    val code: String,
    val globalDays: Int,
    val procedureDescription: String
)

object CptGlobalPeriodMap {
    val entries: List<CptGlobalPeriodEntry> = listOf(
        // 90-Day Major Surgeries
        CptGlobalPeriodEntry("19301", 90, "Partial Mastectomy"),
        CptGlobalPeriodEntry("22551", 90, "Anterior Cervical Discectomy and Fusion"),
        CptGlobalPeriodEntry("22612", 90, "Lumbar Spine Fusion"),
        CptGlobalPeriodEntry("27130", 90, "Total Hip Arthroplasty (Replacement)"),
        CptGlobalPeriodEntry("27236", 90, "Femur Fracture Repair (Open)"),
        CptGlobalPeriodEntry("27446", 90, "Partial Knee Arthroplasty"),
        CptGlobalPeriodEntry("27447", 90, "Total Knee Arthroplasty (Replacement)"),
        CptGlobalPeriodEntry("29827", 90, "Arthroscopic Rotator Cuff Repair"),
        CptGlobalPeriodEntry("29881", 90, "Arthroscopic Meniscectomy (Knee)"),
        CptGlobalPeriodEntry("33405", 90, "Aortic Valve Replacement"),
        CptGlobalPeriodEntry("33430", 90, "Mitral Valve Replacement"),
        CptGlobalPeriodEntry("33533", 90, "Coronary Artery Bypass Graft (CABG), Single"),
        CptGlobalPeriodEntry("35301", 90, "Carotid Endarterectomy"),
        CptGlobalPeriodEntry("39540", 90, "Diaphragmatic Hernia Repair"),
        CptGlobalPeriodEntry("43280", 90, "Laparoscopic Fundoplication"),
        CptGlobalPeriodEntry("43281", 90, "Laparoscopic Hiatal Hernia Repair"),
        CptGlobalPeriodEntry("44140", 90, "Colectomy (Partial)"),
        CptGlobalPeriodEntry("44204", 90, "Laparoscopic Colectomy"),
        CptGlobalPeriodEntry("44950", 90, "Appendectomy"),
        CptGlobalPeriodEntry("45395", 90, "Laparoscopic Proctectomy"),
        CptGlobalPeriodEntry("47120", 90, "Partial Hepatectomy (Liver)"),
        CptGlobalPeriodEntry("47562", 90, "Laparoscopic Cholecystectomy (Gallbladder)"),
        CptGlobalPeriodEntry("47600", 90, "Open Cholecystectomy"),
        CptGlobalPeriodEntry("48140", 90, "Pancreatectomy"),
        CptGlobalPeriodEntry("49505", 90, "Inguinal Hernia Repair"),
        CptGlobalPeriodEntry("50220", 90, "Nephrectomy (Kidney Removal)"),
        CptGlobalPeriodEntry("50544", 90, "Laparoscopic Partial Nephrectomy"),
        CptGlobalPeriodEntry("52601", 90, "Transurethral Resection of the Prostate (TURP)"),
        CptGlobalPeriodEntry("55866", 90, "Laparoscopic Radical Prostatectomy"),
        CptGlobalPeriodEntry("58150", 90, "Total Abdominal Hysterectomy"),
        CptGlobalPeriodEntry("58570", 90, "Laparoscopic Total Hysterectomy"),
        CptGlobalPeriodEntry("58940", 90, "Oophorectomy (Ovary Removal)"),
        CptGlobalPeriodEntry("59510", 90, "Cesarean Delivery (Global Maternity)"),
        CptGlobalPeriodEntry("60220", 90, "Total Thyroidectomy"),
        CptGlobalPeriodEntry("60240", 90, "Total Thyroidectomy (Complete)"),
        CptGlobalPeriodEntry("61510", 90, "Craniotomy for Brain Tumor"),
        CptGlobalPeriodEntry("63047", 90, "Lumbar Laminectomy"),
        CptGlobalPeriodEntry("63267", 90, "Lumbar Laminectomy (Spine)"),
        CptGlobalPeriodEntry("66984", 90, "Cataract Surgery with IOL"),
        CptGlobalPeriodEntry("69950", 90, "Vestibular Nerve Section"),
        // 10-Day Minor Surgeries
        CptGlobalPeriodEntry("10060", 10, "Incision & Drainage (I&D) of Abscess, simple"),
        CptGlobalPeriodEntry("10061", 10, "Incision & Drainage of Abscess, complicated"),
        CptGlobalPeriodEntry("10080", 10, "I&D of Pilonidal Cyst, simple"),
        CptGlobalPeriodEntry("10081", 10, "I&D of Pilonidal Cyst, complicated"),
        CptGlobalPeriodEntry("10120", 10, "Incision & Removal of Foreign Body, simple"),
        CptGlobalPeriodEntry("10121", 10, "Incision & Removal of Foreign Body, complicated"),
        CptGlobalPeriodEntry("10140", 10, "Incision & Drainage of Hematoma"),
        CptGlobalPeriodEntry("10160", 10, "Puncture Aspiration of Abscess"),
        CptGlobalPeriodEntry("10180", 10, "I&D, Complex, Postoperative Wound Infection"),
        CptGlobalPeriodEntry("11010", 10, "Debridement of Skin at Fracture Site"),
        CptGlobalPeriodEntry("11200", 10, "Removal of Skin Tags"),
        CptGlobalPeriodEntry("11400", 10, "Excision, Benign Lesion, Trunk/Arms/Legs (0.5 cm or less)"),
        CptGlobalPeriodEntry("11401", 10, "Excision, Benign Lesion, Trunk/Arms/Legs (0.6 to 1.0 cm)"),
        CptGlobalPeriodEntry("11402", 10, "Excision, Benign Lesion, Trunk/Arms/Legs (1.1 to 2.0 cm)"),
        CptGlobalPeriodEntry("11403", 10, "Excision, Benign Lesion, Trunk/Arms/Legs (2.1 to 3.0 cm)"),
        CptGlobalPeriodEntry("11404", 10, "Excision, Benign Lesion, Trunk/Arms/Legs (3.1 to 4.0 cm)"),
        CptGlobalPeriodEntry("11420", 10, "Excision, Benign Lesion, Scalp/Neck/Hands (0.5 cm or less)"),
        CptGlobalPeriodEntry("11421", 10, "Excision, Benign Lesion, Scalp/Neck/Hands (0.6 to 1.0 cm)"),
        CptGlobalPeriodEntry("11422", 10, "Excision, Benign Lesion, Scalp/Neck/Hands (1.1 to 2.0 cm)"),
        CptGlobalPeriodEntry("11423", 10, "Excision, Benign Lesion, Scalp/Neck/Hands (2.1 to 3.0 cm)"),
        CptGlobalPeriodEntry("11440", 10, "Excision, Benign Lesion, Face/Ears/Lips (0.5 cm or less)"),
        CptGlobalPeriodEntry("11441", 10, "Excision, Benign Lesion, Face/Ears/Lips (0.6 to 1.0 cm)"),
        CptGlobalPeriodEntry("11442", 10, "Excision, Benign Lesion, Face/Ears/Lips (1.1 to 2.0 cm)"),
        CptGlobalPeriodEntry("11600", 10, "Excision, Malignant Lesion, Trunk/Arms/Legs (0.5 cm or less)"),
        CptGlobalPeriodEntry("11601", 10, "Excision, Malignant Lesion, Trunk/Arms/Legs (0.6 to 1.0 cm)"),
        CptGlobalPeriodEntry("11602", 10, "Excision, Malignant Lesion, Trunk/Arms/Legs (1.1 to 2.0 cm)"),
        CptGlobalPeriodEntry("11620", 10, "Excision, Malignant Lesion, Scalp/Neck/Hands (0.5 cm or less)"),
        CptGlobalPeriodEntry("11621", 10, "Excision, Malignant Lesion, Scalp/Neck/Hands (0.6 to 1.0 cm)"),
        CptGlobalPeriodEntry("11640", 10, "Excision, Malignant Lesion, Face/Ears/Lips (0.5 cm or less)"),
        CptGlobalPeriodEntry("11641", 10, "Excision, Malignant Lesion, Face/Ears/Lips (0.6 to 1.0 cm)"),
        CptGlobalPeriodEntry("11750", 10, "Excision of Nail and Nail Matrix"),
        CptGlobalPeriodEntry("11760", 10, "Repair of Nail Bed"),
        CptGlobalPeriodEntry("12001", 10, "Simple Wound Repair, Trunk/Extremities (2.5 cm or less)"),
        CptGlobalPeriodEntry("12002", 10, "Simple Wound Repair, Trunk/Extremities (2.6 to 7.5 cm)"),
        CptGlobalPeriodEntry("12011", 10, "Simple Wound Repair, Face/Ears/Lips (2.5 cm or less)"),
        CptGlobalPeriodEntry("12013", 10, "Simple Wound Repair, Face/Ears/Lips (2.6 to 5.0 cm)"),
        CptGlobalPeriodEntry("12031", 10, "Intermediate Wound Repair, Trunk/Extremities (2.5 cm or less)"),
        CptGlobalPeriodEntry("12032", 10, "Intermediate Wound Repair, Trunk/Extremities (2.6 to 7.5 cm)"),
        CptGlobalPeriodEntry("12041", 10, "Intermediate Wound Repair, Neck/Hands/Feet (2.5 cm or less)"),
        CptGlobalPeriodEntry("12051", 10, "Intermediate Wound Repair, Face/Ears/Lips (2.5 cm or less)")
    )

    private val globalDaysByCode: Map<String, Int> = entries.associate { entry ->
        entry.code.uppercase() to entry.globalDays
    }

    private val entryByCode: Map<String, CptGlobalPeriodEntry> = entries.associateBy { entry ->
        entry.code.uppercase()
    }

    fun globalDaysFor(code: String): Int? {
        return globalDaysByCode[code.trim().uppercase()]
    }

    fun entryFor(code: String): CptGlobalPeriodEntry? {
        return entryByCode[code.trim().uppercase()]
    }
}
