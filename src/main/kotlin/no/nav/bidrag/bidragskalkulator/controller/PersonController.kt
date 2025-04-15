package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.BrukerInfomasjonDto
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
        summary = "Henter informasjon om pålogget person og relasjoner til barn",
        description = "Henter informasjon om pålogget person og relasjoner til barn. Returnerer 200 ved vellykket henting, eller passende feilkoder.",
        security = [SecurityRequirement(name = SecurityConstants.BEARER_KEY)]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Brukerinformasjon hentet vellykket"),
            ApiResponse(responseCode = "204", description = "Person eksisterer ikke"),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang - ugyldig eller utløpt token"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @GetMapping("/informasjon")
    fun hentInformasjon(): BrukerInfomasjonDto {
        logger.info("Henter informasjon om pålogget person og personens barn")

        val personIdent: String = requireNotNull(TokenUtils.hentBruker()) {
            "Brukerident er ikke tilgjengelig i token"
        }

        secureLogger.info { "Henter informasjon om pålogget person $personIdent og personens barn" }

        return personService.hentInformasjon(personIdent).also {
            secureLogger.info { "Henter informasjon om pålogget person $personIdent fullført" }
        }
    }
}