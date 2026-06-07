package app.eob.me.data


/**
 * Derives care-team card display models and provider-directory assurance from hub data.
 */
object CareTeamStateExtractor {
    fun buildCareTeamCards(
        language: AppLanguage,
        preferredDoctors: Map<CareTeamProviderType, PreferredDoctor>,
        appointments: List<DoctorAppointment>,
        records: List<EobRecord>,
        invoiceProcessing: Boolean
    ): List<CareTeamCardDisplayState> {
        return CareTeamProviderType.displayOrder.map { type ->
            val doctor = preferredDoctors[type] ?: PreferredDoctor(type = type)
            val relatedRecords = recordsForCareTeam(doctor, records)
            val metrics = CareTeamMicroMetrics(
                relatedEobCount = relatedRecords.size,
                upcomingAppointments = appointments.count {
                    it.providerType == type && it.providerName.isNotBlank()
                },
                flaggedIssueCount = relatedRecords.count { record ->
                    EobAnalyzer.detectBillingIssues(record).any { issue ->
                        issue.severity != BillingIssueSeverity.Info
                    }
                }
            )
            val assurance = assuranceForCard(
                doctor = doctor,
                relatedRecords = relatedRecords,
                invoiceProcessing = invoiceProcessing
            )
            cardDisplayForType(
                language = language,
                type = type,
                doctor = doctor,
                metrics = metrics,
                assurance = assurance,
                relatedRecords = relatedRecords
            )
        }
    }

    fun buildProviderDirectoryAssurance(
        language: AppLanguage,
        preferredDoctors: Map<CareTeamProviderType, PreferredDoctor>,
        records: List<EobRecord>,
        invoiceProcessing: Boolean
    ): ProviderDirectoryAssurance {
        val state = when {
            invoiceProcessing -> NetworkAssuranceState.VerificationPending
            records.any { EobAnalyzer.recordSignalsOutOfNetwork(it) } &&
                directoryNeedsUpdate(preferredDoctors, records) ->
                NetworkAssuranceState.OutOfNetworkAlert
            records.isNotEmpty() -> NetworkAssuranceState.FullyAssured
            else -> NetworkAssuranceState.VerificationPending
        }
        val labelKey = when (state) {
            NetworkAssuranceState.FullyAssured -> "networkAssuranceFullyAssured"
            NetworkAssuranceState.VerificationPending -> "networkAssurancePending"
            NetworkAssuranceState.OutOfNetworkAlert -> "networkAssuranceOutOfNetwork"
        }
        return ProviderDirectoryAssurance(
            state = state,
            statusLabel = EobStrings.t(language, labelKey),
            showWarningDot = state == NetworkAssuranceState.OutOfNetworkAlert
        )
    }

    private fun directoryNeedsUpdate(
        preferredDoctors: Map<CareTeamProviderType, PreferredDoctor>,
        records: List<EobRecord>
    ): Boolean {
        val latestOon = records
            .filter { EobAnalyzer.recordSignalsOutOfNetwork(it) }
            .maxByOrNull { it.serviceDateSortKey }
            ?: return false
        return preferredDoctors.values.none { doctor ->
            doctor.isAssigned && EobAnalyzer.providerNameMatchesCareTeam(latestOon.providerName, doctor.name)
        }
    }

    private fun recordsForCareTeam(doctor: PreferredDoctor, records: List<EobRecord>): List<EobRecord> {
        if (!doctor.isAssigned) return emptyList()
        return records.filter { EobAnalyzer.providerNameMatchesCareTeam(it.providerName, doctor.name) }
    }

    private fun assuranceForCard(
        doctor: PreferredDoctor,
        relatedRecords: List<EobRecord>,
        invoiceProcessing: Boolean
    ): NetworkAssuranceState {
        if (invoiceProcessing) return NetworkAssuranceState.VerificationPending
        if (!doctor.isAssigned) return NetworkAssuranceState.VerificationPending
        val latest = relatedRecords.maxByOrNull { it.serviceDateSortKey }
        if (latest != null && EobAnalyzer.recordSignalsOutOfNetwork(latest)) {
            return NetworkAssuranceState.OutOfNetworkAlert
        }
        if (relatedRecords.isNotEmpty()) return NetworkAssuranceState.FullyAssured
        return NetworkAssuranceState.VerificationPending
    }

