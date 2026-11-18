package no.nav.klage.kant.domain.kvalitetsvurdering

import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel

data class AnkeKvalitetsvurderingV1(
    val omgjortIKlageomgangen: OmgjortIKlageomgangen = OmgjortIKlageomgangen(),
    val saksbehandlingsfeilIKlageomgangen: SaksbehandlingsfeilIKlageomgangen = SaksbehandlingsfeilIKlageomgangen(),
) : Kvalitetsvurdering {
    data class OmgjortIKlageomgangen(
        val vedtaketBurdeVaertOmgjortIKlageomgangen: Boolean? = null,
        val feilILovtolkningen: SelectorWithRegistreringshjemler = SelectorWithRegistreringshjemler(),
        val feilIFaktagrunnlaget: SelectorWithRegistreringshjemler = SelectorWithRegistreringshjemler(),
        val feilIRettsanvendelsen: SelectorWithRegistreringshjemler = SelectorWithRegistreringshjemler(),
    ) {
        data class SelectorWithRegistreringshjemler(
            val selected: Boolean = false,
            val registreringshjemler: List<Registreringshjemmel> = emptyList(),
        )
    }

    data class SaksbehandlingsfeilIKlageomgangen(
        val skulleHaRyddetIDokumenterEllerJournalfoertOpplysninger: Boolean = false,
        val skulleHaVeiledetPartenBedre: Boolean = false,
        val skulleHaForhaandsvarsletParten: Boolean = false,
        val skulleHaForelagtOpplysningerForParten: Boolean = false,
        val skulleHaUtredetSakenBedre: Boolean = false,
        val klagevedtaketSkulleVaertBedreBegrunnet: Boolean = false,
        val skulleHaBenyttetROL: Boolean = false,
    )
}
