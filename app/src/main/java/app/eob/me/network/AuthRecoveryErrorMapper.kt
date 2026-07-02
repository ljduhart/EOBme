package app.eob.me.network

import com.google.firebase.functions.FirebaseFunctionsException

internal object AuthRecoveryErrorMapper {
    fun describe(error: Throwable): String {
        if (error is FirebaseFunctionsException) {
            return when (error.code) {
                FirebaseFunctionsException.Code.INVALID_ARGUMENT -> error.message.orEmpty()
                FirebaseFunctionsException.Code.NOT_FOUND -> error.message.orEmpty()
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> error.message.orEmpty()
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> error.message.orEmpty()
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> error.message.orEmpty()
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> error.message.orEmpty()
                FirebaseFunctionsException.Code.UNAVAILABLE -> "Could not reach the authentication service. Please try again."
                else -> error.message.orEmpty()
            }.ifBlank { "Authentication request failed." }
        }
        return error.localizedMessage.orEmpty().ifBlank { "Authentication request failed." }
    }
}