    private fun cardDisplayForType(
        language: AppLanguage,
        type: CareTeamProviderType,
        doctor: PreferredDoctor,
        metrics: CareTeamMicroMetrics,
        assurance: NetworkAssuranceState,
        relatedRecords: List<EobRecord>
    ): CareTeamCardDisplayState {
        val phoneUri = doctor.phone.takeIf { it.isNotBlank() }?.let { dialUri(it) }
        return when (type) {
            CareTeamProviderType.Pcp, CareTeamProviderType.Dentist -> {
                val primary = if (doctor.isAssigned) {
                    doctor.name
                } else {
                    EobStrings.t(language, "careTeamTapToEdit")
                }
                val secondary = when {
                    !doctor.isAssigned -> EobStrings.t(language, "careTeamUnassignedHint")
                    phoneUri != null -> EobStrings.t(language, "careTeamTapToCall")
                    else -> EobStrings.t(language, "careTeamAddPhone")
                }
                CareTeamCardDisplayState(
                    type = type,
                    isAssigned = doctor.isAssigned,
                    assuranceState = assurance,
                    metrics = metrics,
                    primaryLine = primary,
                    secondaryLine = secondary,
                    phoneDialUri = phoneUri
                )
            }
            CareTeamProviderType.Specialist -> {
                val referralActive = doctor.isAssigned &&
                    (doctor.specialty.isNotBlank() || relatedRecords.isNotEmpty())
                val primary = if (doctor.isAssigned) {
                    doctor.name
                } else {
                    EobStrings.t(language, "careTeamTapToEdit")
                }
                val refLabel = if (referralActive) {
                    EobStrings.t(language, "careTeamSpecialistRefActive")
                } else {
                    EobStrings.t(language, "careTeamSpecialistRefInactive")
                }
                val specialtyLine = when {
                    doctor.specialty.isNotBlank() -> doctor.specialty
                    doctor.isAssigned -> EobStrings.t(language, "careTeamSpecialtyPending")
                    else -> EobStrings.t(language, "careTeamUnassignedHint")
                }
                CareTeamCardDisplayState(
                    type = type,
                    isAssigned = doctor.isAssigned,
                    assuranceState = assurance,
                    metrics = metrics,
                    primaryLine = primary,
                    secondaryLine = refLabel,
                    tertiaryLine = specialtyLine,
                    specialistReferralActive = referralActive,
                    phoneDialUri = phoneUri
                )
            }
            CareTeamProviderType.Therapist -> {
                val networkStatus = therapistNetworkStatus(relatedRecords)
                val copayAmount = therapistCopayAmount(relatedRecords)
                val primary = if (doctor.isAssigned) {
                    doctor.name
                } else {
                    EobStrings.t(language, "careTeamTapToEdit")
                }
                val secondary = if (copayAmount != null) {
                    EobStrings.tf(language, "careTeamTherapistCopay", copayAmount.asCurrency())
                } else {
                    EobStrings.t(language, "careTeamTherapistCopayPending")
                }
                val tertiary = when (networkStatus) {
                    TherapistNetworkStatus.InNetwork -> EobStrings.t(language, "careTeamTherapistInNetwork")
                    TherapistNetworkStatus.OutOfNetwork -> EobStrings.t(language, "careTeamTherapistOutOfNetwork")
                    TherapistNetworkStatus.Unknown -> EobStrings.t(language, "careTeamTherapistNetworkUnknown")
                }
                CareTeamCardDisplayState(
                    type = type,
                    isAssigned = doctor.isAssigned,
                    assuranceState = assurance,
                    metrics = metrics,
                    primaryLine = primary,
                    secondaryLine = secondary,
                    tertiaryLine = tertiary,
                    therapistNetworkStatus = networkStatus,
                    therapistCopayAmount = copayAmount,
                    phoneDialUri = phoneUri
                )
            }
        }
    }

    private fun therapistCopayAmount(records: List<EobRecord>): Double? {
        if (records.isEmpty()) return null
        val latest = records.maxByOrNull { it.serviceDateSortKey } ?: return null
        return latest.totalCopayAmount.takeIf { it > 0.0 }
            ?: latest.charges.map { it.copayAmount }.filter { it > 0.0 }.maxOrNull()
    }

    private fun therapistNetworkStatus(records: List<EobRecord>): TherapistNetworkStatus {
        if (records.isEmpty()) return TherapistNetworkStatus.Unknown
        val latest = records.maxByOrNull { it.serviceDateSortKey } ?: return TherapistNetworkStatus.Unknown
        return if (EobAnalyzer.recordSignalsOutOfNetwork(latest)) {
            TherapistNetworkStatus.OutOfNetwork
        } else {
            TherapistNetworkStatus.InNetwork
        }
    }

    private fun dialUri(phone: String): String {
        val digits = phone.filter { it.isDigit() || it == '+' }
        return if (digits.isNotBlank()) "tel:$digits" else "tel:${phone.trim()}"
    }
}
