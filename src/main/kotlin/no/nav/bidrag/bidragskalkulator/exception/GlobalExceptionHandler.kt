package no.nav.bidrag.bidragskalkulator.exception

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private fun problem(status: HttpStatus, detail: String, vararg errors: String): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            title = status.reasonPhrase
            if (errors.isNotEmpty()) {
                setProperty("errors", errors.toList())
            }
        }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, e.message ?: "Ugyldig forespørsel")


    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException): ProblemDetail {
        val errors = e.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        val detail = errors.firstOrNull() ?: "Valideringsfeil i request body"
        return problem(HttpStatus.BAD_REQUEST, detail, *errors.toTypedArray())
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(e: ConstraintViolationException): ProblemDetail {
        val errors = e.constraintViolations.map { "${it.propertyPath}: ${it.message}" }
        val detail = errors.firstOrNull() ?: "Valideringsfeil i parametre"
        return problem(HttpStatus.BAD_REQUEST, detail, *errors.toTypedArray())
    }

    /**
     * Gjelder typisk feil enum-verdi i JSON body (Jackson deserialisering).
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException): ProblemDetail {
        val cause = e.cause
        if (cause is InvalidFormatException) {
            val target = cause.targetType
            if (target.isEnum) {
                val field = cause.path.lastOrNull()?.let(JsonMappingException.Reference::getFieldName) ?: "ukjent felt"
                val provided = cause.value?.toString() ?: "null"
                val allowed = target.enumConstants.joinToString(", ") { (it as Enum<*>).name }
                val msg = "Ugyldig verdi '$provided' for felt '$field'. Tillatte verdier: $allowed."
                return problem(HttpStatus.BAD_REQUEST, msg, msg)
            }
        }
        return problem(HttpStatus.BAD_REQUEST, "Ugyldig request body (kunne ikke tolkes).")
    }

    /**
     * Gjelder typisk feil enum-verdi i query/path-parametre.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(e: MethodArgumentTypeMismatchException): ProblemDetail {
        val required = e.requiredType
        val msg = if (required != null && required.isEnum) {
            val allowed = required.enumConstants.joinToString(", ") { (it as Enum<*>).name }
            "Ugyldig verdi '${e.value}' for parameter '${e.name}'. Tillatte verdier: $allowed."
        } else {
            "Ugyldig verdi for parameter '${e.name}'. Forventet type: ${required?.simpleName ?: "ukjent"}."
        }
        return problem(HttpStatus.BAD_REQUEST, msg, msg)
    }

    /** Håndterer NoContentException og returnerer 204 No Content. */
    @ExceptionHandler(NoContentException::class)
    fun handleNoContentException(@Suppress("UNUSED_PARAMETER") ex: NoContentException): ResponseEntity<Void> =
        ResponseEntity.noContent().build()

    /** Håndterer UgyldigBeregningRequestException og returnerer 400 Bad Request. */
    @ExceptionHandler(UgyldigBeregningRequestException::class)
    fun handleUgyldigBeregningRequest(ex: UgyldigBeregningRequestException, req: HttpServletRequest): ProblemDetail {
        val msg = ex.message ?: "Ugyldig forespørsel"
        return problem(HttpStatus.BAD_REQUEST, msg)
    }

    /** Håndterer JwtTokenUnauthorizedException og returnerer 401 Unauthorized. */
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleJwtTokenUnauthorizedException(ex: JwtTokenUnauthorizedException, req: HttpServletRequest): ProblemDetail {
        val msg = ex.message ?: "Ugyldig eller manglende token"
        return problem(HttpStatus.UNAUTHORIZED, msg,msg)
    }

    /** Håndterer generelle unntak og returnerer en ProblemDetail med 500 Internal Server Error. */
    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception, req: HttpServletRequest): ProblemDetail =
        problem(HttpStatus.INTERNAL_SERVER_ERROR, "Uventet feil oppstod.")
}
