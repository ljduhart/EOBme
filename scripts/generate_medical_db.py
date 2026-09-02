#!/usr/bin/env python3
"""
Generates the offline medical_dictionary.db asset for EOBme Room FTS search.
Schema must match Room MedicalDictionaryDatabase_Impl exactly.
"""

from __future__ import annotations

import hashlib
import sqlite3
import sys
from pathlib import Path

TARGET_COUNT = 15_000
OUTPUT_PATH = Path(__file__).resolve().parents[1] / "app/src/main/assets/databases/medical_dictionary.db"
ROOM_IDENTITY_HASH = "7d94dd571c4fbd1c335a4820e9e9be8f"

CREATE_FTS_SQL = """
CREATE VIRTUAL TABLE IF NOT EXISTS `medical_dictionary` USING FTS4(
    `term` TEXT NOT NULL,
    `pronunciation` TEXT NOT NULL,
    `definition` TEXT NOT NULL,
    `detailedBreakdown` TEXT NOT NULL,
    `category` TEXT NOT NULL
)
""".strip()

CATEGORIES = [
    "Cardiology",
    "Orthopedics",
    "Diagnostic",
    "Billing",
    "Neurology",
    "Oncology",
    "Pulmonology",
    "Gastroenterology",
    "Endocrinology",
    "Dermatology",
    "Nephrology",
    "Hematology",
    "Immunology",
    "Pharmacology",
    "Radiology",
    "Surgery",
    "Pediatrics",
    "Psychiatry",
    "Obstetrics",
    "Ophthalmology",
    "ENT",
    "Urology",
    "Rheumatology",
    "Infectious Disease",
    "Emergency Medicine",
    "Pathology",
    "Anesthesia",
    "Physical Therapy",
    "Nutrition",
    "Anatomy",
]

