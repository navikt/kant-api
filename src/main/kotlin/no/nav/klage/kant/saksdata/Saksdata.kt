package no.nav.klage.kant.saksdata

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.TypeConverter
import java.util.UUID

@Entity
@Table(name = "saksdata")
class Saksdata(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "kabal_reference", nullable = false)
    var kabalReference: UUID,
    @Convert(converter = TypeConverter::class)
    @Column(name = "sakstype", nullable = false, length = 16)
    var sakstype: Type,
)
