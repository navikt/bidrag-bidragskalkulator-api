package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.PrivatAvtaleInformasjonDto
import no.nav.bidrag.bidragskalkulator.service.PrivatAvtaleService
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/privat-avtale")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
class PrivatAvtaleController(
    private val privatAvtaleService: PrivatAvtaleService,
    private val innloggetBrukerUtils: InnloggetBrukerUtils
) {
    private val logger = LoggerFactory.getLogger(PrivatAvtaleController::class.java)

    @Operation(
        summary = "Henter informasjon for opprettelse av privat avtale",
        description = "Henter informasjon for opprettelse av privat avtale. Returnerer 200 ved vellykket henting, eller passende feilkoder.",
        security = [SecurityRequirement(name = SecurityConstants.BEARER_KEY)]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Privat avtale informasjon hentet vellykket"),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang - mangler eller ugyldig token"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @GetMapping("/informasjon")
    fun hentInformasjonForPrivatAvtale(): PrivatAvtaleInformasjonDto {
        logger.info("Henter informasjon for opprettelse av privat avtale")

        val personIdent = innloggetBrukerUtils.hentPåloggetPersonIdent()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ugyldig token")

        return runBlocking(Dispatchers.IO + MDCContext()) {
            secureLogger.info { "Henter informasjon om pålogget person $personIdent til bruk i en privat avtale" }

            privatAvtaleService.hentInformasjonForPrivatAvtale(personIdent).also {
                secureLogger.info { "Henter informasjon om pålogget person $personIdent til bruk i en privat avtale fullført" }
            }

        }

    }
}