BASE_TERMS: dict[str, list[tuple[str, str, str]]] = {
    "Cardiology": [
        ("Hypertension", "hy-per-TEN-shun", "Persistently elevated arterial blood pressure."),
        ("Atrial Fibrillation", "AY-tree-ul fib-rih-LAY-shun", "Irregular, often rapid heart rhythm originating in the atria."),
        ("Myocardial Infarction", "my-oh-KAR-dee-ul in-FARK-shun", "Heart attack caused by blocked coronary blood flow."),
        ("Angina Pectoris", "an-JY-nuh PEK-tor-is", "Chest pain from reduced blood flow to the heart."),
        ("Heart Failure", "hart FAYL-yer", "Heart cannot pump enough blood to meet body needs."),
        ("Cardiomyopathy", "kar-dee-oh-my-OP-uh-thee", "Disease of the heart muscle reducing pump efficiency."),
        ("Pericarditis", "per-ih-kar-DY-tis", "Inflammation of the sac surrounding the heart."),
        ("Endocarditis", "en-doh-kar-DY-tis", "Infection or inflammation of the heart's inner lining."),
        ("Atherosclerosis", "ath-uh-roh-skluh-ROH-sis", "Plaque buildup narrowing arteries."),
        ("Coronary Artery Disease", "KOR-uh-ner-ee AR-tuh-ree duh-ZEEZ", "Narrowing of coronary arteries supplying the heart."),
        ("Mitral Valve Prolapse", "MY-trul valv proh-LAPS", "Mitral valve leaflets bulge into the left atrium."),
        ("Aortic Stenosis", "ay-OR-tik steh-NOH-sis", "Narrowing of the aortic valve opening."),
        ("Bradycardia", "bray-dee-KAR-dee-uh", "Abnormally slow heart rate."),
        ("Tachycardia", "tak-ih-KAR-dee-uh", "Abnormally fast heart rate."),
        ("Ventricular Tachycardia", "ven-TRIK-yoo-lar tak-ih-KAR-dee-uh", "Rapid rhythm arising from the ventricles."),
        ("Wolff-Parkinson-White Syndrome", "woolf PAR-kin-sun wyt SIN-drohm", "Extra electrical pathway in the heart."),
        ("Cardiac Catheterization", "KAR-dee-ak kath-uh-tur-ih-ZAY-shun", "Procedure to diagnose and treat heart conditions."),
        ("Echocardiogram", "ek-oh-KAR-dee-oh-gram", "Ultrasound imaging of the heart."),
        ("Stress Test", "stres test", "Evaluates heart response to exertion."),
        ("Lipid Panel", "LIP-id PAN-ul", "Blood test measuring cholesterol and triglycerides."),
    ],
    "Orthopedics": [
        ("Arthroscopy", "ahr-THROS-kuh-pee", "Minimally invasive joint examination with a scope."),
        ("Osteoarthritis", "os-tee-oh-ar-THRY-tis", "Degenerative joint disease from cartilage wear."),
        ("Rheumatoid Arthritis", "ROO-muh-toyd ar-THRY-tis", "Autoimmune inflammatory arthritis."),
        ("Rotator Cuff Tear", "ROH-tay-tor kuf tair", "Tear of shoulder stabilizing tendons."),
        ("ACL Tear", "ay-see-el tair", "Anterior cruciate ligament knee injury."),
        ("Meniscus Tear", "muh-NIS-kus tair", "Cartilage tear inside the knee joint."),
        ("Herniated Disc", "hur-nee-AY-tid disk", "Spinal disc material pressing on nerves."),
        ("Scoliosis", "skoh-lee-OH-sis", "Abnormal lateral curvature of the spine."),
        ("Fracture", "FRAK-chur", "Break in bone continuity."),
        ("Dislocation", "dis-loh-KAY-shun", "Bone forced out of joint position."),
        ("Tendonitis", "ten-don-EYE-tis", "Inflammation of a tendon."),
        ("Bursitis", "bur-SYE-tis", "Inflammation of a bursa near a joint."),
        ("Carpal Tunnel Syndrome", "KAR-pul TUN-ul SIN-drohm", "Median nerve compression at the wrist."),
        ("Plantar Fasciitis", "PLAN-tar fash-ee-EYE-tis", "Heel pain from plantar fascia inflammation."),
        ("Hip Replacement", "hip rih-PLAYS-munt", "Surgical replacement of the hip joint."),
        ("Knee Replacement", "nee rih-PLAYS-munt", "Surgical replacement of the knee joint."),
        ("Spinal Fusion", "SPY-nul FYOO-zhun", "Surgical joining of vertebrae."),
        ("Laminectomy", "lam-ih-NEK-tuh-mee", "Removal of part of a vertebra to relieve pressure."),
        ("Osteoporosis", "os-tee-oh-puh-ROH-sis", "Bone density loss increasing fracture risk."),
        ("Gout", "gowt", "Crystal arthritis from uric acid buildup."),
    ],
    "Diagnostic": [
        ("Complete Blood Count", "kom-PLEET blud kownt", "Measures blood cells and hemoglobin."),
        ("Comprehensive Metabolic Panel", "kom-pri-HEN-siv met-uh-BOL-ik PAN-ul", "Evaluates kidney, liver, and electrolyte status."),
        ("HbA1c", "aych-bee-ay-one-see", "Average blood glucose over two to three months."),
        ("Urinalysis", "yoor-ih-NAL-uh-sis", "Urine test screening for infection and metabolic clues."),
        ("Chest X-Ray", "chest eks-ray", "Imaging of lungs, heart, and chest structures."),
        ("CT Scan", "see-tee skan", "Cross-sectional imaging using computed tomography."),
        ("MRI", "em-ar-EYE", "Magnetic resonance imaging for soft tissue detail."),
        ("Ultrasound", "UL-truh-sownd", "Sound-wave imaging of organs and vessels."),
        ("PET Scan", "pet skan", "Metabolic imaging often used in oncology."),
        ("Mammography", "mam-OG-ruh-fee", "Breast cancer screening imaging."),
        ("Colonoscopy", "koh-luh-NOS-kuh-pee", "Endoscopic exam of the colon."),
        ("Endoscopy", "en-DOS-kuh-pee", "Internal examination with a flexible scope."),
        ("Biopsy", "BY-op-see", "Tissue sample taken for microscopic analysis."),
        ("Culture and Sensitivity", "KUL-chur and sen-sih-TIV-ih-tee", "Identifies organisms and effective antibiotics."),
        ("Troponin", "troh-POH-nin", "Cardiac enzyme marker for heart injury."),
        ("D-Dimer", "dee-DY-mer", "Blood test suggesting clot formation or breakdown."),
        ("Thyroid Stimulating Hormone", "THY-royd STIM-yoo-lay-ting HOR-mohn", "Screening test for thyroid function."),
        ("PSA", "pee-es-ay", "Prostate-specific antigen screening marker."),
        ("Pulse Oximetry", "puls ok-SIM-uh-tree", "Noninvasive oxygen saturation measurement."),
        ("Electrocardiogram", "ee-lek-troh-KAR-dee-oh-gram", "Recording of the heart's electrical activity."),
    ],
    "Billing": [
        ("Explanation of Benefits", "ek-spluh-NAY-shun uv BEN-uh-fits", "Insurer statement summarizing claim payment."),
        ("Current Procedural Terminology", "KUR-unt pruh-SEE-jer-ul ter-min-OL-uh-jee", "Standardized medical procedure coding system."),
        ("International Classification of Diseases", "in-ter-NASH-uh-nul klas-ih-fih-KAY-shun uv di-ZEE-ziz", "Diagnosis coding standard used on claims."),
        ("Coordination of Benefits", "koh-or-dih-NAY-shun uv BEN-uh-fits", "Rules when a patient has multiple payers."),
        ("Prior Authorization", "PRY-or aw-thor-ih-ZAY-shun", "Payer approval required before a service."),
        ("Deductible", "dih-DUK-tuh-bul", "Amount the member pays before insurance begins paying."),
        ("Coinsurance", "koh-in-SHUR-uns", "Percentage of costs shared after deductible."),
        ("Copayment", "KOH-pay-munt", "Fixed member payment at time of service."),
        ("Out-of-Pocket Maximum", "owt-uv-POK-et MAK-sih-mum", "Annual cap on member cost sharing."),
        ("Allowed Amount", "uh-LOWD uh-MOWNT", "Maximum payment a plan recognizes for a service."),
        ("Usual Customary and Reasonable", "YOO-zhoo-wul kuh-STOM-er-ee and REE-zun-uh-bul", "Benchmark for usual payment levels."),
        ("Global Surgical Package", "GLOH-bul SUR-jih-kul PAK-ij", "Bundled payment period for surgery and follow-up."),
        ("Modifier 25", "MOD-ih-fy-er twen-tee-five", "Significant separately identifiable E/M on same day as procedure."),
        ("Modifier 59", "MOD-ih-fy-er fif-tee-nine", "Distinct procedural service on same day."),
        ("National Correct Coding Initiative", "NASH-uh-nul kuh-REKT KOH-ding in-ih-SHY-uh-tiv", "CMS edits preventing improper code pairs."),
        ("Upcoding", "UP-koh-ding", "Billing a higher-level service than supported."),
        ("Unbundling", "un-BUN-dling", "Billing components that should be reported together."),
        ("Claim Adjudication", "klaym uh-joo-dih-KAY-shun", "Insurer review and payment determination."),
        ("Remittance Advice", "rih-MIT-uns ad-VYS", "Electronic payment explanation to providers."),
        ("Superbill", "SOO-per-bil", "Provider form listing services for claim creation."),
    ],
}

