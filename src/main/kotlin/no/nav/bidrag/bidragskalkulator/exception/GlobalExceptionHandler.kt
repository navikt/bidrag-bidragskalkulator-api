package no.nav.bidrag.bidragskalkulator.exception

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.message ?: "Ugyldig forespørsel").apply {
            title = "Bad Request"
        }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException): ProblemDetail {
        val firstError = e.bindingResult.fieldErrors.firstOrNull()
        val detail = if (firstError != null) {
            "${firstError.field}: ${firstError.defaultMessage}"
        } else {
            "Valideringsfeil i request body"
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail).apply {
            title = "Bad Request"
        }
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(e: ConstraintViolationException): ProblemDetail {
        val detail = e.constraintViolations.firstOrNull()?.let { v ->
            "${v.propertyPath}: ${v.message}"
        } ?: "Valideringsfeil i parametre"
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail).apply {
            title = "Bad Request"
        }
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
                val detail = "Ugyldig verdi '$provided' for felt '$field'. Tillatte verdier: $allowed."
                return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail).apply {
                    title = "Bad Request"
                }
            }
        }
        // Fallback for andre parse-feil
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Ugyldig request body (kunne ikke tolkes).").apply {
            title = "Bad Request"
        }
    }

    /**
     * Gjelder typisk feil enum-verdi i query/path-parametre.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(e: MethodArgumentTypeMismatchException): ProblemDetail {
        val required = e.requiredType
        if (required != null && required.isEnum) {
            val field = e.name
            val provided = e.value?.toString() ?: "null"
            val allowed = required.enumConstants.joinToString(", ") { (it as Enum<*>).name }
            val detail = "Ugyldig verdi '$provided' for parameter '$field'. Tillatte verdier: $allowed."
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail).apply {
                title = "Bad Request"
            }
        }
        val detail = "Ugyldig verdi for parameter '${e.name}'. Forventet type: ${required?.simpleName ?: "ukjent"}."
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail).apply {
            title = "Bad Request"
        }
    }

}
