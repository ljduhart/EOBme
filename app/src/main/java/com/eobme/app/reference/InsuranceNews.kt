package com.eobme.app.reference

object InsuranceNews {

    data class NewsItem(
        val title: String,
        val summary: String,
        val source: String,
        val date: String
    )

    val recentNews: List<NewsItem> = listOf(
        NewsItem(
            title = "CMS Finalizes 2026 Medicare Advantage Rates",
            summary = "The Centers for Medicare & Medicaid Services announced final payment rates for 2026 Medicare Advantage plans, with an average increase of 4.3% in benchmark payments to managed-care insurers.",
            source = "CMS.gov",
            date = "2026-04-02"
        ),
        NewsItem(
            title = "UnitedHealthcare Expands Telehealth Coverage",
            summary = "UnitedHealthcare announced expanded telehealth benefits for all commercial plan members, making virtual visits permanently available at in-network copay rates for primary care and behavioral health.",
            source = "UnitedHealthcare",
            date = "2026-03-15"
        ),
        NewsItem(
            title = "Blue Cross Blue Shield Launches Preventive Care Initiative",
            summary = "The BCBS Association launched a nationwide preventive care initiative covering annual wellness visits, key screenings, and immunizations at zero cost-share for members across all plan types.",
            source = "BCBS Association",
            date = "2026-03-01"
        ),
        NewsItem(
            title = "Cigna Healthcare Introduces Prescription Savings Program",
            summary = "Cigna Healthcare announced a new program capping out-of-pocket insulin costs at $35/month for all commercial members and expanding coverage of biosimilar medications.",
            source = "Cigna Healthcare",
            date = "2026-02-20"
        ),
        NewsItem(
            title = "Aetna Enhances Mental Health Access",
            summary = "Aetna expanded its behavioral health provider network by 30% and introduced same-day virtual therapy appointments to address rising demand for mental health services.",
            source = "Aetna",
            date = "2026-02-10"
        ),
        NewsItem(
            title = "Humana Adds New Medicare Supplement Benefits",
            summary = "Humana announced enhanced supplemental benefits for 2026 Medicare Advantage plans, including expanded dental, vision, hearing, and over-the-counter medication coverage.",
            source = "Humana",
            date = "2026-01-28"
        ),
        NewsItem(
            title = "Kaiser Permanente Reports Record Preventive Screening Rates",
            summary = "Kaiser Permanente reported that preventive screening rates among its 12.7 million members reached an all-time high, with colorectal cancer screening exceeding 80%.",
            source = "Kaiser Permanente",
            date = "2026-01-15"
        ),
        NewsItem(
            title = "No Surprises Act Enforcement Strengthened",
            summary = "Federal regulators announced stricter enforcement of the No Surprises Act, increasing penalties for providers who fail to provide good-faith cost estimates to uninsured patients.",
            source = "HHS.gov",
            date = "2025-12-18"
        ),
        NewsItem(
            title = "Anthem BCBS Introduces AI-Powered Prior Authorization",
            summary = "Anthem Blue Cross Blue Shield rolled out an automated prior authorization system using AI to reduce approval wait times from days to hours for common procedures.",
            source = "Anthem",
            date = "2025-11-30"
        ),
        NewsItem(
            title = "Molina Healthcare Expands Medicaid Coverage in Five States",
            summary = "Molina Healthcare was awarded new Medicaid managed care contracts in five states, extending coverage to an additional 1.2 million low-income members.",
            source = "Molina Healthcare",
            date = "2025-11-15"
        ),
        NewsItem(
            title = "Oscar Health Introduces Transparent Pricing Tool",
            summary = "Oscar Health launched a price transparency tool allowing members to see real-time cost estimates for procedures, labs, and imaging at nearby facilities before scheduling.",
            source = "Oscar Health",
            date = "2025-10-22"
        ),
        NewsItem(
            title = "New Federal Rule Requires Insurers to Cover Contraception",
            summary = "A new federal rule requires all private insurers to cover all FDA-approved contraceptive methods without cost-sharing, including over-the-counter options.",
            source = "HHS.gov",
            date = "2025-10-01"
        ),
        NewsItem(
            title = "TRICARE Updates Pharmacy Benefits",
            summary = "TRICARE announced updates to its pharmacy benefits program, adding 15 new generic medications to the formulary and lowering copays for maintenance drugs.",
            source = "Defense Health Agency",
            date = "2025-09-15"
        ),
        NewsItem(
            title = "Centene Acquires Regional Health Plan",
            summary = "Centene Corporation completed the acquisition of a regional health plan, expanding its government-sponsored healthcare coverage to 28 million members nationwide.",
            source = "Centene",
            date = "2025-08-20"
        ),
        NewsItem(
            title = "Healthcare Price Transparency Rules Show Results",
            summary = "A federal report shows that hospital price transparency rules have led to measurable decreases in average negotiated rates for common procedures across commercial insurers.",
            source = "CMS.gov",
            date = "2025-07-10"
        )
    )
}
