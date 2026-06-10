package app.eob.me.data

enum class SubscriptionTier {
    Free,
    Premium;

    fun labelKey(): String = when (this) {
        Free -> "settingsTierFree"
        Premium -> "settingsTierPremium"
    }
}

enum class AppLockTimeout(val millis: Long) {
    Immediately(0L),
    OneMinute(60_000L),
    FiveMinutes(300_000L);

    fun labelKey(): String = when (this) {
        Immediately -> "settingsLockImmediately"
        OneMinute -> "settingsLockOneMinute"
        FiveMinutes -> "settingsLockFiveMinutes"
    }
}

enum class ImageCompressionLevel(val jpegQuality: Int, val maxDimension: Int) {
    Low(jpegQuality = 95, maxDimension = 2_400),
    Medium(jpegQuality = 85, maxDimension = 1_800),
    High(jpegQuality = 70, maxDimension = 1_400);

    fun labelKey(): String = when (this) {
        Low -> "settingsCompressionLow"
        Medium -> "settingsCompressionMedium"
        High -> "settingsCompressionHigh"
    }
}

enum class SettingsTab {
    Account,
    Security,
    DocumentScan,
    Storage,
    Legal;

    fun labelKey(): String = when (this) {
        Account -> "settingsTabAccount"
        Security -> "settingsTabSecurity"
        DocumentScan -> "settingsTabDocumentScan"
        Storage -> "settingsTabStorage"
        Legal -> "settingsTabLegal"
    }
}

data class HubSettingsState(
    val biometricLoginEnabled: Boolean = false,
    val appLockTimeout: AppLockTimeout = AppLockTimeout.FiveMinutes,
    val crashlyticsOptIn: Boolean = true,
    val uploadOverWifiOnly: Boolean = false,
    val imageCompressionLevel: ImageCompressionLevel = ImageCompressionLevel.Medium,
    val autoCropEnabled: Boolean = true,
    val cacheSizeBytes: Long = 0L,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.Free,
    val settingsAccountEditing: Boolean = false,
    val settingsNotice: String = "",
    val appLocked: Boolean = false,
    val selectedTab: SettingsTab = SettingsTab.Account
)

object EobLegalUrls {
    const val TERMS_OF_USE = "https://ljduhart.github.io/EOBme/terms-of-use.html"
    const val PRIVACY_POLICY = "https://ljduhart.github.io/EOBme/privacy-policy.html"
}
