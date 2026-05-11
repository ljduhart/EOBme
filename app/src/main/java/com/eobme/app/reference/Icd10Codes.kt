package com.eobme.app.reference

object Icd10Codes {

    data class Icd10Info(val code: String, val description: String)

    val common: Map<String, Icd10Info> = buildMap {
        // Infectious diseases
        put("A09", Icd10Info("A09", "Infectious gastroenteritis and colitis"))
        put("A49.9", Icd10Info("A49.9", "Bacterial infection, unspecified"))
        put("B00.9", Icd10Info("B00.9", "Herpes viral infection, unspecified"))
        put("B02.9", Icd10Info("B02.9", "Herpes zoster (shingles)"))
        put("B34.9", Icd10Info("B34.9", "Viral infection, unspecified"))
        put("B35.1", Icd10Info("B35.1", "Tinea unguium (nail fungus)"))
        put("B37.0", Icd10Info("B37.0", "Candidal stomatitis (oral thrush)"))

        // Neoplasms
        put("C50.919", Icd10Info("C50.919", "Malignant neoplasm of breast"))
        put("C61", Icd10Info("C61", "Malignant neoplasm of prostate"))
        put("D64.9", Icd10Info("D64.9", "Anemia, unspecified"))

        // Endocrine, metabolic
        put("E03.9", Icd10Info("E03.9", "Hypothyroidism, unspecified"))
        put("E05.90", Icd10Info("E05.90", "Hyperthyroidism, unspecified"))
        put("E11.9", Icd10Info("E11.9", "Type 2 diabetes mellitus without complications"))
        put("E11.65", Icd10Info("E11.65", "Type 2 diabetes with hyperglycemia"))
        put("E10.9", Icd10Info("E10.9", "Type 1 diabetes mellitus without complications"))
        put("E55.9", Icd10Info("E55.9", "Vitamin D deficiency"))
        put("E66.01", Icd10Info("E66.01", "Morbid obesity due to excess calories"))
        put("E66.9", Icd10Info("E66.9", "Obesity, unspecified"))
        put("E78.00", Icd10Info("E78.00", "Pure hypercholesterolemia, unspecified"))
        put("E78.1", Icd10Info("E78.1", "Pure hypertriglyceridemia"))
        put("E78.5", Icd10Info("E78.5", "Dyslipidemia, unspecified"))
        put("E87.6", Icd10Info("E87.6", "Hypokalemia"))

        // Mental health
        put("F10.20", Icd10Info("F10.20", "Alcohol dependence, uncomplicated"))
        put("F17.210", Icd10Info("F17.210", "Nicotine dependence, cigarettes"))
        put("F20.9", Icd10Info("F20.9", "Schizophrenia, unspecified"))
        put("F31.9", Icd10Info("F31.9", "Bipolar disorder, unspecified"))
        put("F32.1", Icd10Info("F32.1", "Major depressive disorder, moderate"))
        put("F32.9", Icd10Info("F32.9", "Major depressive disorder, unspecified"))
        put("F33.0", Icd10Info("F33.0", "Recurrent depressive disorder, mild"))
        put("F33.1", Icd10Info("F33.1", "Recurrent depressive disorder, moderate"))
        put("F41.0", Icd10Info("F41.0", "Panic disorder"))
        put("F41.1", Icd10Info("F41.1", "Generalized anxiety disorder"))
        put("F41.9", Icd10Info("F41.9", "Anxiety disorder, unspecified"))
        put("F43.10", Icd10Info("F43.10", "Post-traumatic stress disorder (PTSD)"))
        put("F43.20", Icd10Info("F43.20", "Adjustment disorder, unspecified"))
        put("F90.9", Icd10Info("F90.9", "ADHD, unspecified"))

        // Nervous system
        put("G20", Icd10Info("G20", "Parkinson disease"))
        put("G25.0", Icd10Info("G25.0", "Essential tremor"))
        put("G30.9", Icd10Info("G30.9", "Alzheimer disease, unspecified"))
        put("G40.909", Icd10Info("G40.909", "Epilepsy, unspecified"))
        put("G43.909", Icd10Info("G43.909", "Migraine, unspecified"))
        put("G47.00", Icd10Info("G47.00", "Insomnia, unspecified"))
        put("G47.33", Icd10Info("G47.33", "Obstructive sleep apnea"))
        put("G54.1", Icd10Info("G54.1", "Lumbosacral plexus disorders"))
        put("G56.00", Icd10Info("G56.00", "Carpal tunnel syndrome"))
        put("G62.9", Icd10Info("G62.9", "Polyneuropathy, unspecified"))
        put("G89.29", Icd10Info("G89.29", "Chronic pain, not elsewhere classified"))

        // Eye
        put("H10.9", Icd10Info("H10.9", "Conjunctivitis, unspecified"))
        put("H26.9", Icd10Info("H26.9", "Cataract, unspecified"))
        put("H40.9", Icd10Info("H40.9", "Glaucoma, unspecified"))
        put("H52.4", Icd10Info("H52.4", "Presbyopia"))

        // Ear
        put("H61.20", Icd10Info("H61.20", "Impacted cerumen (earwax)"))
        put("H66.90", Icd10Info("H66.90", "Otitis media, unspecified"))
        put("H91.90", Icd10Info("H91.90", "Hearing loss, unspecified"))

        // Circulatory
        put("I10", Icd10Info("I10", "Essential hypertension"))
        put("I20.9", Icd10Info("I20.9", "Angina pectoris, unspecified"))
        put("I21.9", Icd10Info("I21.9", "Acute myocardial infarction, unspecified"))
        put("I25.10", Icd10Info("I25.10", "Atherosclerotic heart disease"))
        put("I48.91", Icd10Info("I48.91", "Atrial fibrillation, unspecified"))
        put("I50.9", Icd10Info("I50.9", "Heart failure, unspecified"))
        put("I63.9", Icd10Info("I63.9", "Cerebral infarction, unspecified"))
        put("I73.9", Icd10Info("I73.9", "Peripheral vascular disease"))
        put("I83.90", Icd10Info("I83.90", "Varicose veins of lower extremity"))
        put("I87.2", Icd10Info("I87.2", "Venous insufficiency, chronic"))

        // Respiratory
        put("J00", Icd10Info("J00", "Acute nasopharyngitis (common cold)"))
        put("J02.9", Icd10Info("J02.9", "Acute pharyngitis, unspecified"))
        put("J06.9", Icd10Info("J06.9", "Acute upper respiratory infection"))
        put("J18.9", Icd10Info("J18.9", "Pneumonia, unspecified"))
        put("J20.9", Icd10Info("J20.9", "Acute bronchitis, unspecified"))
        put("J30.1", Icd10Info("J30.1", "Allergic rhinitis, pollen"))
        put("J30.9", Icd10Info("J30.9", "Allergic rhinitis, unspecified"))
        put("J32.9", Icd10Info("J32.9", "Chronic sinusitis, unspecified"))
        put("J40", Icd10Info("J40", "Bronchitis, not specified as acute or chronic"))
        put("J44.1", Icd10Info("J44.1", "COPD with acute exacerbation"))
        put("J45.20", Icd10Info("J45.20", "Mild intermittent asthma"))
        put("J45.909", Icd10Info("J45.909", "Asthma, unspecified"))

        // Digestive
        put("K02.9", Icd10Info("K02.9", "Dental caries, unspecified"))
        put("K08.109", Icd10Info("K08.109", "Loss of teeth due to caries"))
        put("K21.0", Icd10Info("K21.0", "GERD with esophagitis"))
        put("K29.70", Icd10Info("K29.70", "Gastritis, unspecified"))
        put("K30", Icd10Info("K30", "Functional dyspepsia"))
        put("K35.80", Icd10Info("K35.80", "Acute appendicitis"))
        put("K40.90", Icd10Info("K40.90", "Inguinal hernia"))
        put("K44.9", Icd10Info("K44.9", "Diaphragmatic hernia"))
        put("K50.90", Icd10Info("K50.90", "Crohn disease, unspecified"))
        put("K51.90", Icd10Info("K51.90", "Ulcerative colitis, unspecified"))
        put("K57.90", Icd10Info("K57.90", "Diverticulosis/diverticulitis"))
        put("K58.9", Icd10Info("K58.9", "Irritable bowel syndrome"))
        put("K59.00", Icd10Info("K59.00", "Constipation, unspecified"))
        put("K76.0", Icd10Info("K76.0", "Fatty liver, not elsewhere classified"))
        put("K80.20", Icd10Info("K80.20", "Gallstones without obstruction"))

        // Skin
        put("L03.90", Icd10Info("L03.90", "Cellulitis, unspecified"))
        put("L20.9", Icd10Info("L20.9", "Atopic dermatitis (eczema)"))
        put("L30.9", Icd10Info("L30.9", "Dermatitis, unspecified"))
        put("L40.0", Icd10Info("L40.0", "Psoriasis vulgaris"))
        put("L50.9", Icd10Info("L50.9", "Urticaria (hives), unspecified"))
        put("L70.0", Icd10Info("L70.0", "Acne vulgaris"))
        put("L72.0", Icd10Info("L72.0", "Epidermal cyst"))

        // Musculoskeletal
        put("M06.9", Icd10Info("M06.9", "Rheumatoid arthritis, unspecified"))
        put("M10.9", Icd10Info("M10.9", "Gout, unspecified"))
        put("M17.9", Icd10Info("M17.9", "Osteoarthritis of knee, unspecified"))
        put("M19.90", Icd10Info("M19.90", "Osteoarthritis, unspecified"))
        put("M25.50", Icd10Info("M25.50", "Joint pain, unspecified"))
        put("M25.561", Icd10Info("M25.561", "Pain in right knee"))
        put("M25.562", Icd10Info("M25.562", "Pain in left knee"))
        put("M47.816", Icd10Info("M47.816", "Spondylosis, lumbar region"))
        put("M50.30", Icd10Info("M50.30", "Cervical disc degeneration"))
        put("M51.16", Icd10Info("M51.16", "Intervertebral disc disorder with radiculopathy, lumbar"))
        put("M54.2", Icd10Info("M54.2", "Cervicalgia (neck pain)"))
        put("M54.5", Icd10Info("M54.5", "Low back pain"))
        put("M54.9", Icd10Info("M54.9", "Dorsalgia (back pain), unspecified"))
        put("M62.838", Icd10Info("M62.838", "Muscle spasm, other"))
        put("M75.10", Icd10Info("M75.10", "Rotator cuff tear"))
        put("M79.1", Icd10Info("M79.1", "Myalgia (muscle pain)"))
        put("M79.3", Icd10Info("M79.3", "Panniculitis, unspecified"))
        put("M79.7", Icd10Info("M79.7", "Fibromyalgia"))
        put("M81.0", Icd10Info("M81.0", "Age-related osteoporosis"))

        // Genitourinary
        put("N18.9", Icd10Info("N18.9", "Chronic kidney disease, unspecified"))
        put("N20.0", Icd10Info("N20.0", "Calculus of kidney (kidney stone)"))
        put("N30.00", Icd10Info("N30.00", "Acute cystitis without hematuria"))
        put("N39.0", Icd10Info("N39.0", "Urinary tract infection"))
        put("N40.0", Icd10Info("N40.0", "Benign prostatic hyperplasia (BPH)"))
        put("N76.0", Icd10Info("N76.0", "Acute vaginitis"))
        put("N80.0", Icd10Info("N80.0", "Endometriosis of uterus"))
        put("N92.0", Icd10Info("N92.0", "Excessive menstruation"))
        put("N95.1", Icd10Info("N95.1", "Menopausal and female climacteric states"))

        // Pregnancy / childbirth
        put("O80", Icd10Info("O80", "Encounter for full-term uncomplicated delivery"))
        put("Z33.1", Icd10Info("Z33.1", "Pregnant state, incidental"))
        put("Z34.00", Icd10Info("Z34.00", "Encounter for supervision of normal first pregnancy"))

        // Symptoms & Signs
        put("R00.0", Icd10Info("R00.0", "Tachycardia, unspecified"))
        put("R05.9", Icd10Info("R05.9", "Cough, unspecified"))
        put("R06.02", Icd10Info("R06.02", "Shortness of breath"))
        put("R07.9", Icd10Info("R07.9", "Chest pain, unspecified"))
        put("R10.9", Icd10Info("R10.9", "Abdominal pain, unspecified"))
        put("R11.0", Icd10Info("R11.0", "Nausea"))
        put("R11.2", Icd10Info("R11.2", "Nausea with vomiting"))
        put("R19.7", Icd10Info("R19.7", "Diarrhea, unspecified"))
        put("R21", Icd10Info("R21", "Rash and other nonspecific skin eruption"))
        put("R42", Icd10Info("R42", "Dizziness and giddiness"))
        put("R50.9", Icd10Info("R50.9", "Fever, unspecified"))
        put("R51.9", Icd10Info("R51.9", "Headache"))
        put("R53.83", Icd10Info("R53.83", "Fatigue"))
        put("R63.4", Icd10Info("R63.4", "Abnormal weight loss"))

        // Injury
        put("S06.0X0A", Icd10Info("S06.0X0A", "Concussion without loss of consciousness"))
        put("S13.4XXA", Icd10Info("S13.4XXA", "Sprain of cervical spine (whiplash)"))
        put("S39.012A", Icd10Info("S39.012A", "Strain of lumbar spine"))
        put("S52.509A", Icd10Info("S52.509A", "Fracture of forearm, unspecified"))
        put("S62.90XA", Icd10Info("S62.90XA", "Fracture of hand, unspecified"))
        put("S82.90XA", Icd10Info("S82.90XA", "Fracture of lower leg, unspecified"))
        put("S93.401A", Icd10Info("S93.401A", "Sprain of right ankle"))

        // Screening / Preventive
        put("Z00.00", Icd10Info("Z00.00", "General adult medical examination"))
        put("Z00.129", Icd10Info("Z00.129", "Encounter for routine child health examination"))
        put("Z01.00", Icd10Info("Z01.00", "Encounter for examination of eyes"))
        put("Z12.11", Icd10Info("Z12.11", "Screening for malignant neoplasm of colon"))
        put("Z12.31", Icd10Info("Z12.31", "Screening mammogram for malignant neoplasm of breast"))
        put("Z12.4", Icd10Info("Z12.4", "Screening for malignant neoplasm of cervix"))
        put("Z23", Icd10Info("Z23", "Encounter for immunization"))
        put("Z87.891", Icd10Info("Z87.891", "Personal history of nicotine dependence"))
    }

    fun lookup(code: String): Icd10Info? {
        val upper = code.uppercase().trim()
        return common[upper] ?: common.entries.firstOrNull {
            upper.startsWith(it.key)
        }?.value
    }
}
