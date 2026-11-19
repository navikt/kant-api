package no.nav.klage.kant.domain.saksdata

import no.nav.klage.kant.domain.kvalitetsvurdering.Kvalitetsvurdering
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.Utfall
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class SaksdataRequest(
    val kabalReference: UUID,
    val sakstype: Type,
    val ytelse: Ytelse,
)

data class SaksdataUpdateRequest(
    val utfall: Utfall? = null,
    val registreringshjemler: Set<Registreringshjemmel> = emptySet(),
)

data class SaksdataAvslutningRequest(
    val utfall: Utfall,
    val registreringshjemler: Set<Registreringshjemmel>,
    val avsluttetAvSaksbehandler: LocalDateTime,
    val sakITrygderettenOpprettet: LocalDate? = null,
    val mottattKlageinstans: LocalDate? = null,
    val tilknyttetEnhet: String,
    val tilbakekreving: Boolean,
)

data class SaksdataResponse(
    val id: UUID,
    val kabalReference: UUID,
    val sakstype: Type,
    val ytelse: Ytelse,
    val mottattKlageinstans: LocalDate?,
    val sakITrygderettenOpprettet: LocalDate?,
    val tilknyttetEnhet: String?,
    val tilbakekreving: Boolean?,
    val utfall: Utfall?,
    val registreringshjemler: Set<Registreringshjemmel>,
    val avsluttetAvSaksbehandler: LocalDateTime?,
    val kvalitetsvurdering: Kvalitetsvurdering? = null,
)

fun Saksdata.toResponse(): SaksdataResponse =
    SaksdataResponse(
        id = id,
        kabalReference = kabalReference,
        sakstype = sakstype,
        ytelse = ytelse,
        mottattKlageinstans = mottattKlageinstans,
        sakITrygderettenOpprettet = sakITrygderettenOpprettet,
        tilknyttetEnhet = tilknyttetEnhet,
        tilbakekreving = tilbakekreving,
        utfall = utfall,
        registreringshjemler = registreringshjemler.orEmpty(),
        avsluttetAvSaksbehandler = avsluttetAvSaksbehandler,
        kvalitetsvurdering = kvalitetsvurdering,
    )
