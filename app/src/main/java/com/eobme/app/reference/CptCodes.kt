package com.eobme.app.reference

object CptCodes {

    data class CptInfo(
        val code: String,
        val description: String,
        val category: String
    )

    const val CATEGORY_OV = "OV"
    const val CATEGORY_LAB = "LAB"
    const val CATEGORY_HOSPITAL = "HOSPITAL"
    const val CATEGORY_DME = "DME"
    const val CATEGORY_INJECTION = "INJECTION"
    const val CATEGORY_RADIOLOGY = "RADIOLOGY"
    const val CATEGORY_SURGERY = "SURGERY"
    const val CATEGORY_OTHER = "OTHER"

    val allCategories = listOf(CATEGORY_OV, CATEGORY_LAB, CATEGORY_HOSPITAL, CATEGORY_DME, CATEGORY_INJECTION, CATEGORY_RADIOLOGY, CATEGORY_SURGERY, CATEGORY_OTHER)

    val all: Map<String, CptInfo> = buildMap {
        // Office Visits - New Patient
        put("99201", CptInfo("99201", "Office visit, new patient, minimal", CATEGORY_OV))
        put("99202", CptInfo("99202", "Office visit, new patient, straightforward", CATEGORY_OV))
        put("99203", CptInfo("99203", "Office visit, new patient, low complexity", CATEGORY_OV))
        put("99204", CptInfo("99204", "Office visit, new patient, moderate complexity", CATEGORY_OV))
        put("99205", CptInfo("99205", "Office visit, new patient, high complexity", CATEGORY_OV))

        // Office Visits - Established Patient
        put("99211", CptInfo("99211", "Office visit, established patient, minimal", CATEGORY_OV))
        put("99212", CptInfo("99212", "Office visit, established patient, straightforward", CATEGORY_OV))
        put("99213", CptInfo("99213", "Office visit, established patient, low complexity", CATEGORY_OV))
        put("99214", CptInfo("99214", "Office visit, established patient, moderate complexity", CATEGORY_OV))
        put("99215", CptInfo("99215", "Office visit, established patient, high complexity", CATEGORY_OV))

        // Telehealth
        put("99441", CptInfo("99441", "Telephone E/M, 5-10 minutes", CATEGORY_OV))
        put("99442", CptInfo("99442", "Telephone E/M, 11-20 minutes", CATEGORY_OV))
        put("99443", CptInfo("99443", "Telephone E/M, 21-30 minutes", CATEGORY_OV))

        // Preventive - New
        put("99381", CptInfo("99381", "Preventive visit, new, infant", CATEGORY_OV))
        put("99382", CptInfo("99382", "Preventive visit, new, age 1-4", CATEGORY_OV))
        put("99383", CptInfo("99383", "Preventive visit, new, age 5-11", CATEGORY_OV))
        put("99384", CptInfo("99384", "Preventive visit, new, age 12-17", CATEGORY_OV))
        put("99385", CptInfo("99385", "Preventive visit, new, age 18-39", CATEGORY_OV))
        put("99386", CptInfo("99386", "Preventive visit, new, age 40-64", CATEGORY_OV))
        put("99387", CptInfo("99387", "Preventive visit, new, age 65+", CATEGORY_OV))

        // Preventive - Established
        put("99391", CptInfo("99391", "Preventive visit, established, infant", CATEGORY_OV))
        put("99392", CptInfo("99392", "Preventive visit, established, age 1-4", CATEGORY_OV))
        put("99393", CptInfo("99393", "Preventive visit, established, age 5-11", CATEGORY_OV))
        put("99394", CptInfo("99394", "Preventive visit, established, age 12-17", CATEGORY_OV))
        put("99395", CptInfo("99395", "Preventive visit, established, age 18-39", CATEGORY_OV))
        put("99396", CptInfo("99396", "Preventive visit, established, age 40-64", CATEGORY_OV))
        put("99397", CptInfo("99397", "Preventive visit, established, age 65+", CATEGORY_OV))

        // Hospital - Inpatient
        put("99221", CptInfo("99221", "Initial hospital care, straightforward", CATEGORY_HOSPITAL))
        put("99222", CptInfo("99222", "Initial hospital care, moderate complexity", CATEGORY_HOSPITAL))
        put("99223", CptInfo("99223", "Initial hospital care, high complexity", CATEGORY_HOSPITAL))
        put("99231", CptInfo("99231", "Subsequent hospital care, straightforward", CATEGORY_HOSPITAL))
        put("99232", CptInfo("99232", "Subsequent hospital care, moderate complexity", CATEGORY_HOSPITAL))
        put("99233", CptInfo("99233", "Subsequent hospital care, high complexity", CATEGORY_HOSPITAL))
        put("99238", CptInfo("99238", "Hospital discharge day, 30 min or less", CATEGORY_HOSPITAL))
        put("99239", CptInfo("99239", "Hospital discharge day, more than 30 min", CATEGORY_HOSPITAL))

        // Emergency Department
        put("99281", CptInfo("99281", "Emergency department visit, minimal", CATEGORY_HOSPITAL))
        put("99282", CptInfo("99282", "Emergency department visit, low severity", CATEGORY_HOSPITAL))
        put("99283", CptInfo("99283", "Emergency department visit, moderate severity", CATEGORY_HOSPITAL))
        put("99284", CptInfo("99284", "Emergency department visit, high severity", CATEGORY_HOSPITAL))
        put("99285", CptInfo("99285", "Emergency department visit, high severity with threat", CATEGORY_HOSPITAL))

        // Observation
        put("99217", CptInfo("99217", "Observation care discharge", CATEGORY_HOSPITAL))
        put("99218", CptInfo("99218", "Initial observation care, straightforward", CATEGORY_HOSPITAL))
        put("99219", CptInfo("99219", "Initial observation care, moderate complexity", CATEGORY_HOSPITAL))
        put("99220", CptInfo("99220", "Initial observation care, high complexity", CATEGORY_HOSPITAL))

        // Consultation
        put("99241", CptInfo("99241", "Office consultation, straightforward", CATEGORY_OV))
        put("99242", CptInfo("99242", "Office consultation, low complexity", CATEGORY_OV))
        put("99243", CptInfo("99243", "Office consultation, moderate complexity", CATEGORY_OV))
        put("99244", CptInfo("99244", "Office consultation, moderate-high complexity", CATEGORY_OV))
        put("99245", CptInfo("99245", "Office consultation, high complexity", CATEGORY_OV))

        // Labs - Chemistry
        put("80047", CptInfo("80047", "Basic metabolic panel (BMP) with ionized calcium", CATEGORY_LAB))
        put("80048", CptInfo("80048", "Basic metabolic panel (BMP)", CATEGORY_LAB))
        put("80050", CptInfo("80050", "General health panel", CATEGORY_LAB))
        put("80051", CptInfo("80051", "Electrolyte panel", CATEGORY_LAB))
        put("80053", CptInfo("80053", "Comprehensive metabolic panel (CMP)", CATEGORY_LAB))
        put("80055", CptInfo("80055", "Obstetric panel", CATEGORY_LAB))
        put("80061", CptInfo("80061", "Lipid panel", CATEGORY_LAB))
        put("80069", CptInfo("80069", "Renal function panel", CATEGORY_LAB))
        put("80074", CptInfo("80074", "Acute hepatitis panel", CATEGORY_LAB))
        put("80076", CptInfo("80076", "Hepatic function panel", CATEGORY_LAB))

        // Labs - Hematology
        put("85004", CptInfo("85004", "Blood count, automated differential", CATEGORY_LAB))
        put("85007", CptInfo("85007", "Blood smear, microscopic exam", CATEGORY_LAB))
        put("85025", CptInfo("85025", "Complete blood count (CBC) with differential", CATEGORY_LAB))
        put("85027", CptInfo("85027", "Complete blood count (CBC) automated", CATEGORY_LAB))
        put("85610", CptInfo("85610", "Prothrombin time (PT)", CATEGORY_LAB))
        put("85730", CptInfo("85730", "Partial thromboplastin time (PTT)", CATEGORY_LAB))

        // Labs - Urinalysis
        put("81000", CptInfo("81000", "Urinalysis with microscopy, non-automated", CATEGORY_LAB))
        put("81001", CptInfo("81001", "Urinalysis with microscopy, automated", CATEGORY_LAB))
        put("81002", CptInfo("81002", "Urinalysis without microscopy, non-automated", CATEGORY_LAB))
        put("81003", CptInfo("81003", "Urinalysis without microscopy, automated", CATEGORY_LAB))

        // Labs - Individual Tests
        put("82270", CptInfo("82270", "Fecal occult blood test", CATEGORY_LAB))
        put("82306", CptInfo("82306", "Vitamin D, 25-hydroxy", CATEGORY_LAB))
        put("82465", CptInfo("82465", "Cholesterol, serum total", CATEGORY_LAB))
        put("82550", CptInfo("82550", "Creatine kinase (CK)", CATEGORY_LAB))
        put("82565", CptInfo("82565", "Creatinine, blood", CATEGORY_LAB))
        put("82607", CptInfo("82607", "Vitamin B-12", CATEGORY_LAB))
        put("82728", CptInfo("82728", "Ferritin", CATEGORY_LAB))
        put("82746", CptInfo("82746", "Folic acid, serum", CATEGORY_LAB))
        put("82947", CptInfo("82947", "Glucose, blood", CATEGORY_LAB))
        put("83036", CptInfo("83036", "Hemoglobin A1c (HbA1c)", CATEGORY_LAB))
        put("84100", CptInfo("84100", "Phosphorus", CATEGORY_LAB))
        put("84132", CptInfo("84132", "Potassium, serum", CATEGORY_LAB))
        put("84153", CptInfo("84153", "PSA (prostate specific antigen)", CATEGORY_LAB))
        put("84295", CptInfo("84295", "Sodium, serum", CATEGORY_LAB))
        put("84439", CptInfo("84439", "Thyroxine (T4), free", CATEGORY_LAB))
        put("84443", CptInfo("84443", "TSH (thyroid stimulating hormone)", CATEGORY_LAB))
        put("84550", CptInfo("84550", "Uric acid, blood", CATEGORY_LAB))
        put("86140", CptInfo("86140", "C-reactive protein (CRP)", CATEGORY_LAB))
        put("86200", CptInfo("86200", "Cyclic citrullinated peptide (CCP)", CATEGORY_LAB))
        put("86235", CptInfo("86235", "Nuclear antigen antibody (ENA)", CATEGORY_LAB))
        put("86431", CptInfo("86431", "Rheumatoid factor, quantitative", CATEGORY_LAB))
        put("86580", CptInfo("86580", "TB skin test (PPD)", CATEGORY_LAB))
        put("86592", CptInfo("86592", "Syphilis test (RPR)", CATEGORY_LAB))
        put("86701", CptInfo("86701", "HIV-1 antibody", CATEGORY_LAB))
        put("86803", CptInfo("86803", "Hepatitis C antibody", CATEGORY_LAB))
        put("87070", CptInfo("87070", "Culture, bacterial, any source", CATEGORY_LAB))
        put("87081", CptInfo("87081", "Culture, screening only", CATEGORY_LAB))
        put("87086", CptInfo("87086", "Urine culture, bacterial", CATEGORY_LAB))
        put("87491", CptInfo("87491", "Chlamydia, amplified probe", CATEGORY_LAB))
        put("87591", CptInfo("87591", "Gonorrhea, amplified probe", CATEGORY_LAB))
        put("87804", CptInfo("87804", "Influenza rapid test", CATEGORY_LAB))
        put("87880", CptInfo("87880", "Strep A rapid test", CATEGORY_LAB))

        // Injections & Immunizations
        put("90460", CptInfo("90460", "Immunization admin, first component", CATEGORY_INJECTION))
        put("90461", CptInfo("90461", "Immunization admin, each additional component", CATEGORY_INJECTION))
        put("90471", CptInfo("90471", "Immunization admin, injection", CATEGORY_INJECTION))
        put("90472", CptInfo("90472", "Immunization admin, each additional injection", CATEGORY_INJECTION))
        put("90473", CptInfo("90473", "Immunization admin, intranasal/oral", CATEGORY_INJECTION))
        put("90632", CptInfo("90632", "Hepatitis A vaccine, adult", CATEGORY_INJECTION))
        put("90647", CptInfo("90647", "Haemophilus influenzae b vaccine (Hib)", CATEGORY_INJECTION))
        put("90649", CptInfo("90649", "HPV vaccine, 4-valent", CATEGORY_INJECTION))
        put("90651", CptInfo("90651", "HPV vaccine, 9-valent", CATEGORY_INJECTION))
        put("90656", CptInfo("90656", "Influenza vaccine, preservative-free", CATEGORY_INJECTION))
        put("90658", CptInfo("90658", "Influenza vaccine", CATEGORY_INJECTION))
        put("90670", CptInfo("90670", "Pneumococcal conjugate vaccine (PCV13)", CATEGORY_INJECTION))
        put("90672", CptInfo("90672", "Influenza vaccine, intranasal", CATEGORY_INJECTION))
        put("90680", CptInfo("90680", "Rotavirus vaccine, pentavalent", CATEGORY_INJECTION))
        put("90696", CptInfo("90696", "DTaP-IPV vaccine", CATEGORY_INJECTION))
        put("90700", CptInfo("90700", "DTaP vaccine", CATEGORY_INJECTION))
        put("90707", CptInfo("90707", "MMR vaccine", CATEGORY_INJECTION))
        put("90710", CptInfo("90710", "MMRV vaccine", CATEGORY_INJECTION))
        put("90713", CptInfo("90713", "Polio vaccine (IPV)", CATEGORY_INJECTION))
        put("90714", CptInfo("90714", "Td vaccine", CATEGORY_INJECTION))
        put("90715", CptInfo("90715", "Tdap vaccine", CATEGORY_INJECTION))
        put("90716", CptInfo("90716", "Varicella vaccine", CATEGORY_INJECTION))
        put("90732", CptInfo("90732", "Pneumococcal vaccine (PPSV23)", CATEGORY_INJECTION))
        put("90736", CptInfo("90736", "Zoster vaccine (Shingrix)", CATEGORY_INJECTION))
        put("90746", CptInfo("90746", "Hepatitis B vaccine, adult", CATEGORY_INJECTION))
        put("90847", CptInfo("90847", "Family psychotherapy with patient", CATEGORY_OV))

        // Therapeutic Injections
        put("96365", CptInfo("96365", "IV infusion, initial, up to 1 hour", CATEGORY_INJECTION))
        put("96366", CptInfo("96366", "IV infusion, each additional hour", CATEGORY_INJECTION))
        put("96367", CptInfo("96367", "IV infusion, additional sequential", CATEGORY_INJECTION))
        put("96372", CptInfo("96372", "Therapeutic injection, subcutaneous/intramuscular", CATEGORY_INJECTION))
        put("96373", CptInfo("96373", "Therapeutic injection, intra-arterial", CATEGORY_INJECTION))
        put("96374", CptInfo("96374", "Therapeutic injection, IV push, initial", CATEGORY_INJECTION))
        put("96375", CptInfo("96375", "Therapeutic injection, IV push, each additional", CATEGORY_INJECTION))
        put("96376", CptInfo("96376", "Therapeutic injection, IV push, additional sequential", CATEGORY_INJECTION))

        // Joint Injections
        put("20600", CptInfo("20600", "Joint injection, small joint", CATEGORY_INJECTION))
        put("20605", CptInfo("20605", "Joint injection, intermediate joint", CATEGORY_INJECTION))
        put("20610", CptInfo("20610", "Joint injection, major joint", CATEGORY_INJECTION))

        // DME
        put("A4253", CptInfo("A4253", "Blood glucose test strips", CATEGORY_DME))
        put("A4256", CptInfo("A4256", "Normal low and high calibrator solution", CATEGORY_DME))
        put("A4259", CptInfo("A4259", "Lancets per box", CATEGORY_DME))
        put("A6216", CptInfo("A6216", "Gauze, non-impregnated, sterile, each", CATEGORY_DME))
        put("A6413", CptInfo("A6413", "Adhesive bandage, first aid type", CATEGORY_DME))
        put("E0100", CptInfo("E0100", "Cane, includes canes of all materials", CATEGORY_DME))
        put("E0105", CptInfo("E0105", "Cane, quad or three prong", CATEGORY_DME))
        put("E0110", CptInfo("E0110", "Crutches, forearm, pair", CATEGORY_DME))
        put("E0112", CptInfo("E0112", "Crutches, underarm, wood, pair", CATEGORY_DME))
        put("E0114", CptInfo("E0114", "Crutches, underarm, other than wood, pair", CATEGORY_DME))
        put("E0130", CptInfo("E0130", "Walker, rigid, adjustable or fixed height", CATEGORY_DME))
        put("E0135", CptInfo("E0135", "Walker, folding, adjustable or fixed height", CATEGORY_DME))
        put("E0143", CptInfo("E0143", "Walker, folding, wheeled, adjustable", CATEGORY_DME))
        put("E0148", CptInfo("E0148", "Walker, heavy duty, without wheels", CATEGORY_DME))
        put("E0163", CptInfo("E0163", "Commode chair, stationary, with fixed arms", CATEGORY_DME))
        put("E0260", CptInfo("E0260", "Hospital bed, semi-electric", CATEGORY_DME))
        put("E0277", CptInfo("E0277", "Powered pressure-reducing air mattress", CATEGORY_DME))
        put("E0424", CptInfo("E0424", "Stationary compressed gaseous oxygen system", CATEGORY_DME))
        put("E0431", CptInfo("E0431", "Portable gaseous oxygen system", CATEGORY_DME))
        put("E0470", CptInfo("E0470", "Respiratory assist device, bi-level (CPAP)", CATEGORY_DME))
        put("E0601", CptInfo("E0601", "Continuous positive airway pressure (CPAP)", CATEGORY_DME))
        put("E0607", CptInfo("E0607", "Home blood glucose monitor", CATEGORY_DME))
        put("E0650", CptInfo("E0650", "Pneumatic compressor, non-segmental", CATEGORY_DME))
        put("E0730", CptInfo("E0730", "Transcutaneous electrical nerve stimulator (TENS)", CATEGORY_DME))
        put("E1390", CptInfo("E1390", "Oxygen concentrator, single delivery port", CATEGORY_DME))

        // Radiology
        put("70553", CptInfo("70553", "MRI brain without and with contrast", CATEGORY_RADIOLOGY))
        put("71046", CptInfo("71046", "Chest X-ray, 2 views", CATEGORY_RADIOLOGY))
        put("71250", CptInfo("71250", "CT chest without contrast", CATEGORY_RADIOLOGY))
        put("72148", CptInfo("72148", "MRI lumbar spine without contrast", CATEGORY_RADIOLOGY))
        put("73030", CptInfo("73030", "X-ray shoulder, complete", CATEGORY_RADIOLOGY))
        put("73070", CptInfo("73070", "X-ray elbow, 2 views", CATEGORY_RADIOLOGY))
        put("73110", CptInfo("73110", "X-ray wrist, complete", CATEGORY_RADIOLOGY))
        put("73130", CptInfo("73130", "X-ray hand, 2 views", CATEGORY_RADIOLOGY))
        put("73502", CptInfo("73502", "X-ray hip, 2-3 views", CATEGORY_RADIOLOGY))
        put("73560", CptInfo("73560", "X-ray knee, 1-2 views", CATEGORY_RADIOLOGY))
        put("73600", CptInfo("73600", "X-ray ankle, 2 views", CATEGORY_RADIOLOGY))
        put("73630", CptInfo("73630", "X-ray foot, complete", CATEGORY_RADIOLOGY))
        put("74018", CptInfo("74018", "X-ray abdomen, 1 view", CATEGORY_RADIOLOGY))
        put("76700", CptInfo("76700", "Ultrasound, abdominal, complete", CATEGORY_RADIOLOGY))
        put("76770", CptInfo("76770", "Ultrasound, retroperitoneal, complete", CATEGORY_RADIOLOGY))
        put("76856", CptInfo("76856", "Ultrasound, pelvic, non-obstetric", CATEGORY_RADIOLOGY))
        put("77065", CptInfo("77065", "Screening mammography, unilateral", CATEGORY_RADIOLOGY))
        put("77067", CptInfo("77067", "Screening mammography, bilateral", CATEGORY_RADIOLOGY))

        // Surgery - Common Minor
        put("10060", CptInfo("10060", "Incision and drainage of abscess, simple", CATEGORY_SURGERY))
        put("10061", CptInfo("10061", "Incision and drainage of abscess, complicated", CATEGORY_SURGERY))
        put("10120", CptInfo("10120", "Incision and removal of foreign body, simple", CATEGORY_SURGERY))
        put("11102", CptInfo("11102", "Tangential biopsy of skin, single lesion", CATEGORY_SURGERY))
        put("11200", CptInfo("11200", "Removal of skin tags, up to 15", CATEGORY_SURGERY))
        put("12001", CptInfo("12001", "Simple repair of wound, 2.5 cm or less", CATEGORY_SURGERY))
        put("12002", CptInfo("12002", "Simple repair of wound, 2.6-7.5 cm", CATEGORY_SURGERY))
        put("17000", CptInfo("17000", "Destruction of premalignant lesion, first", CATEGORY_SURGERY))
        put("17110", CptInfo("17110", "Destruction of benign lesions, up to 14", CATEGORY_SURGERY))

        // Mental Health
        put("90791", CptInfo("90791", "Psychiatric diagnostic evaluation", CATEGORY_OV))
        put("90792", CptInfo("90792", "Psychiatric diagnostic evaluation with medical services", CATEGORY_OV))
        put("90832", CptInfo("90832", "Psychotherapy, 30 minutes", CATEGORY_OV))
        put("90834", CptInfo("90834", "Psychotherapy, 45 minutes", CATEGORY_OV))
        put("90837", CptInfo("90837", "Psychotherapy, 60 minutes", CATEGORY_OV))
        put("90839", CptInfo("90839", "Psychotherapy for crisis, initial 60 min", CATEGORY_OV))
        put("90846", CptInfo("90846", "Family psychotherapy without patient", CATEGORY_OV))

        // Physical Therapy
        put("97110", CptInfo("97110", "Therapeutic exercises, each 15 minutes", CATEGORY_OV))
        put("97112", CptInfo("97112", "Neuromuscular re-education, each 15 minutes", CATEGORY_OV))
        put("97116", CptInfo("97116", "Gait training, each 15 minutes", CATEGORY_OV))
        put("97140", CptInfo("97140", "Manual therapy, each 15 minutes", CATEGORY_OV))
        put("97161", CptInfo("97161", "PT evaluation, low complexity", CATEGORY_OV))
        put("97162", CptInfo("97162", "PT evaluation, moderate complexity", CATEGORY_OV))
        put("97163", CptInfo("97163", "PT evaluation, high complexity", CATEGORY_OV))

        // Misc
        put("36415", CptInfo("36415", "Venipuncture (blood draw)", CATEGORY_LAB))
        put("36416", CptInfo("36416", "Capillary blood collection (finger stick)", CATEGORY_LAB))
        put("99000", CptInfo("99000", "Handling/conveyance of specimen to lab", CATEGORY_LAB))
    }

