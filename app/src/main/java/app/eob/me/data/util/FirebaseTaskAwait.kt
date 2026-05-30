package app.eob.me.data.util

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspends until this Firebase [Task] completes. Use from [kotlinx.coroutines] scopes only.
 */
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isCancelled) return@addOnCompleteListener
        if (task.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            continuation.resume(task.result as T)
        } else {
            continuation.resumeWithException(
                task.exception ?: IllegalStateException("Firebase task failed without an exception.")
            )
        }
    }
}

suspend fun Task<Void>.awaitUnit(): Unit = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isCancelled) return@addOnCompleteListener
        if (task.isSuccessful) {
            continuation.resume(Unit)
        } else {
            continuation.resumeWithException(
                task.exception ?: IllegalStateException("Firebase task failed without an exception.")
            )
        }
    }
}
