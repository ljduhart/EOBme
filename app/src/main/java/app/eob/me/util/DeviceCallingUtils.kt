package app.eob.me.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Safety barrier for care-team dial actions. Uses [Intent.ACTION_DIAL] only — no
 * [Intent.ACTION_CALL], no manifest permissions, and no runtime permission prompts.
 */
object DeviceCallingUtils {
    fun filterPhoneInput(input: String): String {
        return buildString(input.length) {
            input.forEach { char ->
                if (char.isDigit() || char == '(' || char == ')' || char == '-') {
                    append(char)
                }
            }
        }
    }

    fun extractPhoneDigits(input: String): String {
        return input.filter { it.isDigit() }.take(10)
    }

    fun formatPhoneForDisplay(input: String): String {
        val digits = extractPhoneDigits(input)
        if (digits.isEmpty()) return ""
        return when (digits.length) {
            in 1..3 -> "(${digits}"
            in 4..6 -> "(${digits.take(3)}) ${digits.drop(3)}"
            else -> "(${digits.take(3)}) ${digits.drop(3).take(3)}-${digits.drop(6)}"
        }
    }

    fun applyPhoneInputChange(rawInput: String): String {
        return formatPhoneForDisplay(extractPhoneDigits(rawInput))
    }

    fun dialUriFor(phone: String): String? {
        val digits = phone.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return "tel:$digits"
    }

    fun hasDialablePhone(phone: String): Boolean = dialUriFor(phone) != null

    enum class DialOutcome {
        Dialed,
        NoTelephony,
        NoDialer
    }

    fun safelyDialNumber(context: Context, phoneNumber: String): DialOutcome {
        val uriString = dialUriFor(phoneNumber) ?: return DialOutcome.NoTelephony
        val packageManager = context.packageManager
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return DialOutcome.NoTelephony
        }
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse(uriString))
        if (dialIntent.resolveActivity(packageManager) == null) {
            return DialOutcome.NoDialer
        }
        context.startActivity(dialIntent)
        return DialOutcome.Dialed
    }
}
