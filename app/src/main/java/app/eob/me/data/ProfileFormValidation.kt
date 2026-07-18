package app.eob.me.data

data class ProfileFieldErrors(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val city: String? = null,
    val state: String? = null,
    val insuranceName: String? = null,
    val insuranceId: String? = null,
    val groupName: String? = null,
    val pcpCopay: String? = null,
    val specialistCopay: String? = null,
    val annualDeductibleLimit: String? = null,
    val annualOutOfPocketMax: String? = null,
    val hsaAllocation: String? = null,
    val fsaAllocation: String? = null,
    val password: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(
            firstName,
            lastName,
            email,
            city,
            state,
            insuranceName,
            insuranceId,
            groupName,
            pcpCopay,
            specialistCopay,
            annualDeductibleLimit,
            annualOutOfPocketMax,
            hsaAllocation,
            fsaAllocation,
            password
        ).any { it != null }
}

object ProfileFormValidator {
    private val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

    fun validate(
        language: AppLanguage,
        profile: UserProfile,
        credentials: RegistrationCredentials
    ): ProfileFieldErrors {
        fun required(value: String, key: String): String? =
            if (value.isBlank()) EobStrings.t(language, key) else null

        fun optionalCurrency(value: String, key: String): String? {
            if (value.isBlank()) return null
            return if (parseCurrencyInput(value) != null) {
                null
            } else {
                EobStrings.t(language, key)
            }
        }

        val email = credentials.email.ifBlank { profile.email }
        val emailError = when {
            email.isBlank() -> EobStrings.t(language, "profileErrorEmailRequired")
            !emailPattern.matches(email.trim()) -> EobStrings.t(language, "profileErrorEmailInvalid")
            else -> null
        }

        val stateError = when {
            profile.state.isBlank() -> EobStrings.t(language, "profileErrorStateRequired")
            profile.state.trim().length < 2 -> EobStrings.t(language, "profileErrorStateInvalid")
            else -> null
        }

        val passwordError = when {
            credentials.password.isBlank() -> null
            credentials.isPasswordValid -> null
            else -> EobStrings.t(language, "passwordRule")
        }

        val deductibleError = when {
            profile.annualDeductibleLimit == 0.0 -> null
            profile.annualDeductibleLimit < 0.0 -> EobStrings.t(language, "profileErrorAmountInvalid")
            else -> null
        }

        val outOfPocketError = when {
            profile.annualOutOfPocketMax == 0.0 -> null
            profile.annualOutOfPocketMax < 0.0 -> EobStrings.t(language, "profileErrorAmountInvalid")
            else -> null
        }

        val hsaError = when {
            profile.hsaAllocation == 0.0 -> null
            profile.hsaAllocation < 0.0 -> EobStrings.t(language, "profileErrorAmountInvalid")
            else -> null
        }

        val fsaError = when {
            profile.fsaAllocation == 0.0 -> null
            profile.fsaAllocation < 0.0 -> EobStrings.t(language, "profileErrorAmountInvalid")
            else -> null
        }

        return ProfileFieldErrors(
            firstName = required(profile.firstName, "profileErrorFirstNameRequired"),
            lastName = required(profile.lastName, "profileErrorLastNameRequired"),
            email = emailError,
            city = required(profile.city, "profileErrorCityRequired"),
            state = stateError,
            insuranceName = null,
            insuranceId = null,
            groupName = null,
            pcpCopay = optionalCurrency(profile.pcpCopay, "profileErrorCopayInvalid"),
            specialistCopay = optionalCurrency(profile.specialistCopay, "profileErrorCopayInvalid"),
            annualDeductibleLimit = deductibleError,
            annualOutOfPocketMax = outOfPocketError,
            hsaAllocation = hsaError,
            fsaAllocation = fsaError,
            password = passwordError
        )
    }

    private fun parseCurrencyInput(raw: String): Double? {
        val normalized = raw.trim().removePrefix("$").replace(",", "")
        if (normalized.isBlank()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value >= 0.0) value else null
    }
}
