package app.eob.me.data

import java.util.Locale

object EobStrings {
    fun t(language: AppLanguage, key: String): String {
        localized[language]?.get(key)?.let { return it }
        localized[AppLanguage.English]?.get(key)?.let { return it }
        return key
    }

    fun tf(language: AppLanguage, key: String, vararg formatArgs: Any): String {
        return String.format(Locale.US, t(language, key), *formatArgs)
    }

    /** Keys referenced from UI — validated in [app.eob.me.EobStringsCoverageTest]. */
    val allEnglishKeys: Set<String> get() = english.keys

    fun localizedIntro(language: AppLanguage): List<Pair<String, String>> = intro(language)

    fun intro(language: AppLanguage): List<Pair<String, String>> {
        return when (language) {
            AppLanguage.Spanish -> listOf(
                "Comprenda su EOB" to "EOBme resume beneficios, cargos y responsabilidad del paciente.",
                "Controle códigos y costos" to "Revise CPT, pagos, ajustes, copagos, deducibles y coseguro.",
                "Prepare apelaciones" to "Genere una carta editable con los detalles disponibles."
            )
            AppLanguage.French -> listOf(
                "Comprendre votre EOB" to "EOBme résume les prestations, frais et montants patient.",
                "Suivre les codes et coûts" to "Consultez CPT, paiements, ajustements, franchises et coassurance.",
                "Préparer les appels" to "Générez une lettre modifiable avec les informations disponibles."
            )
            AppLanguage.Chinese -> listOf(
                "了解您的 EOB" to "EOBme 汇总福利说明、费用和患者责任。",
                "跟踪代码和费用" to "查看 CPT、付款、调整、自付额、免赔额和共同保险。",
                "准备申诉" to "使用可用信息生成可编辑的申诉信。"
            )
            AppLanguage.English -> listOf(
                "Understand your EOB" to "EOBme summarizes benefits, charges, and patient responsibility.",
                "Track codes and costs" to "Review CPTs, payments, adjustments, copays, deductibles, and coinsurance.",
                "Prepare appeals" to "Generate an editable appeal letter with available EOB details."
            )
        }
    }

    fun firebaseStatusText(language: AppLanguage, status: FirebaseSyncStatus): String {
        return when {
            !status.isConfigured -> t(language, "firebaseNotConfigured")
            status.userId.isNotBlank() -> t(language, "firebaseActive")
            else -> t(language, "firebaseConfigured")
        }
    }

    fun cptCategoryLabel(language: AppLanguage, category: CptCategory): String {
        return when (category) {
            CptCategory.OfficeVisit -> t(language, "categoryOfficeVisit")
            CptCategory.Lab -> t(language, "categoryLab")
            CptCategory.Hospital -> t(language, "categoryHospital")
            CptCategory.Dme -> t(language, "categoryDme")
            CptCategory.Injection -> t(language, "categoryInjection")
            CptCategory.Other -> t(language, "categoryOther")
        }
    }

    val sampleEobText = """
        UnitedHealthcare Explanation of Benefits
        Provider: Lakeside Family Medical Clinic
        Date of Service: 01/12/2025
        99215 billed $265.00 insurance paid $120.00 contractual adjustment $95.00 copay $25.00 deductible $20.00 coinsurance $5.00
        80053 billed $48.00 insurance paid $22.00 contractual adjustment $18.00 copay $0.00 deductible $8.00 coinsurance $0.00
    """.trimIndent()

