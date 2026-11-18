package no.nav.klage.kant.domain.kvalitetsvurdering

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = AnkeKvalitetsvurderingV1::class, name = "ANKE_V1"),
    JsonSubTypes.Type(value = AnkeITRKvalitetsvurderingV1::class, name = "ANKE_ITR_V1"),
)
sealed interface Kvalitetsvurdering
