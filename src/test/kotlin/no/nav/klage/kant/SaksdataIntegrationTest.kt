package no.nav.klage.kant

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.nimbusds.oauth2.sdk.TokenRequest
import io.kotest.matchers.shouldBe
import no.nav.klage.auth.test.MockAzure
import no.nav.klage.kant.saksdata.SaksdataRepository
import no.nav.klage.kodeverk.Type
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.KeyProvider
import no.nav.security.mock.oauth2.token.OAuth2TokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Testcontainers
class SaksdataIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var repository: SaksdataRepository

    @Autowired
    lateinit var jdbc: JdbcTemplate

    private val mapper = JsonMapper.builder().build()

    companion object {
        private val mockAzure =
            MockAzure(
                clientId = "kant-api",
                preAuthorizedClientIds = setOf("kant-frontend", "kabal-api"),
            )

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:18")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            mockAzure.registerProperties(registry)
        }
    }

    @BeforeEach
    fun cleanDatabase() {
        repository.deleteAll()
    }

    private fun protectedRequest(): MockHttpServletRequestBuilder = get("/api/saksdata")

    @Test
    fun `supports full CRUD for frontend OBO and API client credentials`() {
        val callers =
            listOf(
                "kant-frontend" to mapOf(MockAzure.NAV_IDENT_CLAIM to "TESTIDENT"),
                "kabal-api" to emptyMap(),
            )
        for ((caller, claims) in callers) {
            val token = mockAzure.token(azp = caller, claims = claims)
            val reference = UUID.randomUUID()
            val created = create(reference, Type.entries.first(), token)
            val id = created["id"].asString()
            (UUID.fromString(id) == reference) shouldBe false
            authorized(get("/api/saksdata/$id"), token)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.kabalReference").value(reference.toString()))

            val updatedReference = UUID.randomUUID()
            authorized(
                put("/api/saksdata/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(updatedReference, Type.entries.last())),
                token,
            ).andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.kabalReference").value(updatedReference.toString()))
                .andExpect(jsonPath("$.sakstype").value(Type.entries.last().name))
            authorized(delete("/api/saksdata/$id"), token)
                .andExpect(status().isNoContent)
                .andExpect(content().string(""))
            authorized(get("/api/saksdata/$id"), token).andExpect(status().isNotFound)
        }
    }

    @Test
    fun `stores every Type by kodeverk id and returns enum names without unique case constraint`() {
        val reference = UUID.randomUUID()
        Type.entries.forEach { type ->
            val created = create(reference, type)
            created["sakstype"].asString() shouldBe type.name
            val id = UUID.fromString(created["id"].asString())
            repository.findById(id).orElseThrow().sakstype shouldBe type
            jdbc.queryForObject("SELECT sakstype FROM saksdata WHERE id = ?", String::class.java, id) shouldBe type.id
            authorized(get("/api/saksdata/$id")).andExpect(jsonPath("$.sakstype").value(type.name))
        }
        repository.count() shouldBe Type.entries.size.toLong()
    }

    @Test
    fun `returns stable bounded pagination metadata sorted by id`() {
        repeat(3) { create(UUID.randomUUID(), Type.entries.first()) }
        val expected = jdbc.queryForList("SELECT id::text FROM saksdata ORDER BY id", String::class.java)
        for (page in 0..2) {
            authorized(get("/api/saksdata?page=$page&size=1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(expected[page]))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.number").value(page))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(3))
        }
        authorized(get("/api/saksdata?page=3&size=1")).andExpect(jsonPath("$.content.length()").value(0))
        authorized(get("/api/saksdata?size=100")).andExpect(status().isOk)
        authorized(get("/api/saksdata")).andExpect(jsonPath("$.page.size").value(20))
    }

    @Test
    fun `rejects malformed missing null fields and invalid pagination with sanitized problems`() {
        val reference = UUID.randomUUID()
        val invalidBodies =
            listOf(
                "{}",
                """{"kabalReference":null,"sakstype":"${Type.entries.first().name}"}""",
                """{"kabalReference":"$reference","sakstype":null}""",
                """{"kabalReference":"not-a-uuid","sakstype":"${Type.entries.first().name}"}""",
                """{"kabalReference":"$reference","sakstype":"PRIVATE_REJECTED_VALUE"}""",
                """{"kabalReference":"$reference","sakstype":0}""",
                """{"kabalReference":"$reference"}""",
                """{"sakstype":"${Type.entries.first().name}"}""",
                "{",
            )
        invalidBodies.forEach { invalid ->
            for (request in listOf(post("/api/saksdata"), put("/api/saksdata/${UUID.randomUUID()}"))) {
                authorized(request.contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Invalid request"))
            }
        }
        listOf("page=-1", "size=0", "size=-1", "size=101", "page=invalid", "size=invalid").forEach { query ->
            authorized(get("/api/saksdata?$query"))
                .andExpect(status().isBadRequest)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        }
        val malformedIds =
            listOf(
                get("/api/saksdata/invalid"),
                put("/api/saksdata/invalid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(reference, Type.entries.first())),
                delete("/api/saksdata/invalid"),
            )
        for (request in malformedIds) {
            authorized(request).andExpect(status().isBadRequest)
        }
    }

    @Test
    fun `returns sanitized 404 for unknown UUID on get put and delete`() {
        val id = UUID.randomUUID()
        val requests =
            listOf(
                get("/api/saksdata/$id"),
                put("/api/saksdata/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(UUID.randomUUID(), Type.entries.first())),
                delete("/api/saksdata/$id"),
            )
        requests.forEach { request ->
            authorized(request)
                .andExpect(status().isNotFound)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.instance").value("/api/saksdata"))
                .andExpect(jsonPath("$.detail").value("Saksdata not found"))
        }
    }

    @Test
    fun `requires bearer authentication for every CRUD operation`() {
        val id = UUID.randomUUID()
        val requests =
            listOf(
                get("/api/saksdata"),
                get("/api/saksdata/$id"),
                post("/api/saksdata")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(id, Type.entries.first())),
                put("/api/saksdata/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(id, Type.entries.first())),
                delete("/api/saksdata/$id"),
            )
        requests.forEach { request ->
            mockMvc
                .perform(request)
                .andExpect(status().isUnauthorized)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Authentication required"))
        }
    }

    @Test
    fun `rejects missing claims future nbf and foreign signing key using library server`() {
        val claims = mapOf("azp" to "kabal-api")
        val invalidClaims =
            listOf(
                emptyMap(),
                claims + ("azp" to emptyList<String>()),
                claims + ("azp" to ""),
                claims + ("aud" to emptyList<String>()),
                claims + ("iss" to "https://wrong.invalid"),
                claims + ("iss" to ""),
                claims + ("nbf" to Instant.now().plusSeconds(300).epochSecond),
            )
        invalidClaims.forEach { values ->
            val token =
                mockAzure.server
                    .issueToken(
                        issuerId = "azuread",
                        clientId = "kabal-api",
                        tokenCallback =
                            object : DefaultOAuth2TokenCallback(
                                issuerId = "azuread",
                                subject = "test-subject",
                                audience = listOf(mockAzure.clientId),
                                claims = values,
                            ) {
                                override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> = values
                            },
                    ).serialize()
            authorized(protectedRequest(), token).andExpect { result ->
                check(result.response.status == 401) {
                    "Unexpected authentication status for claims ${values.keys}: ${result.response.status}"
                }
            }
        }
        val wrongSignature =
            OAuth2TokenProvider(KeyProvider(initialKeys = emptyList()))
                .jwt(
                    mapOf(
                        "iss" to mockAzure.issuer,
                        "aud" to listOf(mockAzure.clientId),
                        "azp" to "kabal-api",
                        "sub" to "test-subject",
                    ),
                ).serialize()
        authorized(protectedRequest(), wrongSignature).andExpect(status().isUnauthorized)
    }

    @Test
    fun `rejects unauthorized caller even for OBO user tokens`() {
        val token =
            mockAzure.token(
                azp = "unauthorized-frontend",
                claims = mapOf(MockAzure.NAV_IDENT_CLAIM to "TESTIDENT"),
            )
        authorized(protectedRequest(), token)
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
    }

    @Test
    fun `health hides details and actuator diagnostics are never public`() {
        mockMvc
            .perform(get("/internal/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist())
            .andExpect(jsonPath("$.details").doesNotExist())
        listOf("/internal/env", "/internal/heapdump").forEach { path ->
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized)
            authorized(get(path)).andExpect(status().isNotFound)
        }
        mockMvc.perform(post("/internal/health")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `does not log case identifiers payload rejected values or tokens`() {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        val reference = UUID.randomUUID()
        val token = mockAzure.token()
        try {
            val id = create(reference, Type.entries.first(), token)["id"].asString()
            authorized(get("/api/saksdata/$id"), token).andExpect(status().isOk)
            authorized(
                post("/api/saksdata")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"kabalReference":"$reference","sakstype":"PRIVATE_PAYLOAD"}"""),
                token,
            ).andExpect(status().isBadRequest)
            authorized(get("/api/saksdata/${UUID.randomUUID()}"), token).andExpect(status().isNotFound)
            val messages = appender.list.joinToString { it.formattedMessage }
            listOf(reference.toString(), id, token, "PRIVATE_PAYLOAD").forEach {
                messages.contains(it) shouldBe false
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    private fun authorized(
        request: MockHttpServletRequestBuilder,
        token: String = mockAzure.token(),
    ) = mockMvc.perform(request.header("Authorization", "Bearer $token"))

    @Test
    fun `sanitizes real database failures and logs only error identifiers and exception types`() {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        val reference = UUID.randomUUID()
        logger.addAppender(appender)
        jdbc.execute("ALTER TABLE saksdata ADD CONSTRAINT test_insert_failure CHECK (FALSE)")
        try {
            authorized(
                post("/api/saksdata")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(reference, Type.entries.first())),
            ).andExpect(status().isInternalServerError)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Database operation failed"))
                .andExpect(jsonPath("$.errorId").isNotEmpty)
            val messages = appender.list.joinToString { it.formattedMessage }
            messages.contains(reference.toString()) shouldBe false
            messages.contains("test_insert_failure") shouldBe false
            appender.list.filter { it.loggerName.startsWith("no.nav.klage.kant") }.forEach {
                it.throwableProxy shouldBe null
            }
        } finally {
            jdbc.execute("ALTER TABLE saksdata DROP CONSTRAINT test_insert_failure")
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    private fun create(
        reference: UUID,
        type: Type,
        token: String = mockAzure.token(),
    ): JsonNode {
        val response =
            authorized(
                post("/api/saksdata").contentType(MediaType.APPLICATION_JSON).content(body(reference, type)),
                token,
            ).andExpect(status().isCreated)
                .andReturn()
                .response
        val created = mapper.readTree(response.contentAsString)
        response.getHeader("Location") shouldBe "/api/saksdata/${created["id"].asString()}"
        return created
    }

    private fun body(
        reference: UUID,
        type: Type,
    ): String = """{"kabalReference":"$reference","sakstype":"${type.name}"}"""
}
