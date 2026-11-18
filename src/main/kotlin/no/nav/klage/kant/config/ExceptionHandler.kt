package no.nav.klage.kant.config

import jakarta.validation.ConstraintViolationException
import no.nav.klage.kant.domain.kvalitetsvurdering.KvalitetsvurderingValidationException
import no.nav.klage.kant.services.InvalidKvalitetsvurderingTypeException
import no.nav.klage.kant.services.InvalidSakstypeException
import no.nav.klage.kant.services.SaksdataConflictException
import no.nav.klage.kant.services.SaksdataNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI
import java.util.UUID

@RestControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(SaksdataNotFoundException::class)
    fun notFound(): ProblemDetail = problem(status = HttpStatus.NOT_FOUND, detail = "Saksdata not found")

    @ExceptionHandler(SaksdataConflictException::class)
    fun conflict(exception: SaksdataConflictException): ProblemDetail =
        problem(status = HttpStatus.CONFLICT, detail = exception.message ?: "Saksdata conflict")

    @ExceptionHandler(ConstraintViolationException::class)
    fun invalidParameters(): ProblemDetail = problem(status = HttpStatus.BAD_REQUEST, detail = "Invalid request parameters")

    @ExceptionHandler(InvalidSakstypeException::class)
    fun invalidSakstype(exception: InvalidSakstypeException): ProblemDetail =
        problem(status = HttpStatus.BAD_REQUEST, detail = exception.message ?: "Invalid sakstype")

    @ExceptionHandler(InvalidKvalitetsvurderingTypeException::class)
    fun invalidKvalitetsvurderingType(exception: InvalidKvalitetsvurderingTypeException): ProblemDetail =
        problem(status = HttpStatus.BAD_REQUEST, detail = exception.message ?: "Invalid kvalitetsvurdering type")

    @ExceptionHandler(KvalitetsvurderingValidationException::class)
    fun invalidKvalitetsvurdering(exception: KvalitetsvurderingValidationException): ProblemDetail =
        problem(status = HttpStatus.BAD_REQUEST, detail = "Kvalitetsvurdering is incomplete or invalid").apply {
            setProperty("errors", exception.errors)
        }

    @ExceptionHandler(DataAccessException::class)
    fun databaseFailure(exception: DataAccessException): ProblemDetail {
        val errorId = UUID.randomUUID()
        log.error("Database operation failed: errorId={}, exceptionType={}", errorId, exception.javaClass.simpleName)
        return problem(status = HttpStatus.INTERNAL_SERVER_ERROR, detail = "Database operation failed").apply {
            setProperty("errorId", errorId)
        }
    }

    @ExceptionHandler(Exception::class)
    fun unexpectedFailure(exception: Exception): ProblemDetail {
        val errorId = UUID.randomUUID()
        log.error("Request failed: errorId={}, exceptionType={}", errorId, exception.javaClass.simpleName)
        return problem(status = HttpStatus.INTERNAL_SERVER_ERROR, detail = "Internal server error").apply {
            setProperty("errorId", errorId)
        }
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val detail =
            if (statusCode.value() == HttpStatus.BAD_REQUEST.value()) {
                "Invalid request"
            } else {
                HttpStatus.valueOf(statusCode.value()).reasonPhrase
            }
        return ResponseEntity(problem(status = statusCode, detail = detail), headers, statusCode)
    }

    private fun problem(
        status: HttpStatusCode,
        detail: String,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            instance = URI("/api/saksdata")
        }
}
