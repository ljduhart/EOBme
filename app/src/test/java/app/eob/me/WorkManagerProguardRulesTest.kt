package app.eob.me

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards release R8 rules required for WorkManager's internal Room database
 * (WorkDatabase_Impl no-arg constructor via reflection).
 */
class WorkManagerProguardRulesTest {

    @Test
    fun proguardRulesKeepWorkManagerImplAndRoomDatabase() {
        val rules = readProguardRules()
        listOf(
            "androidx.work.impl.**",
            "androidx.work.WorkerParameters",
            "androidx.room.RoomDatabase_Impl",
            "androidx.room.RoomDatabase",
            "androidx.sqlite.db.**",
            "**_Impl",
            "FsaDoomsdayNotificationWorker"
        ).forEach { snippet ->
            assertTrue(
                "proguard-rules.pro must keep WorkManager/Room symbol: $snippet",
                rules.contains(snippet)
            )
        }
    }

    @Test
    fun proguardRulesKeepListenableWorkerConstructors() {
        val rules = readProguardRules()
        assertTrue(rules.contains("extends androidx.work.ListenableWorker"))
        assertTrue(rules.contains("public <init>(...);"))
    }

    private fun readProguardRules(): String {
        val candidates = listOf(
            File("proguard-rules.pro"),
            File("app/proguard-rules.pro")
        )
        return candidates.first { it.isFile }.readText()
    }
}
