package app.eob.me.data

import android.content.Context

/**
 * Persists hub settings locally. [app.eob.me.viewmodel.EobViewModel] is the runtime source of truth.
 */
class HubSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): HubSettingsState {
        return HubSettingsState(
            biometricLoginEnabled = prefs.getBoolean(KEY_BIOMETRIC, false),
            appLockTimeout = AppLockTimeout.entries.getOrElse(prefs.getInt(KEY_LOCK_TIMEOUT, 2)) {
                AppLockTimeout.FiveMinutes
            },
            crashlyticsOptIn = prefs.getBoolean(KEY_CRASHLYTICS, true),
            uploadOverWifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
            imageCompressionLevel = ImageCompressionLevel.entries.getOrElse(
                prefs.getInt(KEY_COMPRESSION, ImageCompressionLevel.Medium.ordinal)
            ) { ImageCompressionLevel.Medium },
            autoCropEnabled = prefs.getBoolean(KEY_AUTO_CROP, true)
        )
    }

    fun write(state: HubSettingsState) {
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC, state.biometricLoginEnabled)
            .putInt(KEY_LOCK_TIMEOUT, state.appLockTimeout.ordinal)
            .putBoolean(KEY_CRASHLYTICS, state.crashlyticsOptIn)
            .putBoolean(KEY_WIFI_ONLY, state.uploadOverWifiOnly)
            .putInt(KEY_COMPRESSION, state.imageCompressionLevel.ordinal)
            .putBoolean(KEY_AUTO_CROP, state.autoCropEnabled)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "eobme_hub_settings"
        const val KEY_BIOMETRIC = "biometric_login"
        const val KEY_LOCK_TIMEOUT = "app_lock_timeout"
        const val KEY_CRASHLYTICS = "crashlytics_opt_in"
        const val KEY_WIFI_ONLY = "upload_wifi_only"
        const val KEY_COMPRESSION = "image_compression"
        const val KEY_AUTO_CROP = "auto_crop"
    }
}
