package no.nav.bidrag.bidragskalkulator.controller

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.servlet.http.HttpServletRequest
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.bidragskalkulator.validering.BeregningRequestValidator
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import kotlin.time.measureTimedValue

@RestController
@RequestMapping("/api/v1/beregning")
class BeregningController(
    private val httpServletRequest: HttpServletRequest,
    private val beregningService: BeregningService,
    meterRegistry: MeterRegistry ) {
    private val logger = LoggerFactory.getLogger(BeregningController::class.java)

private val aapenBeregningCounter = Counter.builder("bidragskalkulator_antall_beregninger")
        .description("Antall beregninger utført")
        .register(meterRegistry)


    @Operation(summary = "Beregner barnebidrag",
        description = "Beregner barnebidrag basert på inntekten til foreldre og barnets alder. Returnerer 200 ved vellykket beregning.",
        security = [SecurityRequirement(name = SecurityConstants.BEARER_KEY)])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Beregning fullført"),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel - mangler eller feil i inputdata"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @PostMapping("/barnebidrag")
    @ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
    fun beregnBarnebidrag(@Valid @RequestBody request: BeregningRequestDto): BeregningsresultatDto {
        logger.info("Starter beregning av barnebidrag (endepunkt=${httpServletRequest.requestURI})")
        BeregningRequestValidator.valider(request)

        val (resultat, varighet) = measureTimedValue {
            runBlocking(Dispatchers.IO + MDCContext()) {
                aapenBeregningCounter.increment()
                beregningService.beregnBarnebidrag(request)
            }
        }

        logger.info("Fullført beregning av barnebidrag på ${varighet.inWholeMilliseconds} ms")
        return resultat
    }

    @Operation(summary = "Beregner barnebidrag",
        description = "Beregner barnebidrag basert på inntekten til foreldre og barnets alder. Returnerer 200 ved vellykket beregning.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Beregning fullført"),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel - mangler eller feil i inputdata"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @PostMapping("/barnebidrag/åpen")
    @Unprotected
    fun beregnBarnebidragÅpen(@Valid @RequestBody request: ÅpenBeregningRequestDto): ÅpenBeregningsresultatDto {
        logger.info("Starter beregning av barnebidrag uten autentisering (endepunkt=${httpServletRequest.requestURI})")
        BeregningRequestValidator.valider(request)

        val (resultat, varighet) = measureTimedValue {
            runBlocking(Dispatchers.IO + MDCContext()) {
                aapenBeregningCounter.increment()
                beregningService.beregnBarnebidragAnonym(request)
            }
        }

        logger.info("Fullført beregning av barnebidrag uten autentisering  på ${varighet.inWholeMilliseconds} ms")
        return resultat
    }
}
