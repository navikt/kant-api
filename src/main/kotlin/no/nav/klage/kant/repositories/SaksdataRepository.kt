package no.nav.klage.kant.repositories

import jakarta.persistence.LockModeType
import no.nav.klage.kant.domain.saksdata.Saksdata
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SaksdataRepository : JpaRepository<Saksdata, UUID> {
    fun findByKabalReference(kabalReference: UUID): Saksdata?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Saksdata s where s.id = :id")
    fun findForUpdate(id: UUID): Saksdata?
}
