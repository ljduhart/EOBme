package app.eob.me.data

import java.util.Calendar

enum class FsaDoomsdayPhase {
    GREEN,
    ORANGE,
    RED
}

data class FsaDoomsdaySnapshot(
    val phase: FsaDoomsdayPhase,
    val daysRemaining: Int,
    val eligibleClaimAmount: Double,
    val unspentAmount: Double,
    val messageKey: String,
    val pulseAlert: Boolean = false
)

data class ReceiptRecord(
    val firestoreId: String,
    val providerName: String,
    val serviceDate: String,
    val serviceDateSortKey: Int = 0,
    val amount: Double,
    val thumbnailUrl: String,
    val storagePath: String,
    val stapledEobId: String = "",
    val createdAtMillis: Long = 0L
) {
    fun historyListKey(): String = "receipt:$firestoreId"
}

data class VaultEvidenceThumbnail(
    val id: String,
    val imageUrl: String,
    val label: String,
    val rotationDegrees: Float,
    val isReceipt: Boolean
)

sealed class VaultEvidencePreviewDetail {
    data class Eob(val record: EobRecord) : VaultEvidencePreviewDetail()
    data class Receipt(val record: ReceiptRecord) : VaultEvidencePreviewDetail()
}

data class TaxVaultExportRow(
    val date: String,
    val provider: String,
    val cptCode: String,
    val patientResponsibility: Double
)

enum class VaultSubstantiationStatus {
    NONE,
    PAID_AND_SUBSTANTIATED;

    companion object {
        fun fromFirestore(value: String): VaultSubstantiationStatus {
            return if (value.equals("Paid & Substantiated", ignoreCase = true)) {
                PAID_AND_SUBSTANTIATED
            } else {
                NONE
            }
        }
    }

    fun firestoreLabel(): String = when (this) {
        NONE -> ""
        PAID_AND_SUBSTANTIATED -> "Paid & Substantiated"
    }
}

object FsaDoomsdayEngine {
    private const val DEADLINE_MONTH = Calendar.DECEMBER
    private const val DEADLINE_DAY = 31

    fun snapshot(
        calendar: Calendar = Calendar.getInstance(),
        fsaAllocation: Double,
        eligibleClaimAmount: Double,
        nowMillis: Long = System.currentTimeMillis()
    ): FsaDoomsdaySnapshot {
        val month = calendar.get(Calendar.MONTH)
        val phase = when (month) {
            in Calendar.JANUARY..Calendar.AUGUST -> FsaDoomsdayPhase.GREEN
            in Calendar.SEPTEMBER..Calendar.NOVEMBER -> FsaDoomsdayPhase.ORANGE
            else -> FsaDoomsdayPhase.RED
        }
        val daysRemaining = daysUntilDeadline(calendar, nowMillis)
        val unspent = (fsaAllocation - eligibleClaimAmount).coerceAtLeast(0.0)
        val messageKey = when (phase) {
            FsaDoomsdayPhase.GREEN -> "taxVaultFsaDoomsdayGreen"
            FsaDoomsdayPhase.ORANGE -> "taxVaultFsaDoomsdayOrange"
            FsaDoomsdayPhase.RED -> "taxVaultFsaDoomsdayRed"
        }
        return FsaDoomsdaySnapshot(
            phase = phase,
            daysRemaining = daysRemaining,
            eligibleClaimAmount = eligibleClaimAmount,
            unspentAmount = unspent,
            messageKey = messageKey,
            pulseAlert = phase == FsaDoomsdayPhase.RED
        )
    }

    internal fun daysUntilDeadline(calendar: Calendar, nowMillis: Long): Int {
        val year = calendar.get(Calendar.YEAR)
        val deadline = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, DEADLINE_MONTH)
            set(Calendar.DAY_OF_MONTH, DEADLINE_DAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        if (nowMillis > deadline.timeInMillis) {
            return 0
        }
        val diffMs = deadline.timeInMillis - nowMillis
        return ((diffMs + 86_399_999L) / 86_400_000L).toInt().coerceAtLeast(0)
    }
}
