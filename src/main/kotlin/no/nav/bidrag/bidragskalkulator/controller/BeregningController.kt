package no.nav.bidrag.bidragskalkulator.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.bidragskalkulator.validering.BeregningRequestValidator
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/beregning")
class BeregningController(
    private val httpServletRequest: HttpServletRequest,
    private val beregningService: BeregningService,
    meterRegistry: MeterRegistry ) {

private val aapenBeregningCounter = Counter.builder("bidragskalkulator_antall_beregninger")
        .description("Antall beregninger utført")
        .register(meterRegistry)

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
        logger.info { "Starter beregning av barnebidrag uten autentisering (endepunkt=${httpServletRequest.requestURI})" }
        BeregningRequestValidator.valider(request)

        val (resultat, varighet) = measureTimedValue {
            runBlocking(Dispatchers.IO + MDCContext()) {
                aapenBeregningCounter.increment()
                beregningService.beregnBarnebidragAnonym(request)
            }
        }

        logger.info { "Fullført beregning av barnebidrag uten autentisering (varighet_ms=${varighet.inWholeMilliseconds})" }
        return resultat
    }
}
