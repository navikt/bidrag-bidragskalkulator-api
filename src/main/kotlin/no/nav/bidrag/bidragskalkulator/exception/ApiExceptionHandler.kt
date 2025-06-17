package no.nav.bidrag.bidragskalkulator.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, List<String>>> {
        val errors = ex.bindingResult.fieldErrors.map { 
            "${it.field}: ${it.defaultMessage}"
        }
        return ResponseEntity(mapOf("errors" to errors), HttpStatus.BAD_REQUEST)
    }

    // Handle the custom NoContentException and return a 204 No Content
    @ExceptionHandler(NoContentException::class)
    fun handleNoContentException(ex: NoContentException): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()  // This returns a 204 No Content response
    }

    // Handle the custom UgyldigBeregningRequestException and return a 400 Bad Request
    @ExceptionHandler(UgyldigBeregningRequestException::class)
    fun handleUgyldigBeregningRequestException(ex: UgyldigBeregningRequestException): ResponseEntity<Map<String, List<String>>> {
        val errors = listOf(ex.message ?: "Ugyldig forespørsel")
        return ResponseEntity(mapOf("errors" to errors), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<Map<String, String>> {
        val body = mapOf("error" to (ex.reason ?: "Uautorisert"))
        return ResponseEntity(body, ex.statusCode)
    }
}