    private val english = mapOf(
        "next" to "Next",
        "createAccount" to "Create account",
        "login" to "Log in",
        "chooseAccountAction" to "Choose how you want to continue",
        "forgotPassword" to "Forgot password?",
        "forgotUsername" to "Forgot username?",
        "forgotUsernameHelp" to "Your EOBme username is your email address. Check your inbox for EOBme or contact support if you no longer know which email you used.",
        "passwordResetSent" to "Password reset email sent if the account exists.",
        "profileRequired" to "Create your profile",
        "profileRequiredHelp" to "First name, last name, email, city, and state are required. Password is collected only during sign-in or account creation. Insurance ID and insurance name are optional.",
        "passwordRule" to "Password must be at least 8 characters and include at least 1 number.",
        "home" to "Home",
        "analysis" to "EOB History",
        "history" to "EOB History",
        "dashboard" to "Dashboard",
        "analysisHelp" to "EOBs are organized by Date of Service. Tap an EOB to show deductible, copay, coinsurance, insurance paid, and other details.",
        "cptCount" to "CPT Count",
        "news" to "News",
        "appeal" to "Appeal",
        "profile" to "Profile",
        "support" to "Support",
        "logout" to "Log out",
        "uploadEob" to "Upload EOB",
        "duplicateReplaced" to "Duplicate EOB found. The original copy was replaced with this upload.",
        "eobAdded" to "EOB added.",
        "eobs" to "EOBs",
        "insuranceCard" to "Insurance card",
        "insuranceNameField" to "Insurance Name",
        "insuranceId" to "Insurance ID",
        "groupName" to "Group Name",
        "addInsuranceInfo" to "Add insurance details in profile settings",
        "member" to "Member",
        "profileIncomplete" to "Profile incomplete",
        "quickActions" to "Quick Actions",
        "scanBill" to "Scan Bill",
        "appointmentCalendar" to "Appointment calendar",
        "appointmentDate" to "Appointment date",
        "appointmentProvider" to "Provider or doctor",
        "time" to "Time",
        "appointmentNotes" to "Appointment notes",
        "addAppointment" to "Add appointment",
        "saveAppointment" to "Save appointment",
        "removeAppointment" to "Remove appointment",
        "delete" to "Delete",
        "deleteEob" to "Delete EOB",
        "deleteNews" to "Delete news",
        "noAppointments" to "No doctor appointments added yet.",
        "selectAppointmentDate" to "Select a date to add an appointment.",
        "uploadHelp" to "Choose an EOB image or PDF from your library, scan with camera, or paste OCR text for testing.",
        "eobText" to "Optional pasted EOB text",
        "libraryUpload" to "Library upload",
        "uploadFromLibrary" to "Upload from library",
        "libraryUploadStarted" to "Library file selected. OCR analysis started.",
        "cameraScan" to "Camera scan",
        "scanWithCamera" to "Scan with camera",
        "cameraScanStarted" to "Camera image captured. OCR analysis started.",
        "cameraPermissionRequired" to "Camera permission is required to scan an EOB.",
        "ocrEmpty" to "OCR did not find text. Try a clearer image or PDF.",
        "ocrFailed" to "OCR failed. Please try another image.",
        "close" to "Close",
        "insuranceNews" to "Insurance news",
        "analysisResults" to "Analysis Results",
        "insurance" to "Insurance",
        "provider" to "Provider",
        "dateOfService" to "Date of Service",
        "eobBilledAmount" to "EOB billed amount",
        "insurancePaid" to "Insurance paid",
        "contractualAdjustment" to "Contractual adjustment",
        "copay" to "Copay",
        "deductible" to "Deductible",
        "coinsurance" to "Coinsurance",
        "noDuplicateCharges" to "No duplicate charges detected.",
        "billed" to "Billed",
        "paid" to "Paid",
        "cptsBilledIn" to "CPTs billed in",
        "cptRule" to "Only five-character CPT/HCPCS codes starting with 1-9 or A-J are stored.",
        "category" to "Category",
        "noCptsFound" to "No CPTs found for",
        "appealLetter" to "Appeal letter",
        "appealHelp" to "Auto-filled when profile details are saved and provider information is retrievable from the selected EOB.",
        "currentMember" to "Current member",
        "selectedEob" to "Selected EOB",
        "noEobSelected" to "No EOB selected",
        "autoFillAppeal" to "Auto-fill appeal letter",
        "editAppealLetter" to "Edit appeal letter",
        "userProfile" to "User profile",
        "saveProfile" to "Save profile",
        "editSavedDetails" to "Edit saved details",
        "imagePrepFailed" to "Could not prepare the image for upload. Try another file or a clearer photo.",
        "languageSettings" to "Language settings",
        "firstName" to "First name",
        "lastName" to "Last name",
        "email" to "Email",
        "password" to "Password",
        "city" to "City",
        "state" to "State",
        "insuranceCardDetails" to "Insurance details",
        "verifyEmailTitle" to "Verify your email",
        "verifyEmailHelp" to "A verification email was sent. Verify your email, then return here and continue.",
        "resendVerification" to "Resend verification email",
        "iVerifiedEmail" to "I verified my email",
        "howToUse" to "How to use EOBme",
        "supportStep1" to "1. Select your language, review the three intro screens, and create your profile.",
        "supportStep2" to "2. Upload or scan an EOB, then review insurance, provider, CPTs, billed amount, insurance paid, contractual adjustment, copay, deductible, and coinsurance.",
        "supportStep3" to "3. Use Analysis to find EOBs sorted by Date of Service.",
        "supportStep4" to "4. Use CPT Count to see yearly counts such as 99215 (5x), with category tabs for OVs, labs, hospital, DME, and injections.",
        "supportStep5" to "5. Use Quick Actions for appointments and Appeal Letter to edit a draft.",
        "features" to "Features",
        "featuresText" to "Firebase auth, OCR uploads, appointment calendar, EOB analysis, CPT tracking, appeal drafts, multilingual text, and secure inactivity logout.",
        "categoryOfficeVisit" to "OVs",
        "categoryLab" to "Labs",
        "categoryHospital" to "Hospital",
        "categoryDme" to "DME",
        "categoryInjection" to "Injections",
        "categoryOther" to "Other",
        "patientResponsibility" to "Patient responsibility",
        "firebaseNotConfigured" to "Firebase is not configured for this build.",
        "firebaseActive" to "Firebase sync active",
        "firebaseConfigured" to "Firebase configured — sign in to sync",
        "welcomeUser" to "Welcome, %s",
        "homeRecordSummary" to "%d %s • %s",
        "eobSingular" to "EOB",
        "featuresSection" to "Features",
        "insuranceNewsTitle" to "Insurance news • %d",
        "insuranceNewsSubtitle" to "Updates from United Healthcare, Medicare, Aetna, Blue Cross, and Medicaid.",
        "insuranceNewsMonthlyBriefings" to "Tap to read • 12 monthly briefings",
        "calendarFullMonthView" to "Full month view",
        "calendarWeekView" to "Week view",
        "calendarExpand" to "Expand",
        "closeReader" to "Close reader",
        "capturing" to "Capturing...",
        "signInBeforeUpload" to "Please sign in before uploading an EOB.",
        "bottomDashboard" to "Dashboard",
        "bottomScanEob" to "Scan EOB",
        "bottomProfile" to "Profile",
        "bentoProviderDirectory" to "Provider Directory",
        "bentoEobHistory" to "EOB History",
        "bentoCptTracker" to "CPT Tracker",
        "bentoYtdExpense" to "Y-T-D Expense",
        "bentoInsuranceNews" to "Insurance News",
        "bentoAppealGenerator" to "Appeal Generator",
        "historyEmptyHint" to "Scan an EOB with the camera button to build your history grid.",
        "pageOf" to "Page %1\$d of %2\$d",
        "pageSlash" to "Page %1\$d / %2\$d",
        "expenseAnalytics" to "Expense Analytics",
        "dashboardUploadHint" to "Upload an EOB invoice to view analytics charting summaries.",
        "claimAllocationProfile" to "Claim Allocation Profile",
        "totalAggregatedClaims" to "Total Aggregated Claims: $%.2f",
        "networkSavingsAdjustments" to "Network Savings Adjustments",
        "coveredByCarrierPlan" to "Covered By Carrier Plan",
        "yourPatientResponsibility" to "Your Patient Responsibility",
        "spendingByFacility" to "Spending by Medical Facility",
        "unknownProvider" to "Unknown Provider",
        "appealGeneratorTitle" to "Appeal Generator",
        "appealSelectClaimHint" to "Select a claim from the History tab to generate an appeal letter.",
        "appealingProvider" to "Appealing: %s",
        "appealServiceDate" to "Service Date: %s",
        "appealDraftPlaceholder" to "Drafting your appeal...",
        "appealRegenerate" to "Regenerate",
        "appealCopy" to "Copy",
        "insuranceIntelligence" to "Insurance Intelligence",
        "insuranceIntelligenceSubtitle" to "Policy updates, coverage warnings, and carrier alerts",
        "insuranceIntelligenceTip" to "EOBme cross-references your uploaded provider networks with regional policy changes to flag unexpected coverage adjustments.",
        "insuranceNewsAllClear" to "All clear! No pending network alerts or updates.",
        "cptTrackingTitle" to "Medical Code Tracking",
        "cptTrackingSubtitle" to "Analyze procedural frequency and billing distributions",
        "cptCodeDescriptionHeader" to "Code / Description",
        "cptFrequencyHeader" to "Frequency",
        "cptBilledTotalHeader" to "Billed Total",
        "cptNoProcedures" to "No recorded procedures match this category footprint.",
        "cptCodeLabel" to "CPT %s",
        "unspecifiedProcedure" to "Unspecified Procedure",
        "providerDirectoryTitle" to "Provider Directory",
        "providerDirectorySubtitle" to "Facilities and clinicians extracted from your synced EOB history.",
        "providerDirectoryEmpty" to "Providers will appear here after EOBs are scanned and saved to Firestore.",
        "providerEobSummary" to "EOBs: %1\$d • Last service: %2\$s",
        "providerBilledPaid" to "Billed: %1\$s • Paid: %2\$s",
        "yearlyExpenseTitle" to "Yearly Expense",
        "yearlyHealthCostDashboard" to "Yearly Health Cost Dashboard",
        "yearlyNoEobs" to "No EOBs yet",
        "yearLabel" to "Year: %s • EOBs: %d",
        "totalBilled" to "Total billed",
        "totalInsurancePaid" to "Insurance paid",
        "contractualAdjustments" to "Contractual adjustments",
        "copays" to "Copays",
        "deductibles" to "Deductibles",
        "coinsuranceLabel" to "Coinsurance",
        "datePlaceholder" to "MM/DD/YYYY",
        "previous" to "Previous",
        "nextPage" to "Next",
        "signInBeforeScan" to "Please sign in before scanning an EOB.",
        "signInToDeleteEob" to "Please sign in to delete EOBs from the cloud.",
        "signInToSaveProfile" to "Please sign in to save your profile.",
        "verificationEmailSentSignup" to "Verification email sent. Check your email before continuing.",
        "verificationEmailRequired" to "Email verification required. Check your original verification email before continuing.",
        "invalidCredentials" to "Invalid credentials. Please try again.",
        "authErrorGeneric" to "An authentication error occurred.",
        "verificationEmailResent" to "Verification email sent. Check your inbox.",
        "resendVerificationFailed" to "Unable to resend verification email.",
        "emailNotVerifiedYet" to "Email is not verified yet. Please check your inbox and try again.",
        "profileSavedPasswordRule" to "Profile saved. Password must be at least 8 characters and include a number.",
        "profileAndPasswordSaved" to "Profile and password saved.",
        "profileSavedPasswordFailed" to "Profile saved. Password update failed: %s",
        "profileSaved" to "Profile saved.",
        "unableToSaveProfile" to "Unable to save profile.",
        "firebaseConfigMissing" to "Firebase config was not included in this build. Confirm app/google-services.json exists, contains package_name app.eob.me, then Sync Gradle and rebuild the signed AAB.",
        "calendarThisWeek" to "This week",
        "patientOutOfPocketShare" to "Patient Out-of-Pocket Share: $%.2f",
        "providerNameMissing" to "—",
        "cptCountTimes" to "%dx",
        "editProfile" to "Edit",
        "profileSavedButton" to "Saved",
        "homeInsuranceCardTitle" to "Insurance card",
        "insuranceCardMemberLabel" to "Member name",
        "valueNotSet" to "—",
        "editAppointment" to "Edit appointment",
        "updateAppointment" to "Update appointment",
        "profileSavedFirebase" to "Profile saved to Firebase.",
        "profileSaveFailed" to "Profile save failed: %s",
        "insuranceMetadataSaved" to "Insurance card metadata saved.",
        "insuranceMetadataSaveFailed" to "Insurance card metadata save failed: %s",
        "signInToSaveEob" to "Please sign in to save EOBs.",
        "signInToDeleteEobsShort" to "Please sign in to delete EOBs.",
        "eobSavedFirebase" to "EOB saved to Firebase.",
        "eobSaveFailed" to "EOB save failed: %s",
        "eobDeleted" to "EOB deleted.",
        "eobDeleteFailed" to "EOB delete failed: %s",
        "eobUploadVeryfiStarted" to "EOB uploaded. Veryfi processing started.",
        "eobUploadFailed" to "EOB upload failed: %s"
    )

