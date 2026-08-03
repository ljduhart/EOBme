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

class RefillAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getLong(KEY_MEDICATION_ID, -1L)
        val medicationName = inputData.getString(KEY_MEDICATION_NAME).orEmpty()
        if (medicationId <= 0L || medicationName.isBlank()) return Result.success()

        ensureChannel()
        val title = EobStrings.t(AppLanguage.English, "rxVaultRefillAlertTitle")
        val body = EobStrings.tf(AppLanguage.English, "rxVaultRefillAlertBody", medicationName)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java)
            ?.notify(medicationId.toInt(), notification)
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            EobStrings.t(AppLanguage.English, "rxVaultRefillAlertChannel"),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_MEDICATION_NAME = "medication_name"
        const val CHANNEL_ID = "rx_refill_alerts"

        fun uniqueWorkName(medicationId: Long): String = "rx_refill_alert_$medicationId"
    }
}
