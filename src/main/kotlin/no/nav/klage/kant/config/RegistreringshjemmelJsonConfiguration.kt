package no.nav.klage.kant.config

import no.nav.klage.kodeverk.hjemmel.Registreringshjemmel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule

@Configuration
class RegistreringshjemmelJsonConfiguration {
    @Bean
    fun registreringshjemmelModule(): JacksonModule =
        SimpleModule("registreringshjemmel-ids").apply {
            addSerializer(Registreringshjemmel::class.java, RegistreringshjemmelSerializer())
            addDeserializer(Registreringshjemmel::class.java, RegistreringshjemmelDeserializer())
        }

    private class RegistreringshjemmelSerializer : ValueSerializer<Registreringshjemmel>() {
        override fun serialize(
            value: Registreringshjemmel,
            generator: JsonGenerator,
            context: SerializationContext,
        ) {
            generator.writeString(value.id)
        }
    }

    private class RegistreringshjemmelDeserializer : ValueDeserializer<Registreringshjemmel>() {
        override fun deserialize(
            parser: JsonParser,
            context: DeserializationContext,
        ): Registreringshjemmel {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                return context.reportInputMismatch(Registreringshjemmel::class.java, "Expected a registreringshjemmel ID string")
            }
            return try {
                Registreringshjemmel.of(parser.string)
            } catch (_: IllegalArgumentException) {
                context.reportInputMismatch(Registreringshjemmel::class.java, "Unknown registreringshjemmel ID")
            }
        }

        override fun getNullValue(context: DeserializationContext): Registreringshjemmel =
            context.reportInputMismatch(Registreringshjemmel::class.java, "Registreringshjemmel ID cannot be null")
    }
}
