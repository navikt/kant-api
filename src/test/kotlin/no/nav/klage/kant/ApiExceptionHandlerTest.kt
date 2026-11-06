package no.nav.klage.kant

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import no.nav.klage.kant.config.ApiExceptionHandler
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID

class ApiExceptionHandlerTest {
    @Test
    fun `sanitizes database and unexpected failures without exception stack traces`() {
        val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        val sensitive = UUID.randomUUID().toString()
        try {
            val handler = ApiExceptionHandler()
            val databaseProblem = handler.databaseFailure(DataIntegrityViolationException(sensitive))
            val unexpectedProblem = handler.unexpectedFailure(IllegalStateException(sensitive))
            listOf(databaseProblem, unexpectedProblem).forEach {
                it.status shouldBe 500
                it.toString().contains(sensitive) shouldBe false
                UUID.fromString(it.properties!!["errorId"].toString())
            }
            appender.list.size shouldBe 2
            appender.list.forEach {
                it.formattedMessage.contains(sensitive) shouldBe false
                it.throwableProxy shouldBe null
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}
