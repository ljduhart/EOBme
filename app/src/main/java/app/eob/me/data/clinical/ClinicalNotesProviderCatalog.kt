package app.eob.me.data.clinical

import app.eob.me.data.AppLanguage
import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.EobStrings
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.ProviderSummary
import app.eob.me.data.local.entity.ProviderDirectoryEntity
import app.eob.me.ui.components.home.careTeamLabel

object ClinicalNotesProviderCatalog {
    private const val EOB_PROVIDER_ID_BASE = 1000

    fun buildDirectoryEntries(
        language: AppLanguage,
        preferredDoctors: Map<CareTeamProviderType, PreferredDoctor>,
        providerSummaries: List<ProviderSummary>
    ): List<ProviderDirectoryEntity> {
        val careTeam = CareTeamProviderType.displayOrder.map { type ->
            val doctor = preferredDoctors[type]
            val roleLabel = careTeamLabel(language, type)
            val displayName = doctor?.name?.trim().orEmpty().ifBlank {
                EobStrings.t(language, "careTeamTapToEdit")
            }
            ProviderDirectoryEntity(
                providerId = careTeamProviderId(type),
                displayName = displayName,
                roleLabel = roleLabel
            )
        }
        val fromEobs = providerSummaries.mapIndexed { index, summary ->
            ProviderDirectoryEntity(
                providerId = EOB_PROVIDER_ID_BASE + index + 1,
                displayName = summary.providerName.trim(),
                roleLabel = EobStrings.t(language, "provider")
            )
        }
        return (careTeam + fromEobs).distinctBy { it.providerId }
    }

    fun toOptions(entries: List<ProviderDirectoryEntity>): List<app.eob.me.data.ClinicalProviderOption> {
        return entries.map { entry ->
            app.eob.me.data.ClinicalProviderOption(
                providerId = entry.providerId,
                displayLabel = "${entry.displayName} | ${entry.roleLabel}"
            )
        }
    }

    fun careTeamProviderId(type: CareTeamProviderType): Int = type.ordinal + 1
}
