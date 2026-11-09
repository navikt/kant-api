package no.nav.klage.kant.saksdata

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.Utfall
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import java.time.LocalDateTime
import java.util.UUID

data class SaksdataRequest(
    val kabalReference: UUID,
    val sakstype: Type,
    val ytelse: Ytelse,
    val utfall: Utfall? = null,
    @param:JsonSetter(contentNulls = Nulls.FAIL)
    val registreringshjemler: Set<Registreringshjemmel>? = null,
    val avsluttetAvSaksbehandler: LocalDateTime? = null,
) {
    override fun toString(): String = "SaksdataRequest"
}

data class SaksdataResponse(
    val id: UUID,
    val kabalReference: UUID,
    val sakstype: Type,
    val ytelse: Ytelse,
    val utfall: Utfall?,
    val registreringshjemler: Set<Registreringshjemmel>,
    val avsluttetAvSaksbehandler: LocalDateTime?,
) {
    override fun toString(): String = "SaksdataResponse"
}

data class SaksdataPage(
    val content: List<SaksdataResponse>,
    val page: PageMetadata,
)

data class PageMetadata(
    val size: Int,
    val number: Int,
    val totalElements: Long,
    val totalPages: Int,
)

fun Saksdata.toResponse(): SaksdataResponse =
    SaksdataResponse(
        id = id,
        kabalReference = kabalReference,
        sakstype = sakstype,
        ytelse = ytelse,
        utfall = utfall,
        registreringshjemler = registreringshjemler.orEmpty(),
        avsluttetAvSaksbehandler = avsluttetAvSaksbehandler,
    )