    private val exactRepositoryMessages = mapOf(
        "Please sign in to save your profile." to "signInToSaveProfile",
        "Profile saved to Firebase." to "profileSavedFirebase",
        "Insurance card metadata saved." to "insuranceMetadataSaved",
        "Please sign in to save EOBs." to "signInToSaveEob",
        "EOB saved to Firebase." to "eobSavedFirebase",
        "Please sign in to delete EOBs." to "signInToDeleteEobsShort",
        "EOB deleted." to "eobDeleted",
        "Please sign in before uploading an EOB." to "signInBeforeUpload",
        "Please sign in before scanning an EOB." to "signInBeforeScan",
        "EOB uploaded. Veryfi processing started." to "eobUploadVeryfiStarted"
    )

    fun localizeRepositoryMessage(language: AppLanguage, message: String): String {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return ""
        exactRepositoryMessages[trimmed]?.let { return t(language, it) }
        return when {
            trimmed.startsWith("Profile save failed:") ->
                tf(language, "profileSaveFailed", trimmed.substringAfter(":").trim())
            trimmed.startsWith("Insurance card metadata save failed:") ->
                tf(language, "insuranceMetadataSaveFailed", trimmed.substringAfter(":").trim())
            trimmed.startsWith("EOB save failed:") ->
                tf(language, "eobSaveFailed", trimmed.substringAfter(":").trim())
            trimmed.startsWith("EOB delete failed:") ->
                tf(language, "eobDeleteFailed", trimmed.substringAfter(":").trim())
            trimmed.startsWith("EOB upload failed:") ->
                tf(language, "eobUploadFailed", trimmed.substringAfter(":").trim())
            else -> trimmed
        }
    }

