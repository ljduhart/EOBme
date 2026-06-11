package app.eob.me.util

import java.io.File

object CacheSizeCalculator {
    fun directorySizeBytes(root: File): Long {
        if (!root.exists()) return 0L
        if (root.isFile) return root.length()
        return root.listFiles()?.sumOf { directorySizeBytes(it) } ?: 0L
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1_024) return "$bytes B"
        val kb = bytes / 1_024.0
        if (kb < 1_024) return String.format("%.1f KB", kb)
        val mb = kb / 1_024.0
        if (mb < 1_024) return String.format("%.1f MB", mb)
        val gb = mb / 1_024.0
        return String.format("%.2f GB", gb)
    }
}
