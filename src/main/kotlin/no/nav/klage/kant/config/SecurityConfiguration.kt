package no.nav.klage.kant.config

import no.nav.klage.auth.AzureAuthProperties
import no.nav.klage.auth.azureJwtDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        environment: Environment,
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(
                        HttpMethod.GET,
                        "/internal/health",
                        "/internal/health/**",
                        "/internal/prometheus",
                    ).permitAll()
                it.anyRequest().authenticated()
            }.oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { it.decoder(azureJwtDecoder(AzureAuthProperties.from(environment))) }
                resourceServer.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.contentType = "application/problem+json"
                    response.setHeader("WWW-Authenticate", "Bearer")
                    response.writer.write(
                        """{"type":"about:blank","title":"Unauthorized","status":401,""" +
                            """"detail":"Authentication required"}""",
                    )
                }
                resourceServer.accessDeniedHandler { _, response, _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                    response.contentType = "application/problem+json"
                    response.writer.write(
                        """{"type":"about:blank","title":"Forbidden","status":403,"detail":"Access denied"}""",
                    )
                }
            }.build()
}
