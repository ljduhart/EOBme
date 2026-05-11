package com.eobme.app.data.repository

import com.eobme.app.data.local.dao.CptRecordDao
import com.eobme.app.data.local.dao.EobDao
import com.eobme.app.data.local.entity.CptRecordEntity
import com.eobme.app.data.local.entity.EobEntity
import com.eobme.app.ocr.EobOcrParser
import com.eobme.app.ocr.ParsedEobData
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class EobRepository(
    private val eobDao: EobDao,
    private val cptRecordDao: CptRecordDao
) {
    fun observeUserEobs(userId: Long): Flow<List<EobEntity>> = eobDao.observeByUser(userId)

    fun observeEob(eobId: Long): Flow<EobEntity?> = eobDao.observeById(eobId)

    suspend fun getEob(eobId: Long): EobEntity? = eobDao.getById(eobId)

    suspend fun saveEob(
        userId: Long,
        imageUri: String,
        rawOcrText: String,
        parsedData: ParsedEobData,
        uploadSource: String
    ): Long {
        val eob = EobEntity(
            userId = userId,
            imageUri = imageUri,
            dateOfService = parsedData.dateOfService,
            providerName = parsedData.providerName,
            insuranceName = parsedData.insuranceName,
            billedAmount = parsedData.billedAmount,
            insurancePaid = parsedData.insurancePaid,
            contractualAdjustment = parsedData.contractualAdjustment,
            copay = parsedData.copay,
            deductible = parsedData.deductible,
            coinsurance = parsedData.coinsurance,
            rawOcrText = rawOcrText,
            uploadSource = uploadSource
        )
        val eobId = eobDao.insert(eob)

        val calendar = Calendar.getInstance().apply { timeInMillis = parsedData.dateOfService }
        val year = calendar.get(Calendar.YEAR)

        val cptRecords = parsedData.cptCodes.mapNotNull { cptEntry ->
            val code = cptEntry.code
            if (!EobOcrParser.isValidCptCode(code)) return@mapNotNull null
            CptRecordEntity(
                eobId = eobId,
                userId = userId,
                code = code,
                description = cptEntry.description,
                category = cptEntry.category,
                dateOfService = parsedData.dateOfService,
                year = year
            )
        }
        if (cptRecords.isNotEmpty()) {
            cptRecordDao.insertAll(cptRecords)
        }
        return eobId
    }

    suspend fun updateEob(eob: EobEntity) {
        eobDao.update(eob)
    }

    suspend fun deleteEob(eobId: Long) {
        eobDao.deleteById(eobId)
    }
}
