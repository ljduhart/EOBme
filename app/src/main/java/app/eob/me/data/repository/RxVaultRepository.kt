package app.eob.me.data.repository

import android.content.Context
import app.eob.me.data.MedicationDoseSlot
import app.eob.me.data.local.EobmeRoomDatabase
import app.eob.me.data.local.entity.MedicationDoseLogEntity
import app.eob.me.data.local.entity.MedicationRecord
import app.eob.me.data.local.entity.TaxVaultExpenseLedgerEntity
import app.eob.me.work.RxReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RxVaultRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = EobmeRoomDatabase.getInstance(appContext)
    private val medicationDao = database.medicationDao()
    private val ledgerDao = database.taxVaultExpenseLedgerDao()
    private val doseLogDao = database.medicationDoseLogDao()

    fun observeMedications(): Flow<List<MedicationRecord>> = medicationDao.getAllMedications()

    fun observeFsaLedgerTotalForCurrentYear(): Flow<Double> {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return ledgerDao.observeFsaEligibleTotalForYear(year)
    }

    fun observeTodayDoseLogs(dayStartMillis: Long): Flow<List<MedicationDoseLogEntity>> {
        return doseLogDao.observeLogsForDay(dayStartMillis)
    }

    suspend fun insertMedication(record: MedicationRecord): Long {
        val id = medicationDao.insert(record)
        bridgeCopayToTaxVaultLedger(id, record)
        val persisted = medicationDao.getMedicationById(id) ?: record.copy(id = id)
        RxReminderScheduler.scheduleRefillAlert(appContext, persisted)
        RxReminderScheduler.ensureDailyPillReminder(appContext)
        return id
    }

    suspend fun updateMedication(record: MedicationRecord) {
        medicationDao.update(record)
        RxReminderScheduler.scheduleRefillAlert(appContext, record)
    }

    suspend fun deleteMedication(record: MedicationRecord) {
        if (record.id > 0L) {
            ledgerDao.deleteForMedication(record.id)
            doseLogDao.deleteForMedication(record.id)
        }
        medicationDao.delete(record)
        RxReminderScheduler.cancelRefillAlert(appContext, record.id)
    }

    suspend fun setDoseTaken(
        medicationId: Long,
        dayStartMillis: Long,
        slot: MedicationDoseSlot,
        taken: Boolean
    ) {
        val existing = doseLogDao.findLog(medicationId, dayStartMillis, slot.storageKey)
        doseLogDao.upsert(
            MedicationDoseLogEntity(
                id = existing?.id ?: 0L,
                medicationId = medicationId,
                dayStartMillis = dayStartMillis,
                slot = slot.storageKey,
                taken = taken
            )
        )
    }

    /**
     * When a medication carries an FSA-eligible copay, record it in the local Tax Vault expense ledger
     * for year-to-date HSA/FSA tracking.
     */
    private suspend fun bridgeCopayToTaxVaultLedger(medicationId: Long, record: MedicationRecord) {
        if (record.copayAmount <= 0.0 || !record.isFsaEligible) return
        val calendar = Calendar.getInstance()
        ledgerDao.insert(
            TaxVaultExpenseLedgerEntity(
                medicationId = medicationId,
                description = record.medicationName.trim(),
                amount = record.copayAmount,
                isFsaEligible = true,
                recordedAtMillis = System.currentTimeMillis(),
                calendarYear = calendar.get(Calendar.YEAR)
            )
        )
    }

    companion object {
        fun startOfDayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        fun refillAlertDelayMillis(record: MedicationRecord, nowMillis: Long = System.currentTimeMillis()): Long {
            val supplyDays = record.quantity.coerceAtLeast(1)
            val alertDayIndex = ((supplyDays * 25) / 30).coerceAtLeast(1)
            val alertOffsetMillis = TimeUnit.DAYS.toMillis(alertDayIndex.toLong())
            val targetMillis = record.refillDate - TimeUnit.DAYS.toMillis(5)
            val fallbackMillis = nowMillis + alertOffsetMillis
            val triggerAt = minOf(targetMillis, fallbackMillis).coerceAtLeast(nowMillis + TimeUnit.MINUTES.toMillis(1))
            return (triggerAt - nowMillis).coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
        }
    }
}
