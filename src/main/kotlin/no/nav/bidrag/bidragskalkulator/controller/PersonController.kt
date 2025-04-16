package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.service.GrunnlagService
import no.nav.bidrag.bidragskalkulator.service.PersonService
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

@RestController
@RequestMapping("/api/v1/person")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
class PersonController(private val personService: PersonService, private val grunnlagService: GrunnlagService) {

    private val logger = LoggerFactory.getLogger(PersonController::class.java)

    @Operation(
        summary = "Henter familierelasjoner",
        description = "Henter familierelasjoner for pålogget person. Returnerer 200 ved vellykket henting, eller passende feilkoder.",
        security = [SecurityRequirement(name = SecurityConstants.BEARER_KEY)]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Familierelasjoner hentet vellykket"),
            ApiResponse(responseCode = "204", description = "Person eksisterer ikke"),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel - mangler eller feil i inputdata"),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang - ugyldig eller utløpt token"),
            ApiResponse(responseCode = "404", description = "Ingen familierelasjoner funnet"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @GetMapping("/familierelasjon")
    fun hentFamilierelasjon(): MotpartBarnRelasjonDto? {
        logger.info("Henter familierelasjoner")

        val personIdent: String = TokenUtils.hentBruker()
            ?: throw IllegalArgumentException("Brukerident er ikke tilgjengelig i token")

        secureLogger.info { "Henter familierelasjoner for person $personIdent" }

        val respons = personService.hentFamilierelasjon(personIdent)

        secureLogger.info { "Henting av familierelasjoner for person $personIdent er fullført" }
        return respons
    }

    @Operation(
        summary = "Henter inntektsgrunnlag",
        description = "Henter inntektsgrunnlaget for en person. Returnerer 200 ved vellykket henting, eller passende feilkoder.",
        security = [SecurityRequirement(name = SecurityConstants.BEARER_KEY)]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Inntektsgrnunlag hentet vellykket"),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel - mangler eller feil i inputdata"),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang - ugyldig eller utløpt token"),
            ApiResponse(responseCode = "404", description = "Ingen inntektsgrnunlag funnet"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @GetMapping("/inntekt")
    fun hentInntekt(): TransformerInntekterResponse {
        val personIdent: String = TokenUtils.hentBruker()
            ?: throw IllegalArgumentException("Brukerident er ikke tilgjengelig i token")

        secureLogger.info { "Henter inntektsgrunnlag for ident $personIdent" }

        val result = grunnlagService.hentInntektsGrunnlag(personIdent)
            ?: throw HttpClientErrorException(HttpStatus.NOT_FOUND, "Fant ikke inntektsgrunnlag for person $personIdent")

        secureLogger.info { "Henting av inntektsgrunnlag for ident $personIdent fullførte OK" }

        return result
    }
}