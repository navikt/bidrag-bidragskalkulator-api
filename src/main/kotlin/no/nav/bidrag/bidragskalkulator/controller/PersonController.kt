package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.dto.GrunnlagsDataDto
import no.nav.bidrag.bidragskalkulator.service.BrukerinformasjonService
import no.nav.bidrag.bidragskalkulator.utils.BidragAwareContext
import no.nav.bidrag.bidragskalkulator.utils.InnloggetBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/v1/person")
class PersonController(
    private val brukerinformasjonService: BrukerinformasjonService,
    private val innloggetBrukerUtils: InnloggetBrukerUtils
) {
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
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – valideringsfeil eller ugyldig enumverdi"
            ),
            ApiResponse(responseCode = "401", description = "Uautorisert tilgang - ugyldig eller utløpt token"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @GetMapping("/informasjon")
    @ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
    fun hentInformasjon(): BrukerInformasjonDto {
        logger.info("Henter informasjon om pålogget person og personens barn")

        val personIdent = innloggetBrukerUtils.hentPåloggetPersonIdent()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ugyldig token")

        return runBlocking(Dispatchers.IO + MDCContext()) {
            secureLogger.info { "Henter informasjon om pålogget person $personIdent og personens barn" }

            brukerinformasjonService.hentBrukerinformasjon(personIdent).also {
                secureLogger.info { "Henter informasjon om pålogget person $personIdent fullført" }
            }

        }
    }

    @Operation(
        summary = "Henter grunnlagsdata for kalkulering uten autentisering",
        description = "Henter underholdskostnader og samværsfradrag som brukes i bidragskalkuleringen. Returnerer 200 ved vellykket henting, eller passende feilkoder."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Grunnlagsdata hentet vellykket"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @Unprotected
    @GetMapping("/grunnlagsdata")
    fun hentGrunnlagsData(): GrunnlagsDataDto {
        secureLogger.info { "Henter Grunnlagsdata (underholdskostnader og samværsfradrag)" }

        val result = runBlocking(BidragAwareContext) {
            brukerinformasjonService.hentGrunnlagsData().also {
                secureLogger.info { "Grunnlagsdata hentet" }
            }
        }
        return result
    }
}