    fun firebaseConfigMessage(language: AppLanguage): String = t(language, "firebaseConfigMissing")

    private val spanish = english + mapOf(
        "patientResponsibility" to "Responsabilidad del paciente",
        "firebaseNotConfigured" to "Firebase no está configurado en esta compilación.",
        "firebaseActive" to "Sincronización de Firebase activa",
        "firebaseConfigured" to "Firebase configurado — inicie sesión para sincronizar",
        "welcomeUser" to "Bienvenido, %s",
        "featuresSection" to "Funciones",
        "bottomDashboard" to "Panel",
        "bottomScanEob" to "Escanear EOB",
        "bottomProfile" to "Perfil",
        "calendarExpand" to "Expandir",
        "calendarWeekView" to "Vista semanal",
        "closeReader" to "Cerrar lector",
        "signInBeforeUpload" to "Inicie sesión antes de subir un EOB.",
        "next" to "Siguiente",
        "createAccount" to "Crear cuenta",
        "login" to "Iniciar sesión",
        "home" to "Inicio",
        "insuranceNameField" to "Nombre del seguro",
        "insuranceId" to "ID del seguro",
        "groupName" to "Nombre del grupo",
        "addInsuranceInfo" to "Agregue detalles del seguro en el perfil",
        "analysis" to "Análisis",
        "cptCount" to "CPT",
        "news" to "Noticias",
        "appeal" to "Apelación",
        "profile" to "Perfil",
        "support" to "Ayuda",
        "logout" to "Salir",
        "scanBill" to "Escanear EOB",
        "uploadFromLibrary" to "Subir desde biblioteca",
        "scanWithCamera" to "Escanear con cámara"
    )

