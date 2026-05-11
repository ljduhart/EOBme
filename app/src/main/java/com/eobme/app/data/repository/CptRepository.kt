package com.eobme.app.data.repository

import com.eobme.app.data.local.dao.CptRecordDao
import com.eobme.app.data.local.entity.CptRecordEntity
import kotlinx.coroutines.flow.Flow

class CptRepository(private val cptRecordDao: CptRecordDao) {

    fun observeByUserAndYear(userId: Long, year: Int): Flow<List<CptRecordEntity>> =
        cptRecordDao.observeByUserAndYear(userId, year)

    fun observeYears(userId: Long): Flow<List<Int>> =
        cptRecordDao.observeYears(userId)

    fun observeByCategory(userId: Long, category: String, year: Int): Flow<List<CptRecordEntity>> =
        cptRecordDao.observeByCategory(userId, category, year)

    fun observeAll(userId: Long): Flow<List<CptRecordEntity>> =
        cptRecordDao.observeByUser(userId)

    suspend fun getByEob(eobId: Long): List<CptRecordEntity> =
        cptRecordDao.getByEob(eobId)
}
