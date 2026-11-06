package no.nav.klage.kant.api

import jakarta.validation.ConstraintViolationException
import no.nav.klage.kant.saksdata.SaksdataNotFoundException
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
class ApiExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(SaksdataNotFoundException::class)
    fun notFound(): ProblemDetail = problem(HttpStatus.NOT_FOUND, "Saksdata not found")

    @ExceptionHandler(ConstraintViolationException::class)
    fun invalidParameters(): ProblemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid request parameters")

    @ExceptionHandler(DataAccessException::class)
    fun databaseFailure(exception: DataAccessException): ProblemDetail {
        val errorId = UUID.randomUUID()
        log.error("Database operation failed: errorId={}, exceptionType={}", errorId, exception.javaClass.simpleName)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Database operation failed").apply {
            setProperty("errorId", errorId)
        }
    }

    @ExceptionHandler(Exception::class)
    fun unexpectedFailure(exception: Exception): ProblemDetail {
        val errorId = UUID.randomUUID()
        log.error("Request failed: errorId={}, exceptionType={}", errorId, exception.javaClass.simpleName)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error").apply {
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
        return ResponseEntity(problem(statusCode, detail), headers, statusCode)
    }

    private fun problem(
        status: HttpStatusCode,
        detail: String,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            instance = URI("/api/saksdata")
        }
}
