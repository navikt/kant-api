package no.nav.klage.kant.repositories

import io.kotest.matchers.shouldBe
import jakarta.persistence.EntityManager
import no.nav.klage.kant.config.RegistreringshjemmelJsonConfiguration
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeITRKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeITRKvalitetsvurderingV1Response
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeKvalitetsvurderingV1Response
import no.nav.klage.kant.domain.kvalitetsvurdering.Kvalitetsvurdering
import no.nav.klage.kant.domain.kvalitetsvurdering.toKvalitetsvurderingResponse
import no.nav.klage.kant.domain.saksdata.Saksdata
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@Import(RegistreringshjemmelJsonConfiguration::class)
@Testcontainers
class SaksdataKvalitetsvurderingRepositoryTest
    @Autowired
    constructor(
        private val repository: SaksdataRepository,
        private val entityManager: EntityManager,
    ) {
        @Test
        fun `persists assessments as jsonb and permits replacement and interim null`() {
            val ankeAssessment =
                AnkeKvalitetsvurderingV1(
                    omgjortIKlageomgangen =
                        AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                            feilILovtolkningen =
                                AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                                    selected = true,
                                    registreringshjemler = listOf(Registreringshjemmel.FTRL_1_3),
                                ),
                        ),
                )
            val itrAssessment =
                AnkeITRKvalitetsvurderingV1(
                    rettensSammensetning =
                        AnkeITRKvalitetsvurderingV1.RettensSammensetning(
                            rettensSammensetning =
                                AnkeITRKvalitetsvurderingV1.RettensSammensetning.RettensSammensetningEnum.ORDINAER_RETT,
                        ),
                    kjennelsensBetydningOgLaeringsverdi =
                        AnkeITRKvalitetsvurderingV1.KjennelsensBetydningOgLaeringsverdi(
                            prinsipiellBetydning = false,
                        ),
                )
            val anke =
                repository.saveAndFlush(
                    saksdata(
                        sakstype = Type.ANKE_ETTER_2027,
                        kvalitetsvurdering = ankeAssessment,
                    ),
                )
            val itr =
                repository.saveAndFlush(
                    saksdata(
                        sakstype = Type.ANKE_I_TRYGDERETTEN_ETTER_2027,
                        kvalitetsvurdering = itrAssessment,
                    ),
                )
            val interim =
                repository.saveAndFlush(
                    saksdata(
                        sakstype = Type.ANKE_I_TRYGDERETTEN_ETTER_2027,
                    ),
                )

            entityManager.flush()
            entityManager.clear()
            val reloadedAnke = repository.findById(anke.id).orElseThrow()
            val reloadedItr = repository.findById(itr.id).orElseThrow()
            val reloadedInterim = repository.findById(interim.id).orElseThrow()

            reloadedAnke.kvalitetsvurdering shouldBe ankeAssessment
            reloadedItr.kvalitetsvurdering shouldBe itrAssessment
            reloadedInterim.kvalitetsvurdering shouldBe null
            assertJsonbRegistreringshjemmel(id = anke.id, expectedName = "FTRL_1_3")
            assertColumn(id = anke.id, expectedType = "ANKE_V1")
            assertColumn(id = itr.id, expectedType = "ANKE_ITR_V1")
            assertColumn(id = interim.id, expectedType = null)

            reloadedAnke.kvalitetsvurdering = AnkeKvalitetsvurderingV1()
            val updatedAnke = repository.saveAndFlush(reloadedAnke)
            val databaseModified =
                entityManager
                    .createNativeQuery("select modified from saksdata where id = :id")
                    .setParameter("id", anke.id)
                    .singleResult as LocalDateTime

            updatedAnke.created shouldBe createdTimestamp
            updatedAnke.modified shouldBe databaseModified
            databaseModified.isAfter(createdTimestamp) shouldBe true
            val responseModified =
                when (val response = updatedAnke.toKvalitetsvurderingResponse()) {
                    is AnkeKvalitetsvurderingV1Response -> response.modified
                    is AnkeITRKvalitetsvurderingV1Response -> response.modified
                    null -> null
                }
            responseModified shouldBe databaseModified

            val timestampKeysPresent =
                entityManager
                    .createNativeQuery(
                        """
                        select jsonb_exists(kvalitetsvurdering, 'created')
                            or jsonb_exists(kvalitetsvurdering, 'modified')
                        from saksdata where id = :id
                        """.trimIndent(),
                    ).setParameter("id", anke.id)
                    .singleResult as Boolean
            timestampKeysPresent shouldBe false

            entityManager.clear()
            repository.findById(anke.id).orElseThrow().kvalitetsvurdering shouldBe AnkeKvalitetsvurderingV1()
            assertColumn(id = anke.id, expectedType = "ANKE_V1")
        }

        private fun assertColumn(
            id: UUID,
            expectedType: String?,
        ) {
            val columnType =
                entityManager
                    .createNativeQuery(
                        "select pg_typeof(kvalitetsvurdering)::text from saksdata where id = :id",
                    ).setParameter("id", id)
                    .singleResult
            columnType shouldBe "jsonb"

            val discriminator =
                entityManager
                    .createNativeQuery(
                        "select kvalitetsvurdering ->> 'type' from saksdata where id = :id",
                    ).setParameter("id", id)
                    .singleResult
            discriminator shouldBe expectedType
        }

        private fun assertJsonbRegistreringshjemmel(
            id: UUID,
            expectedName: String,
        ) {
            val storedId =
                entityManager
                    .createNativeQuery(
                        """
                        select kvalitetsvurdering #>> '{omgjortIKlageomgangen,feilILovtolkningen,registreringshjemler,0}'
                        from saksdata where id = :id
                        """.trimIndent(),
                    ).setParameter("id", id)
                    .singleResult
            storedId shouldBe expectedName
        }

        private fun saksdata(
            sakstype: Type,
            kvalitetsvurdering: Kvalitetsvurdering? = null,
        ) = Saksdata(
            kabalReference = UUID.randomUUID(),
            sakstype = sakstype,
            ytelse = Ytelse.SYK_SYK,
            mottattKlageinstans = LocalDate.of(2026, 11, 1),
            sakITrygderettenOpprettet = null,
            tilknyttetEnhetId = null,
            tilbakekreving = null,
            created = createdTimestamp,
            modified = createdTimestamp,
            kvalitetsvurdering = kvalitetsvurdering,
            trygderettenSaksnummer = "1234",
        )

        companion object {
            private val createdTimestamp = LocalDateTime.of(2020, 1, 1, 0, 0)

            @Container
            @JvmStatic
            val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")

            @JvmStatic
            @DynamicPropertySource
            fun databaseProperties(registry: DynamicPropertyRegistry) {
                registry.add("spring.datasource.url") { postgres.jdbcUrl }
                registry.add("spring.datasource.username") { postgres.username }
                registry.add("spring.datasource.password") { postgres.password }
            }
        }
    }
