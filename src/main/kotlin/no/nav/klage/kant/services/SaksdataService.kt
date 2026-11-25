package no.nav.klage.kant.services

import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeITRKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.AnkeKvalitetsvurderingV1
import no.nav.klage.kant.domain.kvalitetsvurdering.Kvalitetsvurdering
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingResponse
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingValidationErrors
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingValidationException
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingValidator
import no.nav.klage.kant.domain.kvalitetsvurdering.toKvalitetsvurderingResponse
import no.nav.klage.kant.domain.saksdata.Saksdata
import no.nav.klage.kant.domain.saksdata.SaksdataAvslutningRequest
import no.nav.klage.kant.domain.saksdata.SaksdataRequest
import no.nav.klage.kant.domain.saksdata.SaksdataResponse
import no.nav.klage.kant.domain.saksdata.SaksdataUpdateRequest
import no.nav.klage.kant.domain.saksdata.toResponse
import no.nav.klage.kant.repositories.SaksdataRepository
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class SaksdataService(
    private val repository: SaksdataRepository,
) {
    fun get(id: UUID): SaksdataResponse = findSaksdataOrThrowError(id).toResponse()

    @Transactional
    fun delete(id: UUID) {
        val entity = findForUpdate(id)
        repository.delete(entity)
    }

    fun getValidationErrors(id: UUID): KvalitetsvurderingValidationErrors {
        val entity = findSaksdataOrThrowError(id)
        return KvalitetsvurderingValidationErrors(
            validationErrors =
                try {
                    KvalitetsvurderingValidator.validate(
                        sakstype = entity.sakstype,
                        kvalitetsvurdering = entity.kvalitetsvurdering,
                        registreringshjemler = entity.registreringshjemler.orEmpty(),
                    )
                    emptyList()
                } catch (exception: KvalitetsvurderingValidationException) {
                    exception.errors
                },
        )
    }

    @Transactional
    fun create(input: SaksdataRequest): SaksdataResponse {
        val kvalitetsvurdering =
            when (input.sakstype) {
                Type.ANKE_FOER_2027, Type.ANKE_ETTER_2027 -> AnkeKvalitetsvurderingV1()

                Type.ANKE_I_TRYGDERETTEN_FOER_2027, Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> AnkeITRKvalitetsvurderingV1()

                Type.BEGJAERING_OM_GJENOPPTAK_FOER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_ETTER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_I_TRYGDERETTEN_FOER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_I_TRYGDERETTEN_ETTER_2027,
                -> null

                else -> throw InvalidSakstypeException()
            }
        val existingEntity = findSaksdataByKabalReference(id = input.kabalReference)
        if (existingEntity != null) return existingEntity.toResponse()
        return repository
            .saveAndFlush(
                Saksdata(
                    kabalReference = input.kabalReference,
                    sakstype = input.sakstype,
                    ytelse = input.ytelse,
                    mottattKlageinstans = null,
                    sakITrygderettenOpprettet = null,
                    tilknyttetEnhetId = null,
                    tilbakekreving = null,
                    kvalitetsvurdering = kvalitetsvurdering,
                    trygderettenSaksnummer = input.trygderettenSaksnummer,
                ),
            ).toResponse()
    }

    @Transactional
    fun update(
        id: UUID,
        request: SaksdataUpdateRequest,
    ): SaksdataResponse {
        val entity = findForUpdate(id)
        entity.registreringshjemler = request.registreringshjemmelIdSet.map { Registreringshjemmel.of(it) }.toMutableSet()
        return repository.saveAndFlush(entity).toResponse()
    }

    @Transactional
    fun updateKvalitetsvurdering(
        id: UUID,
        request: Kvalitetsvurdering,
    ): SaksdataResponse {
        val entity = findForUpdate(id)
        val matchesSakstype =
            when (entity.sakstype) {
                Type.ANKE_FOER_2027, Type.ANKE_ETTER_2027 -> request is AnkeKvalitetsvurderingV1
                Type.ANKE_I_TRYGDERETTEN_FOER_2027, Type.ANKE_I_TRYGDERETTEN_ETTER_2027 -> request is AnkeITRKvalitetsvurderingV1
                else -> false
            }
        if (!matchesSakstype) throw InvalidKvalitetsvurderingTypeException()
        entity.kvalitetsvurdering = request
        return repository.saveAndFlush(entity).toResponse()
    }

    @Transactional
    fun finalize(
        id: UUID,
        request: SaksdataAvslutningRequest,
    ): SaksdataResponse {
        val entity = findForUpdate(id)

        if (entity.isAvsluttetAvSaksbehandler()) return entity.toResponse()

        val hjemmelSet = request.registreringshjemmelIdSet.map { Registreringshjemmel.of(it) }.toMutableSet()

        KvalitetsvurderingValidator.validate(
            sakstype = entity.sakstype,
            kvalitetsvurdering = entity.kvalitetsvurdering,
            registreringshjemler = hjemmelSet,
        )

        if (entity.sakstype in
            listOf(
                Type.ANKE_FOER_2027,
                Type.ANKE_ETTER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_FOER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_ETTER_2027,
            )
        ) {
            if (request.mottattKlageinstans == null) {
                throw SaksdataInvalidException("Krever mottattKlageinstans for å fullføre sak")
            }
        } else if (entity.sakstype in
            listOf(
                Type.ANKE_I_TRYGDERETTEN_FOER_2027,
                Type.ANKE_I_TRYGDERETTEN_ETTER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_I_TRYGDERETTEN_FOER_2027,
                Type.BEGJAERING_OM_GJENOPPTAK_I_TRYGDERETTEN_ETTER_2027,
            )
        ) {
            if (request.sakITrygderettenOpprettet == null) {
                throw SaksdataInvalidException("Krever sakITrygderettenOpprettet for å fullføre anke")
            }
        }

        entity.utfall = request.utfall
        entity.registreringshjemler = hjemmelSet
        entity.avsluttetAvSaksbehandler = request.avsluttetAvSaksbehandler
        entity.sakITrygderettenOpprettet = request.sakITrygderettenOpprettet
        entity.mottattKlageinstans = request.mottattKlageinstans
        entity.tilknyttetEnhetId = request.tilknyttetEnhetId
        entity.tilbakekreving = request.tilbakekreving
        return repository.saveAndFlush(entity).toResponse()
    }

    private fun findSaksdataOrThrowError(id: UUID): Saksdata = repository.findById(id).orElseThrow { SaksdataNotFoundException() }

    private fun findSaksdataByKabalReference(id: UUID): Saksdata? = repository.findByKabalReference(id)

    private fun findForUpdate(id: UUID): Saksdata {
        val entity = repository.findForUpdate(id) ?: throw SaksdataNotFoundException()
        if (entity.isAvsluttetAvSaksbehandler()) throw SaksdataConflictException("Saksdata is already finalized")
        return entity
    }

    fun getKvalitetsvurdering(id: UUID): KvalitetsvurderingResponse? = findSaksdataOrThrowError(id).toKvalitetsvurderingResponse()
}

class InvalidSakstypeException :
    RuntimeException(
        "Saksdata can only be created for ANKE_ETTER_2027 and ANKE_I_TRYGDERETTEN_ETTER_2027",
    )

class SaksdataInvalidException(
    message: String,
) : RuntimeException(message)

class InvalidKvalitetsvurderingTypeException : RuntimeException("Kvalitetsvurdering type does not match sakstype")

class SaksdataNotFoundException : RuntimeException("Saksdata not found")

class SaksdataConflictException(
    message: String,
) : RuntimeException(message)
