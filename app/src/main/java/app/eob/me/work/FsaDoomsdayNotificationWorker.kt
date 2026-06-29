package app.eob.me.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.eob.me.R
import app.eob.me.data.EobStrings
import app.eob.me.data.AppLanguage

class FsaDoomsdayNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val phase = inputData.getString(KEY_PHASE).orEmpty()
        val daysRemaining = inputData.getInt(KEY_DAYS_REMAINING, 0)
        val eligibleAmount = inputData.getString(KEY_ELIGIBLE_AMOUNT).orEmpty()
        val unspentAmount = inputData.getString(KEY_UNSPENT_AMOUNT).orEmpty()
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID)

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
        const val KEY_PHASE = "phase"
        const val KEY_DAYS_REMAINING = "days_remaining"
        const val KEY_ELIGIBLE_AMOUNT = "eligible_amount"
        const val KEY_UNSPENT_AMOUNT = "unspent_amount"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val DEFAULT_NOTIFICATION_ID = 13601
        const val PHASE_GREEN = "GREEN"
        const val PHASE_ORANGE = "ORANGE"
        const val PHASE_RED = "RED"
    }
}
