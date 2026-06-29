package app.eob.me.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.eob.me.R
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.FsaDoomsdayEngine
import app.eob.me.data.FsaDoomsdayPhase
import app.eob.me.data.asCurrency

class FsaDoomsdayNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val fsaAllocation = inputData.getDouble(KEY_FSA_ALLOCATION, 0.0)
        if (fsaAllocation <= 0.0) return Result.success()

        val eligibleClaimAmount = inputData.getDouble(KEY_ELIGIBLE_CLAIM_AMOUNT, 0.0)
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID)
        val snapshot = FsaDoomsdayEngine.snapshot(
            fsaAllocation = fsaAllocation,
            eligibleClaimAmount = eligibleClaimAmount
        )
        val phase = when (snapshot.phase) {
            FsaDoomsdayPhase.GREEN -> PHASE_GREEN
            FsaDoomsdayPhase.ORANGE -> PHASE_ORANGE
            FsaDoomsdayPhase.RED -> PHASE_RED
        }
        val daysRemaining = snapshot.daysRemaining
        val eligibleAmount = snapshot.eligibleClaimAmount.asCurrency()
        val unspentAmount = snapshot.unspentAmount.asCurrency()

        val title = when (phase) {
            PHASE_RED -> EobStrings.t(AppLanguage.English, "taxVaultFsaNotificationRedTitle")
            PHASE_ORANGE -> EobStrings.t(AppLanguage.English, "taxVaultFsaNotificationOrangeTitle")
            else -> EobStrings.t(AppLanguage.English, "taxVaultFsaNotificationGreenTitle")
        }
        val body = when (phase) {
            PHASE_RED -> EobStrings.tf(
                AppLanguage.English,
                "taxVaultFsaNotificationRedBody",
                daysRemaining
            )
            PHASE_ORANGE -> EobStrings.tf(
                AppLanguage.English,
                "taxVaultFsaNotificationOrangeBody",
                daysRemaining,
                unspentAmount
            )
            else -> EobStrings.tf(
                AppLanguage.English,
                "taxVaultFsaNotificationGreenBody",
                eligibleAmount
            )
        }

        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (phase == PHASE_RED) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setAutoCancel(true)
            .build()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            EobStrings.t(AppLanguage.English, "taxVaultFsaNotificationChannel"),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val WORK_NAME = "fsa_doomsday_monitor"
        const val CHANNEL_ID = "tax_vault_fsa_doomsday"
        const val KEY_FSA_ALLOCATION = "fsa_allocation"
        const val KEY_ELIGIBLE_CLAIM_AMOUNT = "eligible_claim_amount"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val DEFAULT_NOTIFICATION_ID = 13601
        const val PHASE_GREEN = "GREEN"
        const val PHASE_ORANGE = "ORANGE"
        const val PHASE_RED = "RED"
    }
}
