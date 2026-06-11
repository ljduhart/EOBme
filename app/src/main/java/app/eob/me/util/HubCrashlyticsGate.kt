package app.eob.me.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Safe Crashlytics collection toggle for Settings. Never throws into hub startup.
 */
object HubCrashlyticsGate {
    fun setCollectionEnabled(enabled: Boolean) {
        try {
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
        } catch (_: Throwable) {
            // Crashlytics may be unavailable before Firebase/Crashlytics Gradle wiring is complete.
        }
    }
}