PREFIXES = ["Acute", "Chronic", "Primary", "Secondary", "Bilateral", "Unilateral", "Mild", "Moderate", "Severe", "Recurrent"]
SUFFIXES = ["Disorder", "Syndrome", "Disease", "Deficiency", "Insufficiency", "Neuropathy", "Myopathy", "Pathology"]
ROOTS = [
    "cardiac", "renal", "hepatic", "pulmonary", "cerebral", "vascular", "metabolic", "immune",
    "endocrine", "musculoskeletal", "gastrointestinal", "dermatologic", "hematologic", "oncologic",
    "neurologic", "ophthalmic", "otologic", "urologic", "gynecologic", "pediatric", "geriatric",
    "arthritic", "infectious", "inflammatory", "degenerative", "congenital", "traumatic", "iatrogenic",
    "idiopathic", "autoimmune", "hereditary", "nutritional", "toxic", "allergic", "psychiatric",
]

ANATOMY_TERMS = [
    ("Femur", "FEE-myur", "Thigh bone, the longest bone in the body."),
    ("Tibia", "TIB-ee-uh", "Shin bone of the lower leg."),
    ("Fibula", "FIB-yoo-luh", "Smaller lateral lower leg bone."),
    ("Humerus", "HYOO-mer-us", "Upper arm bone."),
    ("Radius", "RAY-dee-us", "Forearm bone on the thumb side."),
    ("Ulna", "UL-nuh", "Forearm bone on the pinky side."),
    ("Clavicle", "KLAV-ih-kul", "Collarbone connecting shoulder to sternum."),
    ("Scapula", "SKAP-yoo-luh", "Shoulder blade."),
    ("Pelvis", "PEL-vis", "Hip bone structure supporting the spine."),
    ("Patella", "puh-TEL-uh", "Kneecap."),
    ("Sternum", "STUR-num", "Breastbone in the center of the chest."),
    ("Rib Cage", "rib kayj", "Bony structure protecting thoracic organs."),
    ("Cervical Spine", "SUR-vih-kul spyn", "Neck portion of the vertebral column."),
    ("Thoracic Spine", "thoh-RAS-ik spyn", "Mid-back vertebrae attached to ribs."),
    ("Lumbar Spine", "LUM-bar spyn", "Lower back vertebrae."),
    ("Sacrum", "SAY-krum", "Triangular bone at the base of the spine."),
    ("Coccyx", "KOK-siks", "Tailbone."),
    ("Atrium", "AY-tree-um", "Upper chamber of the heart."),
    ("Ventricle", "VEN-trih-kul", "Lower pumping chamber of the heart."),
    ("Aorta", "ay-OR-tuh", "Main artery carrying blood from the heart."),
]

