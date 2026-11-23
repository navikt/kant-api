package no.nav.klage.kant.services

import io.kotest.matchers.shouldBe
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeKvalitetsvurderingV1Response
import no.nav.klage.kant.domain.saksdata.SaksdataAvslutningRequest
import no.nav.klage.kant.domain.saksdata.SaksdataRequest
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.Utfall
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration::class)
@Import(SaksdataService::class)
@Testcontainers
class SaksdataConcurrencyTest
    @Autowired
    constructor(
        private val service: SaksdataService,
        private val transactionManager: PlatformTransactionManager,
        private val jdbcTemplate: JdbcTemplate,
    ) {
        @ParameterizedTest
        @EnumSource(Mutation::class)
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        fun `mutation queued behind finalization observes the committed closed state`(mutation: Mutation) {
            val id = createOpenSaksdata()
            val completionWritten = CountDownLatch(1)
            val releaseFinalization = CountDownLatch(1)
            val mutationAttempting = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            val transactionTemplate =
                TransactionTemplate(transactionManager).apply {
                    isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
                }

            try {
                val finalizer =
                    executor.submit {
                        transactionTemplate.executeWithoutResult {
                            service.finalize(id = id, request = finalizationRequest())
                            completionWritten.countDown()
                            check(releaseFinalization.await(30, SECONDS)) {
                                "Timed out waiting to release the finalization transaction"
                            }
                        }
                    }
                check(completionWritten.await(10, SECONDS)) { "Finalization did not write before timeout" }

                val updater =
                    executor.submit {
                        transactionTemplate.execute {
                            jdbcTemplate.execute("SET LOCAL application_name = 'kant-saksdata-concurrency'")
                            mutationAttempting.countDown()
                            when (mutation) {
                                Mutation.ASSESSMENT_UPDATE -> {
                                    service.updateKvalitetsvurdering(
                                        id = id,
                                        request = AnkeKvalitetsvurderingV1(),
                                    )
                                }

                                Mutation.FINALIZATION -> {
                                    service.finalize(id = id, request = finalizationRequest())
                                }
                            }
                        }
                    }
                check(mutationAttempting.await(5, SECONDS)) { "Mutation did not start before timeout" }
                awaitDatabaseLockWait()

                // Release the uncommitted finalization only after PostgreSQL reports the competing backend is blocked.
                releaseFinalization.countDown()
                finalizer.get(30, SECONDS)
                val conflict = assertThrows<ExecutionException> { updater.get(10, SECONDS) }
                assertInstanceOf(SaksdataConflictException::class.java, conflict.cause)

                val stored = service.get(id)
                stored.avsluttetAvSaksbehandler shouldBe finalizationRequest().avsluttetAvSaksbehandler
                stored.utfall shouldBe Utfall.MEDHOLD
                val assessment = stored.kvalitetsvurdering as AnkeKvalitetsvurderingV1Response
                assessment.omgjortIKlageomgangen shouldBe completedAssessment.omgjortIKlageomgangen
                assessment.saksbehandlingsfeilIKlageomgangen shouldBe completedAssessment.saksbehandlingsfeilIKlageomgangen
            } finally {
                releaseFinalization.countDown()
                executor.shutdownNow()
                executor.awaitTermination(5, SECONDS)
            }
        }

        private fun createOpenSaksdata(): UUID {
            val created =
                service.create(
                    SaksdataRequest(
                        kabalReference = UUID.randomUUID(),
                        sakstype = Type.ANKE_ETTER_2027,
                        ytelse = Ytelse.SYK_SYK,
                        trygderettenSaksnummer = "1234",
                    ),
                )
            service.updateKvalitetsvurdering(id = created.id, request = completedAssessment)
            return created.id
        }

        private fun awaitDatabaseLockWait() {
            val deadline = System.nanoTime() + SECONDS.toNanos(10)
            while (System.nanoTime() < deadline) {
                val waiting =
                    jdbcTemplate.queryForObject(
                        """
                        select count(*)
                        from pg_stat_activity
                        where datname = current_database()
                          and application_name = 'kant-saksdata-concurrency'
                          and wait_event_type = 'Lock'
                        """.trimIndent(),
                        Int::class.java,
                    ) ?: 0
                if (waiting > 0) return
                Thread.sleep(20L)
            }
            error("Mutation backend did not enter a database lock wait")
        }

        private fun finalizationRequest() =
            SaksdataAvslutningRequest(
                utfall = Utfall.MEDHOLD,
                registreringshjemler = emptySet(),
                avsluttetAvSaksbehandler = LocalDateTime.of(2026, 11, 16, 12, 0),
                tilknyttetEnhetId = "4291",
                tilbakekreving = false,
                mottattKlageinstans = LocalDate.of(2026, 10, 16),
            )

        enum class Mutation {
            ASSESSMENT_UPDATE,
            FINALIZATION,
        }

        companion object {
            private val completedAssessment =
                AnkeKvalitetsvurderingV1(
                    omgjortIKlageomgangen =
                        AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                            vedtaketBurdeVaertOmgjortIKlageomgangen = false,
                        ),
                )

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
