package app.eob.me.network

import com.google.firebase.functions.FirebaseFunctionsException

object VeryfiHybridStreamErrorMapper {
    fun describe(error: Throwable): String {
        if (error is FirebaseFunctionsException) {
            val code = error.code.name
            val details = error.details?.toString()?.takeIf { it.isNotBlank() }
            val message = error.message?.takeIf { it.isNotBlank() }
            val genericInternal = code.equals("INTERNAL", ignoreCase = true) && (
                message.isNullOrBlank() ||
                    message.equals("INTERNAL", ignoreCase = true) ||
                    message.equals("internal", ignoreCase = true) ||
                    message.equals("internal error", ignoreCase = true)
                )
            return buildString {
                append("Veryfi hybrid stream failed ($code)")
                when {
                    genericInternal -> append(
                        ": The Veryfi proxy could not complete extraction. " +
                            "Verify Firebase Functions secrets (VERYFI_CLIENT_ID, VERYFI_USERNAME, VERYFI_API_KEY) and deployment."
                    )
                    !message.isNullOrBlank() -> {
                        append(": ")
                        append(message)
                    }
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
