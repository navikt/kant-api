package no.nav.klage.kant.saksdata

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class SaksdataService(
    private val repository: SaksdataRepository,
) {
    fun list(
        page: Int,
        size: Int,
    ): SaksdataPage {
        val result = repository.findAll(PageRequest.of(page, size, Sort.by("id")))
        return SaksdataPage(
            content = result.content.map { it.toResponse() },
            page =
                PageMetadata(
                    size = result.size,
                    number = result.number,
                    totalElements = result.totalElements,
                    totalPages = result.totalPages,
                ),
        )
    }

    fun get(id: UUID): SaksdataResponse = find(id).toResponse()

    @Transactional
    fun create(request: SaksdataRequest): SaksdataResponse =
        repository
            .saveAndFlush(
                Saksdata(
                    kabalReference = request.kabalReference,
                    sakstype = request.sakstype,
                ),
            ).toResponse()

    @Transactional
    fun update(
        id: UUID,
        request: SaksdataRequest,
    ): SaksdataResponse {
        val entity = find(id)
        entity.kabalReference = request.kabalReference
        entity.sakstype = request.sakstype
        return repository.saveAndFlush(entity).toResponse()
    }

    @Transactional
    fun delete(id: UUID) {
        repository.delete(find(id))
        repository.flush()
    }

    private fun find(id: UUID): Saksdata = repository.findById(id).orElseThrow { SaksdataNotFoundException() }
}

class SaksdataNotFoundException : RuntimeException("Saksdata not found")
