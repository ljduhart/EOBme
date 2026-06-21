package app.eob.me.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Persists hub settings locally. [app.eob.me.viewmodel.EobViewModel] is the runtime source of truth.
 */
class HubSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): HubSettingsState {
        migrateLegacyBiometricKey()
        return HubSettingsState(
            pinLockEnabled = prefs.getBoolean(KEY_PIN_LOCK, false),
            pinConfigured = hasAppPin(),
            appLockTimeout = readAppLockTimeout(),
            crashlyticsOptIn = prefs.getBoolean(KEY_CRASHLYTICS, true),
            uploadOverWifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
            imageCompressionLevel = ImageCompressionLevel.entries.getOrElse(
                prefs.getInt(KEY_COMPRESSION, ImageCompressionLevel.Medium.ordinal)
            ) { ImageCompressionLevel.Medium },
            autoCropEnabled = prefs.getBoolean(KEY_AUTO_CROP, true),
            darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false)
        )
    }

    fun write(state: HubSettingsState) {
        prefs.edit()
            .putBoolean(KEY_PIN_LOCK, state.pinLockEnabled)
            .putInt(KEY_LOCK_TIMEOUT, state.appLockTimeout.ordinal)
            .putInt(KEY_LOCK_TIMEOUT_VERSION, LOCK_TIMEOUT_SCHEMA_VERSION)
            .putBoolean(KEY_CRASHLYTICS, state.crashlyticsOptIn)
            .putBoolean(KEY_WIFI_ONLY, state.uploadOverWifiOnly)
            .putInt(KEY_COMPRESSION, state.imageCompressionLevel.ordinal)
            .putBoolean(KEY_AUTO_CROP, state.autoCropEnabled)
            .putBoolean(KEY_DARK_MODE, state.darkModeEnabled)
            .apply()
    }

    fun hasAppPin(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null)?.isNotBlank() == true
    }

    fun saveAppPin(pin: String) {
        val existingSalt = prefs.getString(KEY_PIN_SALT, null)
        val salt = existingSalt ?: generateSalt()
        prefs.edit()
            .apply { if (existingSalt == null) putString(KEY_PIN_SALT, salt) }
            .putString(KEY_PIN_HASH, hashPin(pin, salt))
            .apply()
    }

    fun verifyAppPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        return storedHash == hashPin(pin, salt)
    }

    fun clearAppPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .putBoolean(KEY_PIN_LOCK, false)
            .apply()
    }

    private fun readAppLockTimeout(): AppLockTimeout {
        val ordinal = prefs.getInt(KEY_LOCK_TIMEOUT, AppLockTimeout.FiveMinutes.ordinal)
        if (prefs.getInt(KEY_LOCK_TIMEOUT_VERSION, 0) < LOCK_TIMEOUT_SCHEMA_VERSION) {
            return when (ordinal) {
                0, 1, 2 -> AppLockTimeout.FiveMinutes
                else -> AppLockTimeout.FiveMinutes
            }
        }
        return AppLockTimeout.entries.getOrElse(ordinal) { AppLockTimeout.FiveMinutes }
    }

    private fun migrateLegacyBiometricKey() {
        if (prefs.contains(KEY_BIOMETRIC_LEGACY)) {
            prefs.edit().remove(KEY_BIOMETRIC_LEGACY).apply()
        }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PREFS_NAME = "eobme_hub_settings"
        const val KEY_PIN_LOCK = "pin_lock_enabled"
        const val KEY_BIOMETRIC_LEGACY = "biometric_login"
        const val KEY_PIN_HASH = "app_pin_hash"
        const val KEY_PIN_SALT = "app_pin_salt"
        const val KEY_LOCK_TIMEOUT = "app_lock_timeout"
        const val KEY_LOCK_TIMEOUT_VERSION = "app_lock_timeout_version"
        const val LOCK_TIMEOUT_SCHEMA_VERSION = 2
        const val KEY_CRASHLYTICS = "crashlytics_opt_in"
        const val KEY_WIFI_ONLY = "upload_wifi_only"
        const val KEY_COMPRESSION = "image_compression"
        const val KEY_AUTO_CROP = "auto_crop"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
