package com.eobme.app

import android.app.Application
import com.eobme.app.data.local.AppDatabase
import com.eobme.app.data.preferences.UserPreferences
import com.eobme.app.data.repository.CptRepository
import com.eobme.app.data.repository.EobRepository
import com.eobme.app.data.repository.UserRepository

class EOBmeApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var userPreferences: UserPreferences
        private set
    lateinit var userRepository: UserRepository
        private set
    lateinit var eobRepository: EobRepository
        private set
    lateinit var cptRepository: CptRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        userPreferences = UserPreferences(this)
        userRepository = UserRepository(database.userDao())
        eobRepository = EobRepository(database.eobDao(), database.cptRecordDao())
        cptRepository = CptRepository(database.cptRecordDao())
    }
}
