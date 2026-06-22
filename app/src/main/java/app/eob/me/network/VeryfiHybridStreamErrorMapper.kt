package app.eob.me.network

import com.google.firebase.functions.FirebaseFunctionsException

object VeryfiHybridStreamErrorMapper {
    fun describe(error: Throwable): String {
        if (error is FirebaseFunctionsException) {
            val code = error.code.name
            val details = error.details?.toString()?.takeIf { it.isNotBlank() }
            val message = error.message?.takeIf { it.isNotBlank() }
            return buildString {
                append("Veryfi hybrid stream failed ($code)")
                if (!message.isNullOrBlank()) {
                    append(": ")
                    append(message)
                }
                if (!details.isNullOrBlank()) {
                    append(" — ")
                    append(details)
                }
            }
        }

        val message = error.message?.takeIf { it.isNotBlank() }
        if (!message.isNullOrBlank()) return message
        return error::class.java.simpleName.ifBlank { "Unknown Veryfi extraction error" }
    }
}
