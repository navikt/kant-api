package no.nav.klage.kant.domain.kvalitetsvurdering

import no.nav.klage.kant.domain.saksdata.Saksdata
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel

object KvalitetsvurderingValidator {
    fun validate(
        sakstype: Type,
        kvalitetsvurdering: Kvalitetsvurdering?,
        registreringshjemler: Set<Registreringshjemmel>,
    ) {
        if (sakstype in listOf(Type.BEGJAERING_OM_GJENOPPTAK_ETTER_2027, Type.BEGJAERING_OM_GJENOPPTAK_I_TRYGDERETTEN_ETTER_2027)) return
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
                                field = Saksdata::kvalitetsvurdering.name,
                                reason = "Må fylles ut før fullføring",
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
        val path = "${Saksdata::kvalitetsvurdering.name}.${AnkeKvalitetsvurderingV1::omgjortIKlageomgangen.name}"
        validateOmgjort(
            field = "$path.${omgjort::vedtaketBurdeVaertOmgjortIKlageomgangen.name}",
            answer = omgjort.vedtaketBurdeVaertOmgjortIKlageomgangen,
            selected =
                listOf(
                    omgjort.feilILovtolkningen.selected,
                    omgjort.feilIFaktagrunnlaget.selected,
                    omgjort.feilIRettsanvendelsen.selected,
                ),
        )
        validateSelector(
            field = "$path.${omgjort::feilILovtolkningen.name}.${omgjort.feilILovtolkningen::registreringshjemler.name}",
            selected = omgjort.feilILovtolkningen.selected,
            hjemler = omgjort.feilILovtolkningen.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        validateSelector(
            field = "$path.${omgjort::feilIFaktagrunnlaget.name}.${omgjort.feilIFaktagrunnlaget::registreringshjemler.name}",
            selected = omgjort.feilIFaktagrunnlaget.selected,
            hjemler = omgjort.feilIFaktagrunnlaget.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        validateSelector(
            field = "$path.${omgjort::feilIRettsanvendelsen.name}.${omgjort.feilIRettsanvendelsen::registreringshjemler.name}",
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
        val path = "${Saksdata::kvalitetsvurdering.name}.${AnkeITRKvalitetsvurderingV1::omgjortIAnkeomgangen.name}"
        validateOmgjort(
            field = "$path.${omgjort::vedtaketBurdeVaertOmgjortIAnkeomgangen.name}",
            answer = omgjort.vedtaketBurdeVaertOmgjortIAnkeomgangen,
            selected =
                listOf(
                    omgjort.feilILovtolkningen.selected,
                    omgjort.feilIFaktagrunnlaget.selected,
                    omgjort.feilIRettsanvendelsen.selected,
                ),
        )
        validateSelector(
            field = "$path.${omgjort::feilILovtolkningen.name}.${omgjort.feilILovtolkningen::registreringshjemler.name}",
            selected = omgjort.feilILovtolkningen.selected,
            hjemler = omgjort.feilILovtolkningen.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        validateSelector(
            field = "$path.${omgjort::feilIFaktagrunnlaget.name}.${omgjort.feilIFaktagrunnlaget::registreringshjemler.name}",
            selected = omgjort.feilIFaktagrunnlaget.selected,
            hjemler = omgjort.feilIFaktagrunnlaget.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        validateSelector(
            field = "$path.${omgjort::feilIRettsanvendelsen.name}.${omgjort.feilIRettsanvendelsen::registreringshjemler.name}",
            selected = omgjort.feilIRettsanvendelsen.selected,
            hjemler = omgjort.feilIRettsanvendelsen.registreringshjemler,
            allowedHjemler = allowedHjemler,
        )
        val sammensetning = assessment.rettensSammensetning
        val sammensetningPath = "${Saksdata::kvalitetsvurdering.name}.${assessment::rettensSammensetning.name}"
        requireAnswer(
            field = "$sammensetningPath.${sammensetning::rettensSammensetning.name}",
            value = sammensetning.rettensSammensetning,
        )
        requireAnswer(
            field = "$sammensetningPath.${sammensetning::attfoeringskyndigEllerMedisinskKyndig.name}",
            value = sammensetning.attfoeringskyndigEllerMedisinskKyndig,
        )
        val betydning = assessment.kjennelsensBetydningOgLaeringsverdi
        val betydningPath = "${Saksdata::kvalitetsvurdering.name}.${assessment::kjennelsensBetydningOgLaeringsverdi.name}"
        requireAnswer(
            field = "$betydningPath.${betydning::avsagtMedForenkletBegrunnelse.name}",
            value = betydning.avsagtMedForenkletBegrunnelse,
        )
        requireAnswer(
            field = "$betydningPath.${betydning::prinsipiellBetydning.name}",
            value = betydning.prinsipiellBetydning,
        )
        requireAnswer(
            field = "$betydningPath.${betydning::bryterMedEtablertPraksisINavEllerTR.name}",
            value = betydning.bryterMedEtablertPraksisINavEllerTR,
        )
        requireAnswer(
            field = "$betydningPath.${betydning::betydningForFlereFagomraader.name}",
            value = betydning.betydningForFlereFagomraader,
        )
        requireAnswer(
            field = "$betydningPath.${betydning::inneholderLaeringspunkterKnyttetTilGenerellForvaltningsrettEllerGenerellJuss.name}",
            value = betydning.inneholderLaeringspunkterKnyttetTilGenerellForvaltningsrettEllerGenerellJuss,
        )
        requireAnswer(
            field = "$betydningPath.${betydning::omhandlerEOESRettEllerAnnetInternasjonaltRegelverk.name}",
            value = betydning.omhandlerEOESRettEllerAnnetInternasjonaltRegelverk,
        )
        requireAnswer(
            field = "$betydningPath.${betydning::boerDelesMedFagmiljoetSomLaeringseksempel.name}",
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
            add(KvalitetsvurderingValidationError(field = field, reason = "Minst en grunn må velges"))
        } else if (!answer && selected.any { it }) {
            add(KvalitetsvurderingValidationError(field = field, reason = "Ingen grunner kan velges"))
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
                    reason = "Må velge minst en registreringshjemmel",
                ),
            )
        }
        if (!selected && hjemler.isNotEmpty()) {
            add(KvalitetsvurderingValidationError(field = field, reason = "Må være tom"))
        }
        if (hjemler.any { it !in allowedHjemler }) {
            add(KvalitetsvurderingValidationError(field = field, reason = "Kan kun inneholde registreringshjemler fra saken"))
        }
    }

    private fun MutableList<KvalitetsvurderingValidationError>.requireAnswer(
        field: String,
        value: Any?,
    ) {
        if (value == null) {
            add(KvalitetsvurderingValidationError(field = field, reason = "Må besvares før fullføring"))
        }
    }

    private fun typeMismatch() =
        KvalitetsvurderingValidationError(
            field = Saksdata::kvalitetsvurdering.name,
            reason = "Kvalitetsvurderingstype samsvarer ikke med sakstype",
        )
}

data class KvalitetsvurderingValidationErrors(
    val validationErrors: List<KvalitetsvurderingValidationError>,
)

data class KvalitetsvurderingValidationError(
    val field: String,
    val reason: String,
)

class KvalitetsvurderingValidationException(
    val errors: List<KvalitetsvurderingValidationError>,
) : RuntimeException("Kvalitetsvurdering is incomplete or invalid")
