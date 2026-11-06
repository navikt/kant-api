package no.nav.klage.kant

import io.kotest.assertions.throwables.shouldThrow
import no.nav.klage.auth.AzureAuthProperties
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class SecurityConfigurationTest {
    @Test
    fun `fails closed when any required Azure configuration is missing`() {
        val properties =
            mapOf(
                AzureAuthProperties.ISSUER to "https://issuer.invalid",
                AzureAuthProperties.JWKS_URI to "https://issuer.invalid/jwks",
                AzureAuthProperties.CLIENT_ID to "kant-api",
                AzureAuthProperties.PRE_AUTHORIZED_APPS to """[{"clientId":"kabal-api"}]""",
            )
        properties.keys.forEach { missing ->
            val environment = MockEnvironment()
            properties.filterKeys { it != missing }.forEach { (key, value) ->
                environment.setProperty(key, value)
            }
            shouldThrow<IllegalStateException> { AzureAuthProperties.from(environment) }
        }
    }

    @Test
    fun `fails closed for empty malformed or missing preauthorized client identifiers`() {
        listOf("", "[]", "invalid", """[{"clientId":""}]""", "[{}]").forEach { value ->
            val environment =
                MockEnvironment()
                    .withProperty(AzureAuthProperties.ISSUER, "https://issuer.invalid")
                    .withProperty(AzureAuthProperties.JWKS_URI, "https://issuer.invalid/jwks")
                    .withProperty(AzureAuthProperties.CLIENT_ID, "kant-api")
                    .withProperty(AzureAuthProperties.PRE_AUTHORIZED_APPS, value)
            shouldThrow<IllegalStateException> { AzureAuthProperties.from(environment) }
        }
    }
}
