package com.eobme.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InactivityTracker(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = 3 * 60 * 1000L,
    private val onTimeout: () -> Unit
) {
    private var timerJob: Job? = null

    fun onUserActivity() {
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(timeoutMillis)
            onTimeout()
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
    }

    fun start() {
        onUserActivity()
    }
}
