package app.eob.me.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.eob.me.data.FsaDoomsdayEngine
import app.eob.me.data.FsaDoomsdayPhase
import app.eob.me.data.asCurrency
import java.util.concurrent.TimeUnit

object FsaDoomsdayScheduler {
    fun schedule(context: Context, fsaAllocation: Double, eligibleClaimAmount: Double) {
        if (fsaAllocation <= 0.0) {
            WorkManager.getInstance(context).cancelUniqueWork(FsaDoomsdayNotificationWorker.WORK_NAME)
            return
        }
        val snapshot = FsaDoomsdayEngine.snapshot(
            fsaAllocation = fsaAllocation,
            eligibleClaimAmount = eligibleClaimAmount
        )
        val phase = when (snapshot.phase) {
            FsaDoomsdayPhase.GREEN -> FsaDoomsdayNotificationWorker.PHASE_GREEN
            FsaDoomsdayPhase.ORANGE -> FsaDoomsdayNotificationWorker.PHASE_ORANGE
            FsaDoomsdayPhase.RED -> FsaDoomsdayNotificationWorker.PHASE_RED
        }
        val input = Data.Builder()
            .putString(FsaDoomsdayNotificationWorker.KEY_PHASE, phase)
            .putInt(FsaDoomsdayNotificationWorker.KEY_DAYS_REMAINING, snapshot.daysRemaining)
            .putString(
                FsaDoomsdayNotificationWorker.KEY_ELIGIBLE_AMOUNT,
                snapshot.eligibleClaimAmount.asCurrency()
            )
            .putString(
                FsaDoomsdayNotificationWorker.KEY_UNSPENT_AMOUNT,
                snapshot.unspentAmount.asCurrency()
            )
            .putInt(FsaDoomsdayNotificationWorker.KEY_NOTIFICATION_ID, FsaDoomsdayNotificationWorker.DEFAULT_NOTIFICATION_ID)
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
}
