package no.nav.klage.kant.domain.kvalitetsvurdering

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel

object KvalitetsvurderingValidator {
    fun validate(
        sakstype: Type,
        kvalitetsvurdering: Kvalitetsvurdering?,
        registreringshjemler: Set<Registreringshjemmel>,
    ) {
        val errors =
            buildList<KvalitetsvurderingValidationError> {
                when (kvalitetsvurdering) {
                    is AnkeKvalitetsvurderingV1 -> {
                        if (sakstype == Type.ANKE_ETTER_2027) {
                            validateAnke(assessment = kvalitetsvurdering, allowedHjemler = registreringshjemler)
                        } else {
                            add(typeMismatch())
                        }
                    }

                    is AnkeITRKvalitetsvurderingV1 -> {
                        if (sakstype == Type.ANKE_I_TRYGDERETTEN_ETTER_2027) {
                            validateAnkeITR(assessment = kvalitetsvurdering, allowedHjemler = registreringshjemler)
                        } else {
                            add(typeMismatch())
                        }
                    }

                    null -> {
                        add(
                            KvalitetsvurderingValidationError(
                                field = "kvalitetsvurdering",
                                message = "Må fylles ut før fullføring",
                            ),
                        )
                    }
                }
            }
        if (errors.isNotEmpty()) throw KvalitetsvurderingValidationException(errors)
    }

