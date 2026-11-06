package no.nav.klage.kant.saksdata

import no.nav.klage.kodeverk.Type
import java.util.UUID

data class SaksdataRequest(
    val kabalReference: UUID,
    val sakstype: Type,
) {
    override fun toString(): String = "SaksdataRequest"
}

data class SaksdataResponse(
    val id: UUID,
    val kabalReference: UUID,
    val sakstype: Type,
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
    )
