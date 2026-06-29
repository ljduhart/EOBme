package app.eob.me.data

import java.util.Locale
import java.util.regex.Pattern

object VaultReceiptMapper {
    private val moneyPattern = Pattern.compile("""\$\s*([0-9]+(?:\.[0-9]{2})?)""")
    private val datePattern = Pattern.compile(
        """\b(0?[1-9]|1[0-2])[/-](0?[1-9]|[12][0-9]|3[01])[/-]((?:20)?[0-9]{2})\b"""
    )

    fun receiptToMap(record: ReceiptRecord): Map<String, Any?> {
        return mapOf(
            "providerName" to record.providerName,
            "serviceDate" to record.serviceDate,
            "amount" to record.amount,
            "thumbnailUrl" to record.thumbnailUrl,
            "storagePath" to record.storagePath,
            "stapledEobId" to record.stapledEobId,
            "createdAtMillis" to record.createdAtMillis,
            "scanType" to CameraScanDocumentType.Receipt.name,
            "vaultRecord" to true,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    fun receiptFromMap(data: Map<String, Any?>, documentId: String): ReceiptRecord {
        return ReceiptRecord(
            firestoreId = documentId,
            providerName = data.stringValue("providerName", "provider_name").ifBlank { "Pharmacy Receipt" },
            serviceDate = data.stringValue("serviceDate", "service_date", "dateOfService", "date_of_service"),
            amount = data.doubleValue("amount", "patientAmount", "patient_amount"),
            thumbnailUrl = data.stringValue("thumbnailUrl", "thumbnail_url", "storageDownloadUrl", "storage_download_url"),
            storagePath = data.stringValue("storagePath", "storage_path", "sourceFilePath", "source_file_path"),
            stapledEobId = data.stringValue("stapledEobId", "stapled_eob_id"),
            createdAtMillis = data.longValue("createdAtMillis", "created_at_millis").takeUnless { it == 0L }
                ?: data.longValue("updatedAt", "updated_at")
        )
    }

    fun parseReceiptFromOcr(ocrText: String, fallbackProvider: String = "Pharmacy Receipt"): ReceiptOcrParseResult {
        val amount = moneyPattern.matcher(ocrText).let { matcher ->
            generateSequence { if (matcher.find()) matcher.group(1) else null }
                .mapNotNull { it?.toDoubleOrNull() }
                .maxOrNull()
        } ?: 0.0
        val serviceDate = datePattern.matcher(ocrText).let { matcher ->
            if (matcher.find()) matcher.group() else ""
        }
        val provider = ocrText.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length in 4..48 &&
                    !line.contains('$') &&
                    !datePattern.matcher(line).find()
            }
            ?: fallbackProvider
        return ReceiptOcrParseResult(
            providerName = provider,
            serviceDate = serviceDate,
            amount = amount
        )
    }

    data class ReceiptOcrParseResult(
        val providerName: String,
        val serviceDate: String,
        val amount: Double
    )

    private fun Map<String, Any?>.stringValue(vararg keys: String): String {
        keys.forEach { key ->
            val value = this[key]?.toString()?.trim().orEmpty()
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun Map<String, Any?>.doubleValue(vararg keys: String): Double {
        keys.forEach { key ->
            when (val value = this[key]) {
                is Number -> return value.toDouble()
                is String -> value.toDoubleOrNull()?.let { return it }
            }
        }
        return 0.0
    }

    private fun Map<String, Any?>.longValue(vararg keys: String): Long {
        keys.forEach { key ->
            when (val value = this[key]) {
                is Number -> return value.toLong()
                is String -> value.toLongOrNull()?.let { return it }
            }
        }
        return 0L
    }
}
