package no.nav.bidrag.bidragskalkulator.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.dto.BidragskalkulatorGrunnlagDto
import no.nav.bidrag.bidragskalkulator.service.BidragskalkulatorGrunnlagService
import no.nav.bidrag.bidragskalkulator.utils.BidragAwareContext
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/bidragskalkulator")
@Unprotected
class BidragskalkulatorGrunnlagController(
    private val request: HttpServletRequest,
    private val bidragskalkulatorGrunnlagService: BidragskalkulatorGrunnlagService,
) {

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
    @GetMapping("/grunnlagsdata")
    fun hentGrunnlagsData(): BidragskalkulatorGrunnlagDto {
        logger.info { "Starter henting av grunnlagsdata for kalkulering uten autentisering (endepunkt=${request.requestURI})" }

        val (resultat, varighet) = measureTimedValue {
            runBlocking(BidragAwareContext) {
                bidragskalkulatorGrunnlagService.hentGrunnlagsData()
            }
        }

        logger.info { "Fullført henting av grunnlagsdata for kalkulering uten autentisering (varighet=${varighet.inWholeMilliseconds}ms)" }
        return resultat
    }
}
