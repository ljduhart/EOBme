package app.eob.me.network

import com.google.firebase.functions.FirebaseFunctions
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AuthRecoveryClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {
    suspend fun sendForgotUsernameReminder(email: String): String {
        return callFunction(
            functionName = SEND_FORGOT_USERNAME_REMINDER,
            payload = mapOf("email" to email.trim())
        )
    }

    suspend fun requestPasswordResetCode(email: String): String {
        return callFunction(
            functionName = REQUEST_PASSWORD_RESET_CODE,
            payload = mapOf("email" to email.trim())
        )
    }

    suspend fun confirmPasswordResetCode(
        email: String,
        code: String,
        newPassword: String
    ): String {
        return callFunction(
            functionName = CONFIRM_PASSWORD_RESET_CODE,
            payload = mapOf(
                "email" to email.trim(),
                "code" to code.trim(),
                "newPassword" to newPassword
            )
        )
    }

    private suspend fun callFunction(
        functionName: String,
        payload: Map<String, String>
    ): String = suspendCancellableCoroutine { continuation ->
        val callable = functions.getHttpsCallable(functionName)
            .withTimeout(CALLABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val task = callable.call(payload)
        task.addOnSuccessListener { result ->
            if (!continuation.isActive) return@addOnSuccessListener
            val data = result.data as? Map<*, *>
            val message = data?.get("message")?.toString().orEmpty()
            continuation.resume(message)
        }
        task.addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(
                    IllegalStateException(AuthRecoveryErrorMapper.describe(error), error)
                )
            }
        }
    }

    companion object {
        private const val SEND_FORGOT_USERNAME_REMINDER = "sendForgotUsernameReminder"
        private const val REQUEST_PASSWORD_RESET_CODE = "requestPasswordResetCode"
        private const val CONFIRM_PASSWORD_RESET_CODE = "confirmPasswordResetCode"
        private const val CALLABLE_TIMEOUT_SECONDS = 30L
    }
}
