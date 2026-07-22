package app.eob.me.data

data class UpcodingVerificationAlert(
    val cptCode: String,
    val requiredTimeRange: String,
    val isActive: Boolean = true
)

object UpcodingVerificationCalculator {
    fun upcodingVerificationForCharge(charge: EobCharge): UpcodingVerificationAlert? {
        val requiredTimeRange = UpcodingVerificationMap.requiredTimeFor(charge.cptCode) ?: return null
        return UpcodingVerificationAlert(
            cptCode = charge.cptCode,
            requiredTimeRange = requiredTimeRange,
            isActive = true
        )
    }

    fun upcodingVerificationsForRecord(record: EobRecord): List<UpcodingVerificationAlert> {
        return record.charges.mapNotNull { charge ->
            upcodingVerificationForCharge(charge)?.takeIf { alert -> alert.isActive }
        }
    }
}
