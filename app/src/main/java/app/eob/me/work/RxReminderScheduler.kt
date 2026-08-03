package app.eob.me.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.eob.me.data.local.entity.MedicationRecord
import app.eob.me.data.repository.RxVaultRepository
import java.util.concurrent.TimeUnit

object RxReminderScheduler {

    fun ensureDailyPillReminder(context: Context) {
        val request = PeriodicWorkRequestBuilder<PillReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PillReminderWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleRefillAlert(context: Context, medication: MedicationRecord) {
        if (medication.id <= 0L) return
        val delayMillis = RxVaultRepository.refillAlertDelayMillis(medication)
        val request = OneTimeWorkRequestBuilder<RefillAlertWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    RefillAlertWorker.KEY_MEDICATION_ID to medication.id,
                    RefillAlertWorker.KEY_MEDICATION_NAME to medication.medicationName
                )
            )
            .addTag(RefillAlertWorker.uniqueWorkName(medication.id))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            RefillAlertWorker.uniqueWorkName(medication.id),
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelRefillAlert(context: Context, medicationId: Long) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(RefillAlertWorker.uniqueWorkName(medicationId))
    }
}
