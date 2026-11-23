package no.nav.klage.kant.api

import jakarta.validation.Valid
import no.nav.klage.kant.domain.kvalitetsvurdering.Kvalitetsvurdering
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingResponse
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingValidationErrors
import no.nav.klage.kant.domain.saksdata.SaksdataAvslutningRequest
import no.nav.klage.kant.domain.saksdata.SaksdataRequest
import no.nav.klage.kant.domain.saksdata.SaksdataResponse
import no.nav.klage.kant.services.SaksdataService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/saksdata")
@Validated
class SaksdataController(
    private val saksdataService: SaksdataService,
) {
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): SaksdataResponse = saksdataService.get(id)

    @PostMapping
    fun create(
        @Valid @RequestBody request: SaksdataRequest,
    ): SaksdataResponse = saksdataService.create(request)

    @PutMapping("/{id}/kvalitetsvurdering")
    fun updateKvalitetsvurdering(
        @PathVariable id: UUID,
        @Valid @RequestBody request: Kvalitetsvurdering,
    ): SaksdataResponse = saksdataService.updateKvalitetsvurdering(id = id, request = request)

    @GetMapping("/{id}/kvalitetsvurdering")
    fun getKvalitetsvurdering(
        @PathVariable id: UUID,
    ): KvalitetsvurderingResponse? = saksdataService.getKvalitetsvurdering(id = id)

    @PostMapping("/{id}/finalize")
    fun finalize(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SaksdataAvslutningRequest,
    ): SaksdataResponse = saksdataService.finalize(id = id, request = request)

    @GetMapping("/{id}/validationErrors")
    fun validate(
        @PathVariable id: UUID,
    ): KvalitetsvurderingValidationErrors = saksdataService.getValidationErrors(id)
}
