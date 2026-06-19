package app.eob.me

import android.app.Application
import app.eob.me.billing.RevenueCatConfig
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class EobApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Purchases.configure(
            PurchasesConfiguration.Builder(this, RevenueCatConfig.PUBLIC_API_KEY).build()
        )
    }
}
