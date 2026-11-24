package no.nav.klage.kant.domain.kvalitetsvurdering

import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel

data class AnkeITRKvalitetsvurderingV1(
    val omgjortIAnkeomgangen: OmgjortIAnkeomgangen = OmgjortIAnkeomgangen(),
    val rettensSammensetning: RettensSammensetning = RettensSammensetning(),
    val kjennelsensBetydningOgLaeringsverdi: KjennelsensBetydningOgLaeringsverdi =
        KjennelsensBetydningOgLaeringsverdi(),
) : Kvalitetsvurdering {
    data class OmgjortIAnkeomgangen(
        val vedtaketBurdeVaertOmgjortIAnkeomgangen: Boolean? = null,
        val feilILovtolkningen: SelectorWithRegistreringshjemler = SelectorWithRegistreringshjemler(),
        val feilIFaktagrunnlaget: SelectorWithRegistreringshjemler = SelectorWithRegistreringshjemler(),
        val feilIRettsanvendelsen: SelectorWithRegistreringshjemler = SelectorWithRegistreringshjemler(),
    ) {
        data class SelectorWithRegistreringshjemler(
            val selected: Boolean = false,
            val registreringshjemler: List<Registreringshjemmel> = emptyList(),
        )
    }

    data class RettensSammensetning(
        val rettensSammensetning: RettensSammensetningEnum? = null,
        val attfoeringskyndigEllerMedisinskKyndig: AttfoeringskyndigEllerMedisinskKyndigEnum? = null,
    ) {
        enum class RettensSammensetningEnum {
            ORDINAER_RETT,
            TRE_JURIDISKE_MEDLEMMER,
            FEM_MEDLEMMER,
        }

        enum class AttfoeringskyndigEllerMedisinskKyndigEnum {
            NEI,
            JA_ATTFOERINGSKYNDIG_MEDLEM,
            JA_MEDISINSK_KYNDIG_MEDLEM,
            JA_BAADE_ATTFORINGSKYNDIG_OG_MEDISINSK_KYNDIG_MEDLEM,
        }
    }

    data class KjennelsensBetydningOgLaeringsverdi(
        val avsagtMedForenkletBegrunnelse: Boolean? = null,
        val prinsipiellBetydning: Boolean? = null,
        val bryterMedEtablertPraksisINavEllerTR: Boolean? = null,
        val betydningForFlereFagomraader: Boolean? = null,
        val inneholderLaeringspunkterKnyttetTilGenerellForvaltningsrettEllerGenerellJuss: Boolean? = null,
        val omhandlerEOESRettEllerAnnetInternasjonaltRegelverk: Boolean? = null,
        val boerDelesMedFagmiljoetSomLaeringseksempel: Boolean? = null,
    )
}
