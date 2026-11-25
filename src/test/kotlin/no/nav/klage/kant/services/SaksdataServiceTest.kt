package no.nav.klage.kant.services

import io.kotest.matchers.shouldBe
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeITRKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeITRKvalitetsvurderingV1Response
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeKvalitetsvurderingV1Response
import no.nav.klage.kant.domain.kvalitetsvurdering.Kvalitetsvurdering
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingValidationError
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingValidationErrors
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingValidationException
import no.nav.klage.kant.domain.kvalitetsvurdering.toKvalitetsvurderingResponse
import no.nav.klage.kant.domain.saksdata.Saksdata
import no.nav.klage.kant.domain.saksdata.SaksdataAvslutningRequest
import no.nav.klage.kant.domain.saksdata.SaksdataRequest
import no.nav.klage.kant.domain.saksdata.toResponse
import no.nav.klage.kant.repositories.SaksdataRepository
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.Utfall
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class SaksdataServiceTest {
    private val repository = mock(SaksdataRepository::class.java)
    private val service = SaksdataService(repository)
    private val reference = UUID.randomUUID()
    private val mottatt = LocalDate.of(2026, 11, 1)

    companion object {
        @JvmStatic
        fun supportedTypes(): List<Type> =
            listOf(
                Type.ANKE_FOER_2027,
                Type.ANKE_ETTER_2027,
                Type.ANKE_I_TRYGDERETTEN_FOER_2027,
                Type.ANKE_I_TRYGDERETTEN_ETTER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_FOER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_ETTER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_I_TRYGDERETTEN_FOER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_I_TRYGDERETTEN_ETTER_2027,
            )

        @JvmStatic
        fun unsupportedTypes(): List<Type> = Type.entries.filterNot { it in supportedTypes() }

        @JvmStatic
        fun supportedTypesWithKvalitetsvurdering(): List<Type> =
            listOf(
                Type.ANKE_FOER_2027,
                Type.ANKE_ETTER_2027,
                Type.ANKE_I_TRYGDERETTEN_FOER_2027,
                Type.ANKE_I_TRYGDERETTEN_ETTER_2027,
            )

        @JvmStatic
        fun supportedTypesWithoutKvalitetsvurdering(): List<Type> =
            listOf(
                Type.BEGJAERING_OM_GJENOPPTAK_FOER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_ETTER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_I_TRYGDERETTEN_FOER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_I_TRYGDERETTEN_ETTER_2027,
            )
    }

    @ParameterizedTest
    @MethodSource("supportedTypesWithKvalitetsvurdering")
    fun `creates open saksdata with received date`(sakstype: Type) {
        `when`(repository.findByKabalReference(reference)).thenReturn(null)
        lateinit var saved: Saksdata
        `when`(repository.saveAndFlush(any(Saksdata::class.java))).thenAnswer {
            it.getArgument<Saksdata>(0).also { entity -> saved = entity }
        }

        val response =
            service.create(
                SaksdataRequest(
                    kabalReference = reference,
                    sakstype = sakstype,
                    ytelse = Ytelse.SYK_SYK,
                    trygderettenSaksnummer = "1234",
                ),
            )

        response.kabalReference shouldBe reference
        response.sakstype shouldBe sakstype
        response.ytelse shouldBe Ytelse.SYK_SYK
        response.mottattKlageinstans shouldBe null
        response.avsluttetAvSaksbehandler shouldBe null
        response.sakITrygderettenOpprettet shouldBe null
        response.tilknyttetEnhetId shouldBe null
        response.tilbakekreving shouldBe null
        response.kvalitetsvurdering shouldBe saved.toKvalitetsvurderingResponse()
    }

    @ParameterizedTest
    @MethodSource("supportedTypesWithoutKvalitetsvurdering")
    fun `creates open saksdata without kvalitetsvurdering with received date`(sakstype: Type) {
        `when`(repository.findByKabalReference(reference)).thenReturn(null)
        lateinit var saved: Saksdata
        `when`(repository.saveAndFlush(any(Saksdata::class.java))).thenAnswer {
            it.getArgument<Saksdata>(0).also { entity -> saved = entity }
        }

        val response =
            service.create(
                SaksdataRequest(
                    kabalReference = reference,
                    sakstype = sakstype,
                    ytelse = Ytelse.SYK_SYK,
                    trygderettenSaksnummer = "1234",
                ),
            )

        response.kabalReference shouldBe reference
        response.sakstype shouldBe sakstype
        response.ytelse shouldBe Ytelse.SYK_SYK
        response.mottattKlageinstans shouldBe null
        response.avsluttetAvSaksbehandler shouldBe null
        response.sakITrygderettenOpprettet shouldBe null
        response.tilknyttetEnhetId shouldBe null
        response.tilbakekreving shouldBe null
        response.kvalitetsvurdering shouldBe saved.toKvalitetsvurderingResponse()
    }

    @ParameterizedTest
    @MethodSource("unsupportedTypes")
    fun `rejects unsupported types before accessing repository`(sakstype: Type) {
        val request =
            SaksdataRequest(
                kabalReference = reference,
                sakstype = sakstype,
                ytelse = Ytelse.SYK_SYK,
                trygderettenSaksnummer = "1234",
            )

        assertThrows<InvalidSakstypeException> { service.create(request) }

        verifyNoInteractions(repository)
    }

    @Test
    fun `repeated creation returns existing saksdata without saving`() {
        val entity = saksdata()
        `when`(repository.findByKabalReference(reference)).thenReturn(entity)

        val response =
            service.create(
                SaksdataRequest(
                    kabalReference = reference,
                    sakstype = entity.sakstype,
                    ytelse = entity.ytelse,
                    trygderettenSaksnummer = "1234",
                ),
            )

        response.id shouldBe entity.id
        verify(repository, never()).save(any(Saksdata::class.java))
    }

    @Test
    fun `returns stored saksdata and rejects missing id`() {
        val entity = saksdata()
        `when`(repository.findById(entity.id)).thenReturn(Optional.of(entity))
        service.get(entity.id).mottattKlageinstans shouldBe mottatt

        val missingId = UUID.randomUUID()
        `when`(repository.findById(missingId)).thenReturn(Optional.empty())
        `when`(repository.findForUpdate(missingId)).thenReturn(null)
        assertThrows<SaksdataNotFoundException> { service.get(missingId) }
    }

    @ParameterizedTest
    @MethodSource("supportedTypesWithKvalitetsvurdering")
    fun `assessment responses include type and saksdata timestamps`(sakstype: Type) {
        val created = LocalDateTime.of(2026, 11, 1, 9, 15)
        val modified = LocalDateTime.of(2026, 11, 20, 14, 30)
        val assessment = nonDefaultKvalitetsvurdering(sakstype)
        val entity =
            saksdata(sakstype, created = created, modified = modified).apply {
                kvalitetsvurdering = assessment
            }
        `when`(repository.findById(entity.id)).thenReturn(Optional.of(entity))
        val expected =
            when (assessment) {
                is AnkeKvalitetsvurderingV1 -> {
                    AnkeKvalitetsvurderingV1Response(
                        omgjortIKlageomgangen = assessment.omgjortIKlageomgangen,
                        saksbehandlingsfeilIKlageomgangen = assessment.saksbehandlingsfeilIKlageomgangen,
                        created = created,
                        modified = modified,
                    )
                }

                is AnkeITRKvalitetsvurderingV1 -> {
                    AnkeITRKvalitetsvurderingV1Response(
                        omgjortIAnkeomgangen = assessment.omgjortIAnkeomgangen,
                        rettensSammensetning = assessment.rettensSammensetning,
                        kjennelsensBetydningOgLaeringsverdi = assessment.kjennelsensBetydningOgLaeringsverdi,
                        created = created,
                        modified = modified,
                    )
                }
            }

        service.get(entity.id).kvalitetsvurdering shouldBe expected
        service.getKvalitetsvurdering(entity.id) shouldBe expected
    }

    @Test
    fun `standalone assessment response preserves null`() {
        val entity = saksdata()
        `when`(repository.findById(entity.id)).thenReturn(Optional.of(entity))

        service.getKvalitetsvurdering(entity.id) shouldBe null
    }

    @Test
    fun `validation errors uses persisted assessment and does not modify saksdata`() {
        val entity =
            saksdata().apply {
                kvalitetsvurdering = invalidDraft(Type.ANKE_ETTER_2027)
            }
        `when`(repository.findById(entity.id)).thenReturn(Optional.of(entity))
        val before = entity.toResponse()

        service.getValidationErrors(entity.id) shouldBe
            KvalitetsvurderingValidationErrors(
                validationErrors =
                    listOf(
                        KvalitetsvurderingValidationError(
                            field = "kvalitetsvurdering.omgjortIKlageomgangen.vedtaketBurdeVaertOmgjortIKlageomgangen",
                            reason = "Må besvares før fullføring",
                        ),
                        KvalitetsvurderingValidationError(
                            field = "kvalitetsvurdering.omgjortIKlageomgangen.feilILovtolkningen.registreringshjemler",
                            reason = "Må velge minst en registreringshjemmel",
                        ),
                    ),
            )

        entity.toResponse() shouldBe before
        verify(repository).findById(entity.id)
        verify(repository, never()).findForUpdate(entity.id)
        verify(repository, never()).save(any(Saksdata::class.java))
        verify(repository, never()).saveAndFlush(any(Saksdata::class.java))
    }

    @Test
    fun `validation errors accepts completed saksdata and returns empty list`() {
        val entity =
            saksdata().apply {
                kvalitetsvurdering = validAnkeAssessment()
                avsluttetAvSaksbehandler = LocalDateTime.of(2026, 11, 16, 12, 0)
            }
        `when`(repository.findById(entity.id)).thenReturn(Optional.of(entity))
        val before = entity.toResponse()

        service.getValidationErrors(entity.id) shouldBe
            KvalitetsvurderingValidationErrors(
                validationErrors = emptyList(),
            )

        entity.toResponse() shouldBe before
        verify(repository).findById(entity.id)
        verify(repository, never()).findForUpdate(entity.id)
        verify(repository, never()).save(any(Saksdata::class.java))
        verify(repository, never()).saveAndFlush(any(Saksdata::class.java))
    }

    @Test
    fun `validation errors rejects missing saksdata`() {
        val missingId = UUID.randomUUID()
        `when`(repository.findById(missingId)).thenReturn(Optional.empty())

        assertThrows<SaksdataNotFoundException> { service.getValidationErrors(missingId) }
    }

    @Test
    fun `finalization stores completion fields including false and optional date`() {
        listOf(null, mottatt.plusDays(1)).forEach { opprettet ->
            val entity = saksdata()
            entity.kvalitetsvurdering = validAnkeAssessment()
            prepareUpdate(entity)
            val request = avslutning().copy(sakITrygderettenOpprettet = opprettet)

            val response = service.finalize(id = entity.id, request = request)

            response.utfall shouldBe request.utfall
            response.registreringshjemler shouldBe request.registreringshjemmelIdSet.map { Registreringshjemmel.of(it) }.toSet()
            response.avsluttetAvSaksbehandler shouldBe request.avsluttetAvSaksbehandler
            response.sakITrygderettenOpprettet shouldBe opprettet
            response.tilknyttetEnhetId shouldBe request.tilknyttetEnhetId
            response.tilbakekreving shouldBe false
            response.mottattKlageinstans shouldBe request.mottattKlageinstans
            verify(repository).findForUpdate(entity.id)
        }
    }

    @Test
    fun `finalization accepts assessment homes present only in incoming request`() {
        val entity = saksdata()
        entity.kvalitetsvurdering = ankeAssessmentWithSelectedHome()
        prepareUpdate(entity)

        val response = service.finalize(id = entity.id, request = avslutning())

        response.registreringshjemler shouldBe setOf(Registreringshjemmel.FTRL_1_3)
        verify(repository).findForUpdate(entity.id)
    }

    @Test
    fun `invalid finalization leaves entity unchanged when assessment home exists only in stored homes`() {
        val entity =
            saksdata().apply {
                kvalitetsvurdering = ankeAssessmentWithSelectedHome()
                registreringshjemler = setOf(Registreringshjemmel.FTRL_1_3)
            }
        prepareUpdate(entity)
        val before = entity.toResponse()

        assertThrows<KvalitetsvurderingValidationException> {
            service.finalize(
                id = entity.id,
                request = avslutning().copy(registreringshjemmelIdSet = emptySet()),
            )
        }

        entity.toResponse() shouldBe before
        verify(repository, never()).saveAndFlush(entity)
        verify(repository).findForUpdate(entity.id)
    }

    @Test
    fun `closed saksdata rejects finalization without saving`() {
        val entity =
            saksdata().apply {
                avsluttetAvSaksbehandler = avslutning().avsluttetAvSaksbehandler
                utfall = Utfall.STADFESTELSE
            }
        `when`(repository.findForUpdate(entity.id)).thenReturn(entity)

        assertThrows<SaksdataConflictException> {
            service.finalize(id = entity.id, request = avslutning())
        }

        entity.utfall shouldBe Utfall.STADFESTELSE
        verify(repository, never()).saveAndFlush(any(Saksdata::class.java))
    }

    @Test
    fun `delete removes open saksdata after locking it for update`() {
        val entity = saksdata()
        `when`(repository.findForUpdate(entity.id)).thenReturn(entity)

        service.delete(entity.id)

        verify(repository).findForUpdate(entity.id)
        verify(repository).delete(entity)
    }

    @Test
    fun `delete rejects finalized saksdata without deleting it`() {
        val entity =
            saksdata().apply {
                avsluttetAvSaksbehandler = avslutning().avsluttetAvSaksbehandler
            }
        `when`(repository.findForUpdate(entity.id)).thenReturn(entity)

        assertThrows<SaksdataConflictException> { service.delete(entity.id) }

        verify(repository).findForUpdate(entity.id)
        verify(repository, never()).delete(any(Saksdata::class.java))
    }

    @Test
    fun `delete rejects missing saksdata without deleting it`() {
        val missingId = UUID.randomUUID()
        `when`(repository.findForUpdate(missingId)).thenReturn(null)

        assertThrows<SaksdataNotFoundException> { service.delete(missingId) }

        verify(repository).findForUpdate(missingId)
        verify(repository, never()).delete(any(Saksdata::class.java))
    }

    @ParameterizedTest
    @MethodSource("supportedTypesWithKvalitetsvurdering")
    fun `quality assessment update replaces the existing assessment`(sakstype: Type) {
        val entity =
            saksdata(sakstype).apply {
                kvalitetsvurdering = nonDefaultKvalitetsvurdering(sakstype)
            }
        prepareUpdate(entity)
        val requested = defaultKvalitetsvurdering(sakstype)

        val response = service.updateKvalitetsvurdering(id = entity.id, request = requested)

        response.kvalitetsvurdering shouldBe entity.toKvalitetsvurderingResponse()
        response.kabalReference shouldBe reference
        response.sakstype shouldBe sakstype
        response.mottattKlageinstans shouldBe mottatt
        entity.kvalitetsvurdering shouldBe requested
        verify(repository).saveAndFlush(entity)
        verify(repository).findForUpdate(entity.id)
    }

    @ParameterizedTest
    @MethodSource("supportedTypesWithKvalitetsvurdering")
    fun `quality assessment update rejects mismatched variants without saving`(sakstype: Type) {
        val entity =
            saksdata(sakstype).apply {
                kvalitetsvurdering = defaultKvalitetsvurdering(sakstype)
            }
        prepareUpdate(entity)
        val original = entity.kvalitetsvurdering

        assertThrows<InvalidKvalitetsvurderingTypeException> {
            service.updateKvalitetsvurdering(
                id = entity.id,
                request = defaultKvalitetsvurdering(otherSupportedType(sakstype)),
            )
        }

        entity.kvalitetsvurdering shouldBe original
        verify(repository, never()).saveAndFlush(any(Saksdata::class.java))
    }

    @ParameterizedTest
    @MethodSource("supportedTypesWithKvalitetsvurdering")
    fun `quality assessment update accepts semantically invalid drafts`(sakstype: Type) {
        val entity = saksdata(sakstype)
        prepareUpdate(entity)
        val draft = invalidDraft(sakstype)

        val response = service.updateKvalitetsvurdering(id = entity.id, request = draft)

        response.kvalitetsvurdering shouldBe entity.toKvalitetsvurderingResponse()
        entity.kvalitetsvurdering shouldBe draft
        verify(repository).saveAndFlush(entity)
    }

    @ParameterizedTest
    @MethodSource("supportedTypesWithKvalitetsvurdering")
    fun `quality assessment update rejects missing and closed saksdata without saving`(sakstype: Type) {
        val missingId = UUID.randomUUID()
        `when`(repository.findForUpdate(missingId)).thenReturn(null)
        assertThrows<SaksdataNotFoundException> {
            service.updateKvalitetsvurdering(
                id = missingId,
                request = defaultKvalitetsvurdering(sakstype),
            )
        }

        val closedEntity =
            saksdata(sakstype).apply {
                avsluttetAvSaksbehandler = avslutning().avsluttetAvSaksbehandler
                kvalitetsvurdering = defaultKvalitetsvurdering(sakstype)
            }
        `when`(repository.findForUpdate(closedEntity.id)).thenReturn(closedEntity)
        assertThrows<SaksdataConflictException> {
            service.updateKvalitetsvurdering(
                id = closedEntity.id,
                request = defaultKvalitetsvurdering(sakstype),
            )
        }

        verify(repository, never()).saveAndFlush(any(Saksdata::class.java))
    }

    private fun prepareUpdate(entity: Saksdata) {
        `when`(repository.findForUpdate(entity.id)).thenReturn(entity)
        `when`(repository.saveAndFlush(entity)).thenReturn(entity)
    }

    private fun validAnkeAssessment(): AnkeKvalitetsvurderingV1 =
        AnkeKvalitetsvurderingV1(
            omgjortIKlageomgangen =
                AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                    vedtaketBurdeVaertOmgjortIKlageomgangen = false,
                ),
        )

    private fun ankeAssessmentWithSelectedHome(): AnkeKvalitetsvurderingV1 =
        AnkeKvalitetsvurderingV1(
            omgjortIKlageomgangen =
                AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                    vedtaketBurdeVaertOmgjortIKlageomgangen = true,
                    feilILovtolkningen =
                        AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                            selected = true,
                            registreringshjemler = listOf(Registreringshjemmel.FTRL_1_3),
                        ),
                ),
        )

    private fun saksdata(
        sakstype: Type = Type.ANKE_ETTER_2027,
        created: LocalDateTime = LocalDateTime.of(2026, 11, 1, 9, 15),
        modified: LocalDateTime = created,
    ): Saksdata =
        Saksdata(
            kabalReference = reference,
            sakstype = sakstype,
            ytelse = Ytelse.SYK_SYK,
            mottattKlageinstans = mottatt,
            sakITrygderettenOpprettet = null,
            tilknyttetEnhetId = null,
            tilbakekreving = null,
            created = created,
            modified = modified,
            trygderettenSaksnummer = "1234",
        )

    private fun defaultKvalitetsvurdering(sakstype: Type): Kvalitetsvurdering =
        when (sakstype) {
            Type.ANKE_FOER_2027, Type.ANKE_ETTER_2027 -> AnkeKvalitetsvurderingV1()
            Type.ANKE_I_TRYGDERETTEN_FOER_2027, Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> AnkeITRKvalitetsvurderingV1()
            else -> error("Unsupported type in test")
        }

    private fun nonDefaultKvalitetsvurdering(sakstype: Type): Kvalitetsvurdering =
        when (sakstype) {
            Type.ANKE_FOER_2027, Type.ANKE_ETTER_2027 -> {
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
                )
            }

            Type.ANKE_I_TRYGDERETTEN_FOER_2027, Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> {
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
                )
            }

            else -> {
                error("Unsupported type in test")
            }
        }

    private fun invalidDraft(sakstype: Type): Kvalitetsvurdering =
        when (sakstype) {
            Type.ANKE_FOER_2027, Type.ANKE_ETTER_2027 -> {
                AnkeKvalitetsvurderingV1(
                    omgjortIKlageomgangen =
                        AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen(
                            vedtaketBurdeVaertOmgjortIKlageomgangen = null,
                            feilILovtolkningen =
                                AnkeKvalitetsvurderingV1.OmgjortIKlageomgangen.SelectorWithRegistreringshjemler(
                                    selected = true,
                                    registreringshjemler = emptyList(),
                                ),
                        ),
                )
            }

            Type.ANKE_I_TRYGDERETTEN_FOER_2027, Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> {
                AnkeITRKvalitetsvurderingV1(
                    omgjortIAnkeomgangen =
                        AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen(
                            vedtaketBurdeVaertOmgjortIAnkeomgangen = null,
                            feilIFaktagrunnlaget =
                                AnkeITRKvalitetsvurderingV1.OmgjortIAnkeomgangen.SelectorWithRegistreringshjemler(
                                    selected = true,
                                    registreringshjemler = emptyList(),
                                ),
                        ),
                )
            }

            else -> {
                error("Unsupported type in test")
            }
        }

    private fun otherSupportedType(sakstype: Type): Type =
        when (sakstype) {
            Type.ANKE_FOER_2027, Type.ANKE_ETTER_2027 -> Type.ANKE_I_TRYGDERETTEN_ETTER_2027
            Type.ANKE_I_TRYGDERETTEN_FOER_2027, Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> Type.ANKE_ETTER_2027
            else -> error("Unsupported type in test")
        }

    private fun avslutning(): SaksdataAvslutningRequest =
        SaksdataAvslutningRequest(
            utfall = Utfall.MEDHOLD,
            registreringshjemmelIdSet = setOf(Registreringshjemmel.FTRL_1_3.id),
            avsluttetAvSaksbehandler = LocalDateTime.of(2026, 11, 16, 12, 0),
            tilknyttetEnhetId = "4291",
            tilbakekreving = false,
            mottattKlageinstans = LocalDate.of(2026, 10, 16),
        )
}
