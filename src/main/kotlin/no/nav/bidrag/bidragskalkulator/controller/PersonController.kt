package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
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
import kotlin.time.measureTimedValue

@RestController
@RequestMapping("/api/v1/person")
class PersonController(
    private val request: HttpServletRequest,
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
        logger.info("Starter henting av informasjon om pålogget person (endepunkt=${request.requestURI})")

        val personIdent = innloggetBrukerUtils.requirePåloggetPersonIdent(logger)

        val (resultat, varighet) = measureTimedValue {
            runBlocking(Dispatchers.IO + MDCContext()) {
                brukerinformasjonService.hentBrukerinformasjon(personIdent)
            }
        }

        logger.info("Fullført henting av informasjon om pålogget person (varighet=${varighet.inWholeMilliseconds}ms)")

        return resultat
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
        logger.info("Starter henting av grunnlagsdata for kalkulering uten autentisering (endepunkt=${request.requestURI})")

        val (resultat, varighet) = measureTimedValue {
            runBlocking(BidragAwareContext) {
                brukerinformasjonService.hentGrunnlagsData()
            }
        }

        logger.info("Fullført henting av grunnlagsdata for kalkulering uten autentisering (varighet=${varighet.inWholeMilliseconds}ms)")
        return resultat
    }
}
