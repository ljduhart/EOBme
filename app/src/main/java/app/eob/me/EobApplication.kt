package app.eob.me

import android.app.Application
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class EobApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initializes RevenueCat with the project-specific public API key
        Purchases.configure(
            PurchasesConfiguration.Builder(this, "goog_rmhYQIPDsEWnEBFWUzMRYYlpYMo").build()
        )
    }
}