    private val french = english + mapOf(
        "patientResponsibility" to "Responsabilité du patient",
        "firebaseNotConfigured" to "Firebase n'est pas configuré pour cette version.",
        "firebaseActive" to "Synchronisation Firebase active",
        "firebaseConfigured" to "Firebase configuré — connectez-vous pour synchroniser",
        "welcomeUser" to "Bienvenue, %s",
        "featuresSection" to "Fonctions",
        "bottomDashboard" to "Tableau de bord",
        "bottomScanEob" to "Scanner EOB",
        "bottomProfile" to "Profil",
        "calendarExpand" to "Développer",
        "calendarWeekView" to "Vue semaine",
        "closeReader" to "Fermer le lecteur",
        "signInBeforeUpload" to "Connectez-vous avant de téléverser un EOB.",
        "next" to "Suivant",
        "createAccount" to "Créer un compte",
        "login" to "Connexion",
        "home" to "Accueil",
        "insuranceNameField" to "Nom de l'assurance",
        "insuranceId" to "ID d'assurance",
        "groupName" to "Nom du groupe",
        "addInsuranceInfo" to "Ajoutez les détails d'assurance dans le profil",
        "analysis" to "Analyse",
        "news" to "Actualités",
        "appeal" to "Appel",
        "profile" to "Profil",
        "support" to "Aide",
        "logout" to "Déconnexion",
        "scanBill" to "Scanner EOB",
        "uploadFromLibrary" to "Depuis bibliothèque",
        "scanWithCamera" to "Scanner avec caméra"
    )

