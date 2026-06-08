package app.eob.me.data

object EobStrings {
    fun t(language: AppLanguage, key: String): String {
        return localized[language]?.get(key) ?: localized.getValue(AppLanguage.English).getValue(key)
    }

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
        "profileRequiredHelp" to "First name, last name, email, password, city, and state are required. Insurance ID and insurance name are optional.",
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
        "editSavedDetails" to "Edit saved details",
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
        "appealGeneratorTitle" to "Appeal Generator",
        "appealGeneratorScanning" to "Scanning local claims…",
        "appealGeneratorScanningDetail" to "Reviewing uploaded EOBs for disputable billing lines.",
        "appealGeneratorClaimsReady" to "%d pending claim(s) can be disputed",
        "appealGeneratorDisputeSummary" to "Tap to draft appeals for %d flagged claim(s).",
        "appealGeneratorAllClear" to "No pending disputable claims detected.",
        "appealGeneratorTapToOpen" to "Tap to open appeal letter",
        "providerDirectoryTitle" to "Provider Directory",
        "providerDirectorySubtitle" to "Network assurance badges help identify in-network and out-of-network clinicians.",
        "providerDirectoryEmpty" to "Providers will appear here after EOBs are scanned.",
        "providerEobSummary" to "EOBs: %1\$d • Last service: %2\$s",
        "providerBilledPaid" to "Billed: %1\$s • Paid: %2\$s",
        "patientResponsibility" to "Patient responsibility",
        "networkAssuranceInNetwork" to "In-Network",
        "networkAssurancePending" to "Pending Network",
        "networkAssuranceOutOfNetwork" to "Out-of-Network"
    )

    fun tf(language: AppLanguage, key: String, vararg formatArgs: Any): String {
        return String.format(java.util.Locale.US, t(language, key), *formatArgs)
    }

    private val spanish = english + mapOf(
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