    private fun MutableList<KvalitetsvurderingValidationError>.validateAnke(
        assessment: AnkeKvalitetsvurderingV1,
        allowedHjemler: Set<Registreringshjemmel>,
    ) {
        val omgjort = assessment.omgjortIKlageomgangen
        val path = "kvalitetsvurdering.omgjortIKlageomgangen"
        validateOmgjort(
            field = "$path.vedtaketBurdeVaertOmgjortIKlageomgangen",
            answer = omgjort.vedtaketBurdeVaertOmgjortIKlageomgangen,
            selected =
                listOf(
                    omgjort.feilILovtolkningen.selected,
                    omgjort.feilIFaktagrunnlaget.selected,
                    omgjort.feilIRettsanvendelsen.selected,
                ),
        )
        validateSelector(
            field = "$path.feilILovtolkningen.registreringshjemler",
            selected = omgjort.feilILovtolkningen.selected,
            hjemler = omgjort.feilILovtolkningen.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        validateSelector(
            field = "$path.feilIFaktagrunnlaget.registreringshjemler",
            selected = omgjort.feilIFaktagrunnlaget.selected,
            hjemler = omgjort.feilIFaktagrunnlaget.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        validateSelector(
            field = "$path.feilIRettsanvendelsen.registreringshjemler",
            selected = omgjort.feilIRettsanvendelsen.selected,
            hjemler = omgjort.feilIRettsanvendelsen.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
    }

    private fun MutableList<KvalitetsvurderingValidationError>.validateAnkeITR(
        assessment: AnkeITRKvalitetsvurderingV1,
        allowedHjemler: Set<Registreringshjemmel>,
    ) {
        val omgjort = assessment.omgjortIAnkeomgangen
        val path = "kvalitetsvurdering.omgjortIAnkeomgangen"
        validateOmgjort(
            field = "$path.vedtaketBurdeVaertOmgjortIAnkeomgangen",
            answer = omgjort.vedtaketBurdeVaertOmgjortIAnkeomgangen,
            selected =
                listOf(
                    omgjort.feilILovtolkningen.selected,
                    omgjort.feilIFaktagrunnlaget.selected,
                    omgjort.feilIRettsanvendelsen.selected,
                ),
        )
        validateSelector(
            field = "$path.feilILovtolkningen.registreringshjemler",
            selected = omgjort.feilILovtolkningen.selected,
            hjemler = omgjort.feilILovtolkningen.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        validateSelector(
            field = "$path.feilIFaktagrunnlaget.registreringshjemler",
            selected = omgjort.feilIFaktagrunnlaget.selected,
            hjemler = omgjort.feilIFaktagrunnlaget.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        validateSelector(
            field = "$path.feilIRettsanvendelsen.registreringshjemler",
            selected = omgjort.feilIRettsanvendelsen.selected,
            hjemler = omgjort.feilIRettsanvendelsen.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        val sammensetning = assessment.rettensSammensetning
        requireAnswer(
            field = "kvalitetsvurdering.rettensSammensetning.rettensSammensetning",
            value = sammensetning.rettensSammensetning,
        )
        requireAnswer(
            field = "kvalitetsvurdering.rettensSammensetning.attfoeringskyndigEllerMedisinskKyndig",
            value = sammensetning.attfoeringskyndigEllerMedisinskKyndig,
        )
        val betydning = assessment.kjennelsensBetydningOgLaeringsverdi
        val betydningPath = "kvalitetsvurdering.kjennelsensBetydningOgLaeringsverdi"
        requireAnswer(
            field = "$betydningPath.avsagtMedForenkletBegrunnelse",
            value = betydning.avsagtMedForenkletBegrunnelse,
        )
        requireAnswer(
            field = "$betydningPath.prinsipieltRettsspoersmaal",
            value = betydning.prinsipieltRettsspoersmaal,
        )
        requireAnswer(
            field = "$betydningPath.bryterMedEtablertPraksisINavEllerTR",
            value = betydning.bryterMedEtablertPraksisINavEllerTR,
        )
        requireAnswer(
            field = "$betydningPath.betydningForFlereFagomraader",
            value = betydning.betydningForFlereFagomraader,
        )
        requireAnswer(
            field = "$betydningPath.inneholderLaeringspunkterKnyttetTilGenerellForvaltningsrettEllerGenerellJuss",
            value = betydning.inneholderLaeringspunkterKnyttetTilGenerellForvaltningsrettEllerGenerellJuss,
        )
        requireAnswer(
            field = "$betydningPath.omhandlerEOESRettEllerAnnetInternasjonaltRegelverk",
            value = betydning.omhandlerEOESRettEllerAnnetInternasjonaltRegelverk,
        )
        requireAnswer(
            field = "$betydningPath.boerDelesMedFagmiljoetSomLaeringseksempel",
            value = betydning.boerDelesMedFagmiljoetSomLaeringseksempel,
        )
    }

    private fun MutableList<KvalitetsvurderingValidationError>.validateOmgjort(
        field: String,
        answer: Boolean?,
        selected: List<Boolean>,
    ) {
        if (answer == null) {
            requireAnswer(field = field, value = answer)
        } else if (answer && selected.none { it }) {
            add(KvalitetsvurderingValidationError(field = field, message = "Minst en grunn må velges"))
        } else if (!answer && selected.any { it }) {
            add(KvalitetsvurderingValidationError(field = field, message = "Ingen grunner kan velges"))
        }
    }

    private fun MutableList<KvalitetsvurderingValidationError>.validateSelector(
        field: String,
        selected: Boolean,
        hjemler: List<Registreringshjemmel>,
        allowedHjemler: Set<Registreringshjemmel>,
    ) {
        if (selected && hjemler.isEmpty()) {
            add(
                KvalitetsvurderingValidationError(
                    field = field,
                    message = "Må velge minst en registreringshjemmel",
                ),
            )
        }
        if (!selected && hjemler.isNotEmpty()) {
            add(KvalitetsvurderingValidationError(field = field, message = "Må være tom"))
        }
        if (hjemler.any { it !in allowedHjemler }) {
            add(KvalitetsvurderingValidationError(field = field, message = "Kan kun inneholde registreringshjemler fra saken"))
        }
    }

    private fun MutableList<KvalitetsvurderingValidationError>.requireAnswer(
        field: String,
        value: Any?,
    ) {
        if (value == null) {
            add(KvalitetsvurderingValidationError(field = field, message = "Må besvares før fullføring"))
        }
    }

    private fun typeMismatch() =
        KvalitetsvurderingValidationError(
            field = "kvalitetsvurdering",
            message = "Kvalitetsvurderingstype samsvarer ikke med sakstype",
        )
}

data class KvalitetsvurderingValidationErrors(
    val validationErrors: List<KvalitetsvurderingValidationError>,
)

data class KvalitetsvurderingValidationError(
    val field: String,
    val message: String,
)

class KvalitetsvurderingValidationException(
    val errors: List<KvalitetsvurderingValidationError>,
) : RuntimeException("Kvalitetsvurdering is incomplete or invalid")