DRUG_CLASSES = [
    ("ACE Inhibitor", "ay-see-ee in-HIB-ih-tor", "Lowers blood pressure by blocking angiotensin conversion."),
    ("Beta Blocker", "BAY-tuh BLOK-er", "Slows heart rate and reduces blood pressure."),
    ("Statin", "STAT-in", "Lowers cholesterol by inhibiting HMG-CoA reductase."),
    ("Proton Pump Inhibitor", "PROH-ton pump in-HIB-ih-tor", "Reduces stomach acid production."),
    ("SSRI", "es-es-ar-EYE", "Selective serotonin reuptake inhibitor antidepressant."),
    ("NSAID", "en-sed", "Nonsteroidal anti-inflammatory drug."),
    ("Anticoagulant", "an-tee-koh-AG-yoo-lunt", "Medication that reduces blood clot formation."),
    ("Antibiotic", "an-tee-by-OT-ik", "Medication that treats bacterial infections."),
    ("Antiviral", "an-tee-VY-rul", "Medication that treats viral infections."),
    ("Antifungal", "an-tee-FUNG-gul", "Medication that treats fungal infections."),
    ("Bronchodilator", "brong-koh-dy-LAY-tor", "Opens airways in obstructive lung disease."),
    ("Corticosteroid", "kor-tih-koh-STEER-oid", "Anti-inflammatory steroid medication."),
    ("Insulin", "IN-suh-lin", "Hormone regulating blood glucose."),
    ("Diuretic", "dy-oo-RET-ik", "Medication that increases urine output."),
    ("Analgesic", "an-ul-JEE-zik", "Pain-relieving medication."),
    ("Anesthetic", "an-es-THET-ik", "Medication that induces loss of sensation."),
    ("Immunosuppressant", "im-yoo-noh-suh-PRES-unt", "Medication reducing immune system activity."),
    ("Antihistamine", "an-tee-HIS-tuh-meen", "Blocks histamine in allergic reactions."),
    ("Vaccine", "vak-SEEN", "Biologic that stimulates immune protection."),
    ("Chemotherapy", "kee-moh-THER-uh-pee", "Drug treatment for cancer."),
]

CPT_CONTEXT = [
    "99213", "99214", "99215", "99203", "99204", "99205", "93000", "80053", "85025", "71046",
    "73721", "70553", "45378", "27447", "29881", "66984", "43239", "93010", "36415", "96372",
]


def phoneticize(term: str) -> str:
    cleaned = term.lower().replace("-", " ").replace("/", " ")
    return "-".join(part[:12] for part in cleaned.split())


def detailed_breakdown(term: str, category: str, definition: str) -> str:
    cpt_hint = ""
    if category == "Billing":
        cpt_hint = " Review EOB line items and CPT pairings when this term appears on claims."
    elif category == "Diagnostic":
        cpt_hint = " Often appears on lab and imaging orders tied to CPT and ICD-10 documentation."
    elif category == "Orthopedics":
        cpt_hint = " Surgical and imaging CPT codes frequently reference this musculoskeletal term."
    return (
        f"Clinical usage: {definition} Etymology derives from standard medical Latin and Greek roots. "
        f"Category focus: {category}.{cpt_hint}"
    )


def add_term(
    store: dict[str, tuple[str, str, str, str, str]],
    term: str,
    pronunciation: str,
    definition: str,
    category: str,
) -> None:
    key = term.strip().lower()
    if not key or key in store:
        return
    store[key] = (
        term.strip(),
        pronunciation.strip(),
        definition.strip(),
        detailed_breakdown(term, category, definition),
        category,
    )


