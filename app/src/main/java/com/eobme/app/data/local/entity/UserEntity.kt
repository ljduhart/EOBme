package com.eobme.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val email: String,
    val passwordHash: String,
    val city: String = "",
    val state: String = "",
    val language: String = "en",
    val subscriberId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
