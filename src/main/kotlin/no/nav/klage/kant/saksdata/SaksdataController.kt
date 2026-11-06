package no.nav.klage.kant.saksdata

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

private const val MAX_PAGE_SIZE = 100L

@RestController
@RequestMapping("/api/saksdata")
@Validated
class SaksdataController(
    private val service: SaksdataService,
) {
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) size: Int,
    ): SaksdataPage = service.list(page, size)

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): SaksdataResponse = service.get(id)

    @PostMapping
    fun create(
        @Valid @RequestBody request: SaksdataRequest,
    ): ResponseEntity<SaksdataResponse> {
        val created = service.create(request)
        return ResponseEntity.created(URI("/api/saksdata/${created.id}")).body(created)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SaksdataRequest,
    ): SaksdataResponse = service.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Unit> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
