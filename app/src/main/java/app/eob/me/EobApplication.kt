package app.eob.me

import android.app.Application
import app.eob.me.billing.RevenueCatConfig
import app.eob.me.billing.RevenueCatManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith

class EobApplication : Application() {

    lateinit var revenueCatManager: RevenueCatManager
        private set

    override fun onCreate() {
        super.onCreate()
        revenueCatManager = RevenueCatManager()
        configureRevenueCat()
        linkRevenueCatToFirebaseAuth()
    }

    private fun configureRevenueCat() {
        if (!RevenueCatConfig.isConfigured) return

        if (BuildConfig.DEBUG) {
            Purchases.logLevel = LogLevel.DEBUG
        }

        val firebaseUid = runCatching {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            FirebaseAuth.getInstance().currentUser?.uid
        }.getOrNull()

        val configurationBuilder = PurchasesConfiguration.Builder(this, RevenueCatConfig.publicApiKey)
        if (!firebaseUid.isNullOrBlank()) {
            configurationBuilder.appUserID(firebaseUid)
        }

        Purchases.configure(configurationBuilder.build())
        revenueCatManager.attachToPurchases()
    }

    private fun linkRevenueCatToFirebaseAuth() {
        if (!RevenueCatConfig.isConfigured) return
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull() ?: return

        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Purchases.sharedInstance.logOutWith(
                    onError = { },
                    onSuccess = { }
                )
            } else {
                Purchases.sharedInstance.logInWith(
                    appUserID = uid,
                    onError = { },
                    onSuccess = { _, _ -> }
                )
            }
        }
    }
}
