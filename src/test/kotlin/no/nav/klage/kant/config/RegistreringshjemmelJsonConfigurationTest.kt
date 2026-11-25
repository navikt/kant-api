package no.nav.klage.kant.config

import io.kotest.matchers.shouldBe
import no.nav.klage.kant.api.SaksdataController
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeITRKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.Kvalitetsvurdering
import no.nav.klage.kant.domain.saksdata.Saksdata
import no.nav.klage.kant.repositories.SaksdataRepository
import no.nav.klage.kant.services.SaksdataService
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import java.util.stream.Stream

@WebMvcTest(SaksdataController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SaksdataService::class, ExceptionHandler::class, RegistreringshjemmelJsonConfiguration::class)
class RegistreringshjemmelJsonConfigurationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var saksdataRepository: SaksdataRepository

    private val id = UUID.randomUUID()
    private lateinit var saksdata: Saksdata

    @BeforeEach
    fun setup() {
        saksdata =
            Saksdata(
                id = id,
                kabalReference = UUID.randomUUID(),
                sakstype = Type.ANKE_ETTER_2027,
                ytelse = Ytelse.SYK_SYK,
                mottattKlageinstans = LocalDate.of(2026, 11, 1),
                sakITrygderettenOpprettet = null,
                tilknyttetEnhetId = null,
                tilbakekreving = null,
                trygderettenSaksnummer = "1234",
            )
        `when`(saksdataRepository.findForUpdate(id)).thenAnswer { saksdata }
        `when`(saksdataRepository.findById(id)).thenAnswer { Optional.of(saksdata) }
        `when`(saksdataRepository.saveAndFlush(any<Saksdata>())).thenAnswer { invocation ->
            invocation.getArgument<Saksdata>(0)
        }
    }

    @Test
    fun `accepts registreringshjemmel ID for saksdata and serializes its ID`() {
        listOf(Registreringshjemmel.FTRL_1_3, Registreringshjemmel.FTRL_1_3A).forEach { hjemmel ->
            mockMvc
                .perform(
                    put("/api/saksdata/$id")
                        .contentType(APPLICATION_JSON)
                        .content("""{"registreringshjemmelIdSet":["${hjemmel.id}"]}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.registreringshjemler[0]").value(hjemmel.id))

            saksdata.registreringshjemler shouldBe setOf(hjemmel)
        }
    }

    @Test
    fun `deletes open saksdata and returns no content`() {
        mockMvc
            .perform(delete("/api/saksdata/$id"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))

        verify(saksdataRepository).findForUpdate(id)
        verify(saksdataRepository).delete(saksdata)
    }

    @Test
    fun `delete returns conflict for finalized saksdata without deleting it`() {
        saksdata.avsluttetAvSaksbehandler = java.time.LocalDateTime.of(2026, 11, 16, 12, 0)

        mockMvc
            .perform(delete("/api/saksdata/$id"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.detail").value("Saksdata is already finalized"))

        verify(saksdataRepository).findForUpdate(id)
        verify(saksdataRepository, never()).delete(any<Saksdata>())
    }

    @Test
    fun `delete returns not found for missing saksdata without deleting it`() {
        val missingId = UUID.randomUUID()
        `when`(saksdataRepository.findForUpdate(missingId)).thenReturn(null)

        mockMvc
            .perform(delete("/api/saksdata/$missingId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("Saksdata not found"))

        verify(saksdataRepository).findForUpdate(missingId)
        verify(saksdataRepository, never()).delete(any<Saksdata>())
    }

    @Test
    fun `delete rejects malformed UUID as a problem detail without accessing the repository`() {
        mockMvc
            .perform(delete("/api/saksdata/not-a-uuid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Invalid request"))

        verifyNoInteractions(saksdataRepository)
    }

    @ParameterizedTest
    @MethodSource("assessmentCases")
    fun `accepts registreringshjemmel ID in assessment requests and responses`(case: AssessmentCase) {
        saksdata.sakstype = case.sakstype
        val request =
            """{"type":"${case.type}","${case.assessmentField}":{"feilILovtolkningen":{"selected":true,"registreringshjemler":["308"]}}}"""

        mockMvc
            .perform(
                put("/api/saksdata/$id/kvalitetsvurdering")
                    .contentType(APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.kvalitetsvurdering.type").value(case.type))
            .andExpect(jsonPath("$.kvalitetsvurdering.${case.assessmentField}.feilILovtolkningen.registreringshjemler[0]").value("308"))

        saksdata.kvalitetsvurdering shouldBe case.expectedAssessment

        mockMvc
            .perform(get("/api/saksdata/$id/kvalitetsvurdering"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value(case.type))
            .andExpect(jsonPath("$.${case.assessmentField}.feilILovtolkningen.registreringshjemler[0]").value("308"))
    }

    @ParameterizedTest
    @MethodSource("invalidRegistreringshjemmelValues")
    fun `rejects invalid registreringshjemmel values in all HTTP request locations`(value: String) {
        val requests =
            listOf(
                "/api/saksdata/$id" to """{"registreringshjemler":$value}""",
                "/api/saksdata/$id/kvalitetsvurdering" to
                    assessmentRequest(type = "ANKE_V1", assessmentField = "omgjortIKlageomgangen", registreringshjemler = value),
                "/api/saksdata/$id/kvalitetsvurdering" to
                    assessmentRequest(type = "ANKE_ITR_V1", assessmentField = "omgjortIAnkeomgangen", registreringshjemler = value),
            )

        requests.forEach { (path, body) ->
            mockMvc
                .perform(put(path).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.detail").value("Invalid request"))
        }

        verifyNoInteractions(saksdataRepository)
    }

    private fun assessmentRequest(
        type: String,
        assessmentField: String,
        registreringshjemler: String,
    ): String =
        """{"type":"$type","$assessmentField":{"feilILovtolkningen":{"selected":true,"registreringshjemler":$registreringshjemler}}}"""

    data class AssessmentCase(
        val type: String,
        val sakstype: Type,
        val assessmentField: String,
        val expectedAssessment: Kvalitetsvurdering,
    )

    companion object {
        @JvmStatic
        fun assessmentCases(): Stream<AssessmentCase> =
            Stream.of(
                AssessmentCase(
                    type = "ANKE_V1",
                    sakstype = Type.ANKE_ETTER_2027,
                    assessmentField = "omgjortIKlageomgangen",
                    expectedAssessment =
                        AnkeKvalitetsvurderingV1(
                            omgjortIKlageomgangen =
                                AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                                    feilILovtolkningen =
                                        AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                                            selected = true,
                                            registreringshjemler = listOf(Registreringshjemmel.FTRL_1_3),
                                        ),
                                ),
                        ),
                ),
                AssessmentCase(
                    type = "ANKE_ITR_V1",
                    sakstype = Type.ANKE_I_TRYGDERETTEN_ETTER_2027,
                    assessmentField = "omgjortIAnkeomgangen",
                    expectedAssessment =
                        AnkeITRKvalitetsvurderingV1(
                            omgjortIAnkeomgangen =
                                AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen(
                                    feilILovtolkningen =
                                        AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen.SelectorWithRegistreringshjemler(
                                            selected = true,
                                            registreringshjemler = listOf(Registreringshjemmel.FTRL_1_3),
                                        ),
                                ),
                        ),
                ),
            )

        @JvmStatic
        fun invalidRegistreringshjemmelValues(): Stream<String> =
            Stream.of(
                """["FTRL_1_3"]""",
                """["UNKNOWN"]""",
                "[308]",
                "[true]",
                "[{}]",
                "[[]]",
                """[""]""",
                """[" 308"]""",
                "[null]",
                """"308"""",
            )
    }
}
