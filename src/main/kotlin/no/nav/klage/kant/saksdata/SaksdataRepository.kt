package no.nav.klage.kant.saksdata

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SaksdataRepository : JpaRepository<Saksdata, UUID>
