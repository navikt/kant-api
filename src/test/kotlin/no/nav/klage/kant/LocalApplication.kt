package no.nav.klage.kant

import com.nimbusds.oauth2.sdk.TokenRequest
import no.nav.klage.auth.AzureAuthProperties
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.extensions.clientIdAsString
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.slf4j.LoggerFactory
import org.springframework.boot.runApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    val azure =
        MockOAuth2Server(
            OAuth2Config(
                tokenCallbacks =
                    setOf(
                        object : DefaultOAuth2TokenCallback(
                            issuerId = "azuread",
                            audience = listOf("kant-api"),
                        ) {
                            override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> =
                                mapOf("azp" to tokenRequest.clientIdAsString())
                        },
                    ),
            ),
        ).apply { start() }
    Runtime.getRuntime().addShutdownHook(Thread { azure.shutdown() })
    LoggerFactory.getLogger("LocalApplication").info(
        "Local token endpoint: {}",
        azure.tokenEndpointUrl("azuread"),
    )
    runApplication<Application>(*args) {
        setAdditionalProfiles("local")
        setDefaultProperties(
            mapOf(
                "server.address" to "127.0.0.1",
                AzureAuthProperties.ISSUER to azure.issuerUrl("azuread").toString(),
                AzureAuthProperties.JWKS_URI to azure.jwksUrl("azuread").toString(),
                AzureAuthProperties.CLIENT_ID to "kant-api",
                AzureAuthProperties.PRE_AUTHORIZED_APPS to
                    """[{"name":"local:klage:kant-frontend","clientId":"kant-frontend"},""" +
                    """{"name":"local:klage:kabal-api","clientId":"kabal-api"}]""",
            ),
        )
    }
}
