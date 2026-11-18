package no.nav.klage.kant.domain.kvalitetsvurdering

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KvalitetsvurderingValidatorTest {
    private val hjemmel = Registreringshjemmel.FTRL_1_3
    private val supportedTypes = listOf(Type.ANKE_ETTER_2027, Type.ANKE_I_TRYGDERETTEN_ETTER_2027)

    @Test
    fun `accepts completed assessments with false answers for both types`() {
        supportedTypes.forEach { sakstype ->
            KvalitetsvurderingValidator.validate(
                sakstype = sakstype,
                kvalitetsvurdering = completeAssessment(sakstype),
                registreringshjemler = emptySet(),
            )
        }
    }

    @Test
    fun `requires an assessment of the matching type`() {
        supportedTypes.forEach { sakstype ->
            validationErrors(sakstype = sakstype, assessment = null)
                .map { it.field }
                .toSet() shouldBe setOf("kvalitetsvurdering")
            validationErrors(
                sakstype = sakstype,
                assessment = completeAssessment(otherSupportedType(sakstype)),
            ).map { it.field }
                .toSet() shouldBe setOf("kvalitetsvurdering")
        }
    }

    @Test
    fun `requires omgjort answers for both variants`() {
        val ankeErrors =
            validationErrors(
                sakstype = Type.ANKE_ETTER_2027,
                assessment = AnkeKvalitetsvurderingV1(),
            )
        ankeErrors.map { it.field } shouldContainAll
            listOf("kvalitetsvurdering.omgjortIKlageomgangen.vedtaketBurdeVaertOmgjortIKlageomgangen")

        val itrErrors =
            validationErrors(
                sakstype = Type.ANKE_I_TRYGDERETTEN_ETTER_2027,
                assessment = AnkeITRKvalitetsvurderingV1(),
            )
        itrErrors.map { it.field } shouldContainAll
            listOf("kvalitetsvurdering.omgjortIAnkeomgangen.vedtaketBurdeVaertOmgjortIAnkeomgangen")
    }

    @Test
    fun `requires an omgjort reason when true and rejects reasons when false`() {
        supportedTypes.forEach { sakstype ->
            val trueWithoutReason =
                assessmentWithOmgjort(sakstype = sakstype, omgjort = true, withReason = false)
            val missingReasonErrors = validationErrors(sakstype = sakstype, assessment = trueWithoutReason)
            missingReasonErrors.any { it.field.contains("vedtaketBurdeVaertOmgjort") } shouldBe true

            val falseWithReason =
                assessmentWithOmgjort(sakstype = sakstype, omgjort = false, withReason = true)
            val contradictoryErrors =
                validationErrors(
                    sakstype = sakstype,
                    assessment = falseWithReason,
                    registreringshjemler = setOf(hjemmel),
                )
            contradictoryErrors.any { it.field.contains("vedtaketBurdeVaertOmgjort") } shouldBe true

            KvalitetsvurderingValidator.validate(
                sakstype = sakstype,
                kvalitetsvurdering =
                    assessmentWithOmgjort(sakstype = sakstype, omgjort = true, withReason = true),
                registreringshjemler = setOf(hjemmel),
            )
        }
    }

    @Test
    fun `validates selected and unselected selector homes against finalization homes`() {
        supportedTypes.forEach { sakstype ->
            val section =
                if (sakstype == Type.ANKE_ETTER_2027) {
                    "omgjortIKlageomgangen"
                } else {
                    "omgjortIAnkeomgangen"
                }
            val errors =
                validationErrors(
                    sakstype = sakstype,
                    assessment = assessmentWithSelectorViolations(sakstype),
                    registreringshjemler = setOf(hjemmel),
                )

            val fields = errors.map { it.field }.toSet()
            fields shouldContainAll
                setOf(
                    "kvalitetsvurdering.$section.feilILovtolkningen.registreringshjemler",
                    "kvalitetsvurdering.$section.feilIFaktagrunnlaget.registreringshjemler",
                )
            fields.contains("kvalitetsvurdering.$section.feilIRettsanvendelsen.registreringshjemler") shouldBe false
        }
    }

    @Test
    fun `rejects selector homes absent from finalization homes`() {
        supportedTypes.forEach { sakstype ->
            val errors =
                validationErrors(
                    sakstype = sakstype,
                    assessment = assessmentWithOmgjort(sakstype = sakstype, omgjort = true, withReason = true),
                    registreringshjemler = emptySet(),
                )

            errors.size shouldBe 1
            errors.single().field.endsWith(".feilILovtolkningen.registreringshjemler") shouldBe true
        }
    }

    @Test
    fun `requires both ITR composition fields and all seven learning answers`() {
        val learningPath = "kvalitetsvurdering.kjennelsensBetydningOgLaeringsverdi."
        val fields =
            validationErrors(
                sakstype = Type.ANKE_I_TRYGDERETTEN_ETTER_2027,
                assessment = AnkeITRKvalitetsvurderingV1(),
            ).map { it.field }
                .toSet()

        fields shouldContainAll
            setOf(
                "kvalitetsvurdering.rettensSammensetning.rettensSammensetning",
                "kvalitetsvurdering.rettensSammensetning.attfoeringskyndigEllerMedisinskKyndig",
                "${learningPath}avsagtMedForenkletBegrunnelse",
                "${learningPath}prinsipieltRettsspoersmaal",
                "${learningPath}bryterMedEtablertPraksisINavEllerTR",
                "${learningPath}betydningForFlereFagomraader",
                learningPath +
                    "inneholderLaeringspunkterKnyttetTilGenerellForvaltningsrettEllerGenerellJuss",
                "${learningPath}omhandlerEOESRettEllerAnnetInternasjonaltRegelverk",
                "${learningPath}boerDelesMedFagmiljoetSomLaeringseksempel",
            )
    }

    private fun validationErrors(
        sakstype: Type,
        assessment: Kvalitetsvurdering?,
        registreringshjemler: Set<Registreringshjemmel> = emptySet(),
    ): List<KvalitetsvurderingValidationError> =
        assertThrows<KvalitetsvurderingValidationException> {
            KvalitetsvurderingValidator.validate(
                sakstype = sakstype,
                kvalitetsvurdering = assessment,
                registreringshjemler = registreringshjemler,
            )
        }.errors

    private fun completeAssessment(sakstype: Type): Kvalitetsvurdering =
        when (sakstype) {
            Type.ANKE_ETTER_2027 -> {
                AnkeKvalitetsvurderingV1(
                    omgjortIKlageomgangen =
                        AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                            vedtaketBurdeVaertOmgjortIKlageomgangen = false,
                        ),
                )
            }

            Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> {
                AnkeITRKvalitetsvurderingV1(
                    omgjortIAnkeomgangen =
                        AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen(
                            vedtaketBurdeVaertOmgjortIAnkeomgangen = false,
                        ),
                    rettensSammensetning =
                        AnkeITRKvalitetsvurderingV1.RettensSammensetning(
                            rettensSammensetning =
                                AnkeITRKvalitetsvurderingV1.RettensSammensetning.RettensSammensetningEnum.ORDINAER_RETT,
                            attfoeringskyndigEllerMedisinskKyndig =
                                AnkeITRKvalitetsvurderingV1.RettensSammensetning.AttfoeringskyndigEllerMedisinskKyndigEnum.NEI,
                        ),
                    kjennelsensBetydningOgLaeringsverdi =
                        AnkeITRKvalitetsvurderingV1.KjennelsensBetydningOgLaeringsverdi(
                            avsagtMedForenkletBegrunnelse = false,
                            prinsipieltRettsspoersmaal = false,
                            bryterMedEtablertPraksisINavEllerTR = false,
                            betydningForFlereFagomraader = false,
                            inneholderLaeringspunkterKnyttetTilGenerellForvaltningsrettEllerGenerellJuss = false,
                            omhandlerEOESRettEllerAnnetInternasjonaltRegelverk = false,
                            boerDelesMedFagmiljoetSomLaeringseksempel = false,
                        ),
                )
            }

            else -> {
                error("Unsupported test type")
            }
        }

    private fun assessmentWithOmgjort(
        sakstype: Type,
        omgjort: Boolean,
        withReason: Boolean,
    ): Kvalitetsvurdering {
        val selector =
            AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                selected = withReason,
                registreringshjemler = if (withReason) listOf(hjemmel) else emptyList(),
            )
        return when (sakstype) {
            Type.ANKE_ETTER_2027 -> {
                AnkeKvalitetsvurderingV1(
                    omgjortIKlageomgangen =
                        AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                            vedtaketBurdeVaertOmgjortIKlageomgangen = omgjort,
                            feilILovtolkningen =
                                AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                                    selected = selector.selected,
                                    registreringshjemler = selector.registreringshjemler,
                                ),
                        ),
                )
            }

            Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> {
                val complete = completeAssessment(sakstype) as AnkeITRKvalitetsvurderingV1
                complete.copy(
                    omgjortIAnkeomgangen =
                        complete.omgjortIAnkeomgangen.copy(
                            vedtaketBurdeVaertOmgjortIAnkeomgangen = omgjort,
                            feilILovtolkningen =
                                AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen.SelectorWithRegistreringshjemler(
                                    selected = selector.selected,
                                    registreringshjemler = selector.registreringshjemler,
                                ),
                        ),
                )
            }

            else -> {
                error("Unsupported test type")
            }
        }
    }

    private fun assessmentWithSelectorViolations(sakstype: Type): Kvalitetsvurdering =
        when (sakstype) {
            Type.ANKE_ETTER_2027 -> {
                AnkeKvalitetsvurderingV1(
                    omgjortIKlageomgangen =
                        AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                            vedtaketBurdeVaertOmgjortIKlageomgangen = true,
                            feilILovtolkningen =
                                AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                                    selected = true,
                                ),
                            feilIFaktagrunnlaget =
                                AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                                    registreringshjemler = listOf(hjemmel),
                                ),
                            feilIRettsanvendelsen =
                                AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                                    selected = true,
                                    registreringshjemler = listOf(hjemmel),
                                ),
                        ),
                )
            }

            Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> {
                val complete = completeAssessment(sakstype) as AnkeITRKvalitetsvurderingV1
                complete.copy(
                    omgjortIAnkeomgangen =
                        complete.omgjortIAnkeomgangen.copy(
                            vedtaketBurdeVaertOmgjortIAnkeomgangen = true,
                            feilILovtolkningen =
                                AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen.SelectorWithRegistreringshjemler(
                                    selected = true,
                                ),
                            feilIFaktagrunnlaget =
                                AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen.SelectorWithRegistreringshjemler(
                                    registreringshjemler = listOf(hjemmel),
                                ),
                            feilIRettsanvendelsen =
                                AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen.SelectorWithRegistreringshjemler(
                                    selected = true,
                                    registreringshjemler = listOf(hjemmel),
                                ),
                        ),
                )
            }

            else -> {
                error("Unsupported test type")
            }
        }

    private fun otherSupportedType(sakstype: Type): Type =
        when (sakstype) {
            Type.ANKE_ETTER_2027 -> Type.ANKE_I_TRYGDERETTEN_ETTER_2027
            Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> Type.ANKE_ETTER_2027
            else -> error("Unsupported test type")
        }
}
