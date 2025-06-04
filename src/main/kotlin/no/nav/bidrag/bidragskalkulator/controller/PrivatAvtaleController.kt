package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtalePdfService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/privatavtale")
@Unprotected
class PrivatAvtaleController(private val privatAvtaleService: PrivatAvtalePdfService) {



    @GetMapping(produces = [MediaType.APPLICATION_PDF_VALUE])
    @Operation(
        summary = "Generer privat avtale PDF",
        description = "Genererer en privat avtale i PDF-format.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Privat avtale PDF generert"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    fun genererPrivatAvtale(): ResponseEntity<ByteArray>? {

        return runBlocking(Dispatchers.IO + MDCContext()) {
            val genererPrivatAvtalePdf = async { privatAvtaleService.genererPrivatAvtalePdf() }
            ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"privatavtale.pdf\"")
                .body(genererPrivatAvtalePdf.await().toByteArray())
        }
    }
}