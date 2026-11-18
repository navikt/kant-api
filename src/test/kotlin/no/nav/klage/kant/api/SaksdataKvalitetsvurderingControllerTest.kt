package no.nav.klage.kant.api

import no.nav.klage.auth.test.AzureSecurityContractTest
import no.nav.klage.kant.config.SecurityConfiguration
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeITRKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.Kvalitetsvurdering
import no.nav.klage.kant.domain.saksdata.Saksdata
import no.nav.klage.kant.repositories.SaksdataRepository
import no.nav.klage.kant.services.SaksdataService
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.Utfall
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(SaksdataController::class)
@Import(SecurityConfiguration::class, SaksdataService::class)
class SaksdataKvalitetsvurderingControllerTest : AzureSecurityContractTest() {
    @MockitoBean
    private lateinit var repository: SaksdataRepository

    private val id = UUID.fromString("7217771c-4452-438a-9b01-f775a92b2ad3")

    override fun protectedRequest(): MockHttpServletRequestBuilder =
        put("/api/saksdata/$id/kvalitetsvurdering")
            .contentType(APPLICATION_JSON)
            .content("""{"type":"ANKE_V1"}""")

    override fun unauthenticatedPaths(): List<String> =
        listOf("/internal/health", "/internal/health/liveness", "/internal/health/readiness", "/internal/prometheus")

    @BeforeEach
    fun setup() {
        prepareEntity(Type.ANKE_ETTER_2027)
    }

    @ParameterizedTest
    @EnumSource(value = Type::class, names = ["ANKE_ETTER_2027", "ANKE_I_TRYGDERETTEN_ETTER_2027"])
    fun `updates assessment and serializes its discriminator`(sakstype: Type) {
        prepareEntity(sakstype)
        val type = discriminator(sakstype)

        mockMvc
            .perform(authorizedPut("""{"type":"$type"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.kvalitetsvurdering.type").value(type))
    }

    @Test
    fun `rejects mismatched assessment variant without saving`() {
        mockMvc
            .perform(authorizedPut("""{"type":"ANKE_ITR_V1"}"""))
            .andExpect(status().isBadRequest)

        verify(repository, never()).saveAndFlush(any(Saksdata::class.java))
    }

    @Test
    fun `returns not found for missing saksdata`() {
        `when`(repository.findForUpdate(id)).thenReturn(null)

        mockMvc
            .perform(authorizedPut("""{"type":"ANKE_V1"}"""))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `returns conflict for closed saksdata`() {
        prepareEntity(Type.ANKE_ETTER_2027).apply {
            avsluttetAvSaksbehandler = LocalDateTime.of(2026, 11, 16, 12, 0)
        }

        mockMvc
            .perform(authorizedPut("""{"type":"ANKE_V1"}"""))
            .andExpect(status().isConflict)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"omgjortIKlageomgangen":{}}""",
            """{"type":"UNKNOWN_V1"}""",
            """{"type":null}""",
        ],
    )
    fun `rejects missing unknown and null discriminators`(body: String) {
        mockMvc
            .perform(authorizedPut(body))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `finalization returns safe validation errors for unfilled assessment without saving`() {
        mockMvc
            .perform(authorizedFinalize(finalizationBody))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(
                jsonPath("$.errors[0].field")
                    .value("kvalitetsvurdering.omgjortIKlageomgangen.vedtaketBurdeVaertOmgjortIKlageomgangen"),
            ).andExpect(jsonPath("$.errors[0].message").exists())

        verify(repository, never()).saveAndFlush(any(Saksdata::class.java))
    }

    @Test
    fun `finalization accepts false assessment answers`() {
        prepareEntity(Type.ANKE_ETTER_2027).apply {
            kvalitetsvurdering =
                AnkeKvalitetsvurderingV1(
                    omgjortIKlageomgangen =
                        AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                            vedtaketBurdeVaertOmgjortIKlageomgangen = false,
                        ),
                )
        }

        mockMvc
            .perform(authorizedFinalize(finalizationBody))
            .andExpect(status().isOk)

        verify(repository).saveAndFlush(any(Saksdata::class.java))
    }

    private fun authorizedPut(body: String): MockHttpServletRequestBuilder =
        put("/api/saksdata/$id/kvalitetsvurdering")
            .header("Authorization", "Bearer ${mockAzure.token()}")
            .contentType(APPLICATION_JSON)
            .content(body)

    private fun authorizedFinalize(body: String): MockHttpServletRequestBuilder =
        put("/api/saksdata/$id/avslutning")
            .header("Authorization", "Bearer ${mockAzure.token()}")
            .contentType(APPLICATION_JSON)
            .content(body)

    private fun prepareEntity(sakstype: Type): Saksdata {
        val entity =
            Saksdata(
                id = id,
                kabalReference = UUID.randomUUID(),
                sakstype = sakstype,
                ytelse = Ytelse.SYK_SYK,
                mottattKlageinstans = LocalDate.of(2026, 11, 1),
                sakITrygderettenOpprettet = null,
                tilknyttetEnhet = null,
                tilbakekreving = null,
            ).apply {
                kvalitetsvurdering = defaultKvalitetsvurdering(sakstype)
            }
        `when`(repository.findForUpdate(id)).thenReturn(entity)
        `when`(repository.saveAndFlush(any(Saksdata::class.java))).thenAnswer { it.getArgument<Saksdata>(0) }
        return entity
    }

    private val finalizationBody =
        jacksonObjectMapper().writeValueAsString(
            mapOf(
                "utfall" to Utfall.MEDHOLD,
                "registreringshjemler" to setOf(Registreringshjemmel.FTRL_1_3),
                "avsluttetAvSaksbehandler" to "2026-11-16T12:00:00",
                "tilknyttetEnhet" to "4291",
                "tilbakekreving" to false,
            ),
        )

    private fun defaultKvalitetsvurdering(sakstype: Type): Kvalitetsvurdering =
        when (sakstype) {
            Type.ANKE_ETTER_2027 -> AnkeKvalitetsvurderingV1()
            Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> AnkeITRKvalitetsvurderingV1()
            else -> error("Unsupported type in test")
        }

    private fun discriminator(sakstype: Type): String =
        when (sakstype) {
            Type.ANKE_ETTER_2027 -> "ANKE_V1"
            Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> "ANKE_ITR_V1"
            else -> error("Unsupported type in test")
        }
}