    private val chinese = english + mapOf(
        "patientResponsibility" to "患者责任",
        "firebaseNotConfigured" to "此版本未配置 Firebase。",
        "firebaseActive" to "Firebase 同步已启用",
        "firebaseConfigured" to "Firebase 已配置 — 登录以同步",
        "welcomeUser" to "欢迎，%s",
        "featuresSection" to "功能",
        "bottomDashboard" to "仪表板",
        "bottomScanEob" to "扫描 EOB",
        "bottomProfile" to "资料",
        "calendarExpand" to "展开",
        "calendarWeekView" to "周视图",
        "closeReader" to "关闭阅读器",
        "signInBeforeUpload" to "上传 EOB 前请先登录。",
        "next" to "下一步",
        "createAccount" to "创建账户",
        "login" to "登录",
        "home" to "主页",
        "insuranceNameField" to "保险名称",
        "insuranceId" to "保险 ID",
        "groupName" to "团体名称",
        "addInsuranceInfo" to "请在资料中添加保险信息",
        "analysis" to "分析",
        "news" to "新闻",
        "appeal" to "申诉",
        "profile" to "资料",
        "support" to "支持",
        "logout" to "退出",
        "scanBill" to "扫描 EOB",
        "uploadFromLibrary" to "从图库上传",
        "scanWithCamera" to "用相机扫描"
    )

    private val localized = mapOf(
        AppLanguage.English to english,
        AppLanguage.Spanish to spanish,
        AppLanguage.French to french,
        AppLanguage.Chinese to chinese
    )
}