    fun lookup(code: String): CptInfo? = all[code.uppercase().trim()]

    fun isValidCode(code: String): Boolean {
        val c = code.trim()
        if (c.length != 5) return false
        val first = c[0]
        return first in '1'..'9' || first.uppercaseChar() in 'A'..'J'
    }

    fun categorize(code: String): String {
        return all[code]?.category ?: guessCategory(code)
    }

    private fun guessCategory(code: String): String {
        val c = code.trim()
        if (c.isEmpty()) return CATEGORY_OTHER
        return when {
            c.startsWith("992") -> CATEGORY_HOSPITAL
            c.startsWith("991") || c.startsWith("993") || c.startsWith("994") || c.startsWith("995") -> CATEGORY_OV
            c.startsWith("99") -> CATEGORY_OV
            c.startsWith("8") -> CATEGORY_LAB
            c.startsWith("7") -> CATEGORY_RADIOLOGY
            c.startsWith("904") || c.startsWith("906") || c.startsWith("907") || c.startsWith("963") -> CATEGORY_INJECTION
            c.first().uppercaseChar() in 'A'..'E' -> CATEGORY_DME
            c.startsWith("1") || c.startsWith("2") || c.startsWith("3") || c.startsWith("4") || c.startsWith("5") || c.startsWith("6") -> CATEGORY_SURGERY
            else -> CATEGORY_OTHER
        }
    }
}
