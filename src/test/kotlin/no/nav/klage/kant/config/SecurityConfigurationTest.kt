package no.nav.klage.kant.config

import no.nav.klage.auth.test.AzureSecurityContractTest
import no.nav.klage.kant.api.SaksdataController
import no.nav.klage.kant.domain.saksdata.SaksdataResponse
import no.nav.klage.kant.services.SaksdataService
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.`when`
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(SaksdataController::class)
@Import(SecurityConfiguration::class)
class SecurityConfigurationTest : AzureSecurityContractTest() {
    @MockitoBean
    private lateinit var saksdataService: SaksdataService

    private val id = UUID.randomUUID()

    override fun protectedRequest(): MockHttpServletRequestBuilder = get("/api/saksdata/$id")

    override fun unauthenticatedPaths(): List<String> =
        listOf("/internal/health", "/internal/health/liveness", "/internal/health/readiness", "/internal/prometheus")

    @BeforeEach
    fun setup() {
        `when`(saksdataService.get(id)).thenReturn(
            SaksdataResponse(
                id = id,
                kabalReference = UUID.randomUUID(),
                sakstype = Type.ANKE_ETTER_2027,
                ytelse = Ytelse.SYK_SYK,
                mottattKlageinstans = LocalDate.of(2026, 11, 1),
                sakITrygderettenOpprettet = null,
                tilknyttetEnhet = null,
                tilbakekreving = null,
                utfall = null,
                registreringshjemler = emptySet(),
                avsluttetAvSaksbehandler = null,
            ),
        )
    }
}
