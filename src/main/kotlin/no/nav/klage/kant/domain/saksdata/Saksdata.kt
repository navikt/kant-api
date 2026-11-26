package no.nav.klage.kant.domain.saksdata

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import no.nav.klage.kant.domain.kvalitetsvurdering.Kvalitetsvurdering
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.TypeConverter
import no.nav.klage.kodeverk.Utfall
import no.nav.klage.kodeverk.UtfallConverter
import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import no.nav.klage.kodeverk.hjemmel.RegistreringshjemmelConverter
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.kodeverk.ytelse.YtelseConverter
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.generator.EventType
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "saksdata")
@Suppress("LongParameterList")
class Saksdata(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "kabal_reference", nullable = false)
    var kabalReference: UUID,
    @Convert(converter = TypeConverter::class)
    @Column(name = "sakstype_id", nullable = false)
    var sakstype: Type,
    @Column(name = "ytelse_id")
    @Convert(converter = YtelseConverter::class)
    var ytelse: Ytelse,
    @Column(name = "dato_mottatt_klageinstans")
    var mottattKlageinstans: LocalDate?,
    @Column(name = "sak_i_trygderetten_opprettet")
    var sakITrygderettenOpprettet: LocalDate?,
    @Column(name = "utfall_id")
    @Convert(converter = UtfallConverter::class)
    var utfall: Utfall? = null,
    @ElementCollection(targetClass = Registreringshjemmel::class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "registreringshjemmel",
        joinColumns = [JoinColumn(name = "saksdata_id", referencedColumnName = "id", nullable = false)],
    )
    @Convert(converter = RegistreringshjemmelConverter::class)
    @Column(name = "id")
    var registreringshjemler: Set<Registreringshjemmel>? = null,
    @Column(name = "tilknyttet_enhet")
    var tilknyttetEnhetId: String?,
    @Column(name = "dato_saksdata_avsluttet_av_saksbehandler")
    var avsluttetAvSaksbehandler: LocalDateTime? = null,
    @Column(name = "tilbakekreving")
    var tilbakekreving: Boolean?,
    @Column(name = "created")
    val created: LocalDateTime = LocalDateTime.now(),
    @Generated(event = [EventType.UPDATE])
    @Column(name = "modified", updatable = false)
    var modified: LocalDateTime = created,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kvalitetsvurdering", columnDefinition = "jsonb")
    var kvalitetsvurdering: Kvalitetsvurdering? = null,
    @Column(name = "trygderetten_saksnummer", nullable = false)
    var trygderettenSaksnummer: String?,
) {
    fun isAvsluttetAvSaksbehandler() = avsluttetAvSaksbehandler != null
}