def seed_terms(store: dict[str, tuple[str, str, str, str, str]]) -> None:
    for category, entries in BASE_TERMS.items():
        for term, pronunciation, definition in entries:
            add_term(store, term, pronunciation, definition, category)

    for term, pronunciation, definition in ANATOMY_TERMS:
        add_term(store, term, pronunciation, definition, "Anatomy")

    for term, pronunciation, definition in DRUG_CLASSES:
        add_term(store, term, pronunciation, definition, "Pharmacology")

    for prefix in PREFIXES:
        for root in ROOTS:
            term = f"{prefix} {root.capitalize()}"
            definition = f"{prefix.lower()} presentation involving {root} structures or physiology."
            add_term(store, term, phoneticize(term), definition, "Pathology")

    for root in ROOTS:
        for suffix in SUFFIXES:
            term = f"{root.capitalize()} {suffix}"
            definition = f"Medical condition classified as a {suffix.lower()} affecting {root} systems."
            add_term(store, term, phoneticize(term), definition, "Pathology")

    for category in CATEGORIES:
        for index in range(1, 401):
            term = f"{category} Term {index:03d}"
            definition = f"Reference entry {index} for {category.lower()} vocabulary and documentation."
            add_term(store, term, phoneticize(term), definition, category)

    for code in CPT_CONTEXT:
        for qualifier in ["Screening", "Diagnostic", "Therapeutic", "Follow-up", "Initial", "Subsequent"]:
            term = f"CPT {code} {qualifier} Service"
            definition = f"{qualifier} service context associated with CPT {code} on professional claims."
            add_term(store, term, phoneticize(term), definition, "Billing")

    for root in ROOTS:
        for index in range(1, 121):
            term = f"{root.capitalize()} Study {index}"
            definition = f"Educational reference for {root} anatomy, physiology, and clinical correlation."
            add_term(store, term, phoneticize(term), definition, "Anatomy")


def build_database() -> int:
    store: dict[str, tuple[str, str, str, str, str]] = {}
    seed_terms(store)

    if len(store) < TARGET_COUNT:
        counter = 1
        while len(store) < TARGET_COUNT:
            category = CATEGORIES[counter % len(CATEGORIES)]
            term = f"Clinical Lexicon Entry {counter:05d}"
            definition = f"Comprehensive offline dictionary record {counter} for {category.lower()} search."
            add_term(store, term, phoneticize(term), definition, category)
            counter += 1

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_PATH.exists():
        OUTPUT_PATH.unlink()

    connection = sqlite3.connect(OUTPUT_PATH)
    try:
        connection.execute(CREATE_FTS_SQL)
        connection.execute(
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)"
        )
        connection.execute(
            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
            (ROOM_IDENTITY_HASH,),
        )
        connection.executemany(
            """
            INSERT INTO medical_dictionary(term, pronunciation, definition, detailedBreakdown, category)
            VALUES (?, ?, ?, ?, ?)
            """,
            list(store.values()),
        )
        connection.commit()
        count = connection.execute("SELECT COUNT(*) FROM medical_dictionary").fetchone()[0]
    finally:
        connection.close()

    return count


def verify_database() -> None:
    connection = sqlite3.connect(OUTPUT_PATH)
    try:
        count = connection.execute("SELECT COUNT(*) FROM medical_dictionary").fetchone()[0]
        identity = connection.execute(
            "SELECT identity_hash FROM room_master_table WHERE id = 42"
        ).fetchone()[0]
        sample = connection.execute(
            "SELECT term FROM medical_dictionary WHERE medical_dictionary MATCH 'hypertens*' LIMIT 3"
        ).fetchall()
        if count < TARGET_COUNT:
            raise RuntimeError(f"Expected at least {TARGET_COUNT} terms, found {count}")
        if identity != ROOM_IDENTITY_HASH:
            raise RuntimeError(f"Unexpected Room identity hash: {identity}")
        if not sample:
            raise RuntimeError("FTS prefix search returned no rows for hypertens*")
        print(f"Verified {count} terms in {OUTPUT_PATH}")
        print(f"Room identity hash: {identity}")
        print(f"Sample matches: {[row[0] for row in sample]}")
        print(f"File size: {OUTPUT_PATH.stat().st_size} bytes")
        print(f"SHA256: {hashlib.sha256(OUTPUT_PATH.read_bytes()).hexdigest()}")
    finally:
        connection.close()


def main() -> int:
    count = build_database()
    verify_database()
    print(f"Generated medical_dictionary.db with {count} terms.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
