package app.eob.me.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object FsaDoomsdayScheduler {
    fun schedule(context: Context, fsaAllocation: Double, eligibleClaimAmount: Double) {
        if (fsaAllocation <= 0.0) {
            cancel(context)
            return
        }
        val input = Data.Builder()
            .putDouble(FsaDoomsdayNotificationWorker.KEY_FSA_ALLOCATION, fsaAllocation)
            .putDouble(
                FsaDoomsdayNotificationWorker.KEY_ELIGIBLE_CLAIM_AMOUNT,
                eligibleClaimAmount
            )
            .putInt(
                FsaDoomsdayNotificationWorker.KEY_NOTIFICATION_ID,
                FsaDoomsdayNotificationWorker.DEFAULT_NOTIFICATION_ID
            )
            .build()
        val request = PeriodicWorkRequestBuilder<FsaDoomsdayNotificationWorker>(7, TimeUnit.DAYS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FsaDoomsdayNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FsaDoomsdayNotificationWorker.WORK_NAME)
    }
}
