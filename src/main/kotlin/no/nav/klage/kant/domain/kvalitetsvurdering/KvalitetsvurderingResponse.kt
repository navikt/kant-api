package no.nav.klage.kant.domain.kvalitetsvurdering

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.klage.kant.domain.saksdata.Saksdata
import java.time.LocalDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = AnkeKvalitetsvurderingV1Response::class, name = "ANKE_V1"),
    JsonSubTypes.Type(value = AnkeITRKvalitetsvurderingV1Response::class, name = "ANKE_ITR_V1"),
)
sealed interface KvalitetsvurderingResponse

data class AnkeKvalitetsvurderingV1Response(
    val omgjortIKlageomgangen: AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen,
    val saksbehandlingsfeilIKlageomgangen: AnkeKvalitetsvurderingV1.SaksbehandlingsfeilIKlageomgangen,
    val created: LocalDateTime,
    val modified: LocalDateTime,
) : KvalitetsvurderingResponse

data class AnkeITRKvalitetsvurderingV1Response(
    val omgjortIAnkeomgangen: AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen,
    val rettensSammensetning: AnkeITRKvalitetsvurderingV1.RettensSammensetning,
    val kjennelsensBetydningOgLaeringsverdi: AnkeITRKvalitetsvurderingV1.KjennelsensBetydningOgLaeringsverdi,
    val created: LocalDateTime,
    val modified: LocalDateTime,
) : KvalitetsvurderingResponse

fun Saksdata.toKvalitetsvurderingResponse(): KvalitetsvurderingResponse? =
    when (val assessment = kvalitetsvurdering) {
        is AnkeKvalitetsvurderingV1 -> {
            AnkeKvalitetsvurderingV1Response(
                omgjortIKlageomgangen = assessment.omgjortIKlageomgangen,
                saksbehandlingsfeilIKlageomgangen = assessment.saksbehandlingsfeilIKlageomgangen,
                created = created,
                modified = modified,
            )
        }

        is AnkeITRKvalitetsvurderingV1 -> {
            AnkeITRKvalitetsvurderingV1Response(
                omgjortIAnkeomgangen = assessment.omgjortIAnkeomgangen,
                rettensSammensetning = assessment.rettensSammensetning,
                kjennelsensBetydningOgLaeringsverdi = assessment.kjennelsensBetydningOgLaeringsverdi,
                created = created,
                modified = modified,
            )
        }

        null -> {
            null
        }
    }
