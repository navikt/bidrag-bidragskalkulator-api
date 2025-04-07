package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/v1/person")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
class PersonController(private val personService: PersonService) {

    private val logger = LoggerFactory.getLogger(PersonController::class.java)

    @Operation(
        summary = "Henter familierelasjoner",
        description = "Henter familierelasjoner for pålogget person. Returnerer 200 ved vellykket henting, eller passende feilkoder.",
        security = [SecurityRequirement(name = SecurityConstants.BEARER_KEY)]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Familierelasjoner hentet vellykket"),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel - mangler eller feil i inputdata"),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang - ugyldig eller utløpt token"),
            ApiResponse(responseCode = "404", description = "Ingen familierelasjoner funnet"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @GetMapping("/familierelasjon")
    fun hentFamilierelasjon(): ResponseEntity<MotpartBarnRelasjonDto> {
        return try {
            val familierelasjon = personService.hentFamilierelasjon()
            familierelasjon?.let {
                ResponseEntity.ok(it)
            } ?: run {
                logger.warn("Ingen familierelasjoner funnet for pålogget bruker")
                ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }
        } catch (e: Exception) {
            logger.error("Feil ved henting av familierelasjon for pålogget bruker", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}