package com.eobme.app.data.repository

import com.eobme.app.data.local.dao.UserDao
import com.eobme.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class UserRepository(private val userDao: UserDao) {

    fun observeUser(userId: Long): Flow<UserEntity?> = userDao.observeUser(userId)

    suspend fun getUser(userId: Long): UserEntity? = userDao.getById(userId)

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        language: String
    ): Result<Long> {
        val existing = userDao.findByEmail(email)
        if (existing != null) return Result.failure(Exception("Email already registered"))

        val user = UserEntity(
            firstName = firstName,
            lastName = lastName,
            email = email,
            passwordHash = hashPassword(password),
            language = language
        )
        val id = userDao.insert(user)
        return Result.success(id)
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val user = userDao.login(email, hashPassword(password))
            ?: return Result.failure(Exception("Invalid email or password"))
        return Result.success(user)
    }

    suspend fun updateProfile(user: UserEntity) {
        userDao.update(user)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
