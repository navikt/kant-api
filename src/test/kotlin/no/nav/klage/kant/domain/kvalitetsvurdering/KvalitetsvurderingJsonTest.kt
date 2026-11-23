package no.nav.klage.kant.domain.kvalitetsvurdering

import io.kotest.matchers.shouldBe
import no.nav.klage.kant.domain.saksdata.Saksdata
import no.nav.klage.kant.domain.saksdata.toResponse
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.exc.InvalidTypeIdException
import tools.jackson.databind.exc.UnrecognizedPropertyException
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KvalitetsvurderingJsonTest {
    private val mapper = jacksonObjectMapper()
    private val strictAssessmentReader =
        mapper
            .readerFor(Kvalitetsvurdering::class.java)
            .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    @Test
    fun `round trips default and populated variants with explicit type`() {
        val values: List<Pair<String, Kvalitetsvurdering>> =
            listOf(
                "ANKE_V1" to AnkeKvalitetsvurderingV1(),
                "ANKE_V1" to
                    AnkeKvalitetsvurderingV1(
                        omgjortIKlageomgangen =
                            AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                                vedtaketBurdeVaertOmgjortIKlageomgangen = false,
                                feilILovtolkningen =
                                    AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                                        selected = true,
                                        registreringshjemler = listOf(Registreringshjemmel.FTRL_1_3),
                                    ),
                            ),
                    ),
                "ANKE_ITR_V1" to AnkeITRKvalitetsvurderingV1(),
                "ANKE_ITR_V1" to
                    AnkeITRKvalitetsvurderingV1(
                        omgjortIAnkeomgangen =
                            AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen(
                                vedtaketBurdeVaertOmgjortIAnkeomgangen = true,
                                feilIFaktagrunnlaget =
                                    AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen.SelectorWithRegistreringshjemler(
                                        selected = true,
                                        registreringshjemler = listOf(Registreringshjemmel.FTRL_1_3),
                                    ),
                            ),
                        rettensSammensetning =
                            AnkeITRKvalitetsvurderingV1.RettensSammensetning(
                                rettensSammensetning =
                                    AnkeITRKvalitetsvurderingV1.RettensSammensetning.RettensSammensetningEnum
                                        .TRE_JURIDISKE_MEDLEMMER,
                            ),
                        kjennelsensBetydningOgLaeringsverdi =
                            AnkeITRKvalitetsvurderingV1.KjennelsensBetydningOgLaeringsverdi(
                                avsagtMedForenkletBegrunnelse = false,
                                prinsipieltRettsspoersmaal = true,
                            ),
                    ),
            )

        values.forEach { (type, value) ->
            val json = mapper.writerFor(Kvalitetsvurdering::class.java).writeValueAsString(value)
            json.contains("\"type\":\"$type\"") shouldBe true
            json.contains("\"created\"") shouldBe false
            json.contains("\"modified\"") shouldBe false
            mapper.readValue<Kvalitetsvurdering>(json) shouldBe value
        }
    }

    @Test
    fun `rejects timestamps in assessment input`() {
        listOf("ANKE_V1", "ANKE_ITR_V1").forEach { type ->
            listOf("created", "modified").forEach { field ->
                assertThrows<UnrecognizedPropertyException> {
                    strictAssessmentReader.readValue(
                        """{"type":"$type","$field":"2026-11-01T09:15:00"}""",
                    )
                }
            }
        }
    }

    @Test
    fun `rejects missing and unknown type discriminator`() {
        assertThrows<InvalidTypeIdException> {
            mapper.readValue<Kvalitetsvurdering>("""{"omgjortIKlageomgangen":{}}""")
        }
        assertThrows<InvalidTypeIdException> {
            mapper.readValue<Kvalitetsvurdering>("""{"type":"UNKNOWN_V1"}""")
        }
    }

    @Test
    fun `flat response variants retain assessment fields and add saksdata timestamps`() {
        val created = LocalDateTime.of(2026, 11, 1, 9, 15)
        val modified = LocalDateTime.of(2026, 11, 20, 14, 30)
        val values =
            listOf(
                "ANKE_V1" to AnkeKvalitetsvurderingV1(),
                "ANKE_ITR_V1" to AnkeITRKvalitetsvurderingV1(),
            )

        values.forEach { (type, value) ->
            val entity =
                Saksdata(
                    kabalReference = UUID.randomUUID(),
                    sakstype =
                        if (type == "ANKE_V1") {
                            Type.ANKE_ETTER_2027
                        } else {
                            Type.ANKE_I_TRYGDERETTEN_ETTER_2027
                        },
                    ytelse = Ytelse.SYK_SYK,
                    mottattKlageinstans = LocalDate.of(2026, 11, 1),
                    sakITrygderettenOpprettet = null,
                    tilknyttetEnhetId = null,
                    tilbakekreving = null,
                    created = created,
                    modified = modified,
                    kvalitetsvurdering = value,
                    trygderettenSaksnummer = "1234",
                )
            val domainJson = mapper.writerFor(Kvalitetsvurdering::class.java).writeValueAsString(value)
            val response = entity.toKvalitetsvurderingResponse()
            val responseJson = mapper.writerFor(KvalitetsvurderingResponse::class.java).writeValueAsString(response)

            responseJson.startsWith(domainJson.removeSuffix("}") + ",") shouldBe true
            responseJson.contains("\"type\":\"$type\"") shouldBe true
            responseJson.contains("\"created\":${mapper.writeValueAsString(created)}") shouldBe true
            responseJson.contains("\"modified\":${mapper.writeValueAsString(modified)}") shouldBe true
            response shouldBe
                when (value) {
                    is AnkeKvalitetsvurderingV1 -> {
                        AnkeKvalitetsvurderingV1Response(
                            omgjortIKlageomgangen = value.omgjortIKlageomgangen,
                            saksbehandlingsfeilIKlageomgangen = value.saksbehandlingsfeilIKlageomgangen,
                            created = created,
                            modified = modified,
                        )
                    }

                    is AnkeITRKvalitetsvurderingV1 -> {
                        AnkeITRKvalitetsvurderingV1Response(
                            omgjortIAnkeomgangen = value.omgjortIAnkeomgangen,
                            rettensSammensetning = value.rettensSammensetning,
                            kjennelsensBetydningOgLaeringsverdi = value.kjennelsensBetydningOgLaeringsverdi,
                            created = created,
                            modified = modified,
                        )
                    }
                }
            entity.toResponse().kvalitetsvurdering shouldBe response
        }
    }
}
