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

class PillReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ensureChannel()
        val title = EobStrings.t(AppLanguage.English, "rxVaultPillReminderTitle")
        val body = EobStrings.t(AppLanguage.English, "rxVaultPillReminderBody")
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            EobStrings.t(AppLanguage.English, "rxVaultPillReminderChannel"),
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "rx_pill_reminder_daily"
        const val CHANNEL_ID = "rx_pill_reminders"
        const val NOTIFICATION_ID = 7101
    }
}
