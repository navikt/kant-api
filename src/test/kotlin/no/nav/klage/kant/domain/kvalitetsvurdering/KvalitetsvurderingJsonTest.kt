package no.nav.klage.kant.domain.kvalitetsvurdering

import io.kotest.matchers.shouldBe
import no.nav.klage.kant.domain.saksdata.Saksdata
import no.nav.klage.kant.domain.saksdata.toResponse
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.exc.InvalidTypeIdException
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.UUID

class KvalitetsvurderingJsonTest {
    private val mapper = jacksonObjectMapper()

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
            mapper.readValue<Kvalitetsvurdering>(json) shouldBe value
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
    fun `response mapping preserves quality assessment`() {
        val value = AnkeKvalitetsvurderingV1()
        val entity =
            Saksdata(
                kabalReference = UUID.randomUUID(),
                sakstype = Type.ANKE_ETTER_2027,
                ytelse = Ytelse.SYK_SYK,
                mottattKlageinstans = LocalDate.of(2026, 11, 1),
                sakITrygderettenOpprettet = null,
                tilknyttetEnhet = null,
                tilbakekreving = null,
                kvalitetsvurdering = value,
                trygderettenSaksnummer = "1234",
            )

        entity.toResponse().kvalitetsvurdering shouldBe value
    }
}
