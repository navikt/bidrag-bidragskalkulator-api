package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.config.SecurityConstants
import no.nav.bidrag.bidragskalkulator.dto.åpenBeregning.ÅpenBeregningRequestDto
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.bidrag.bidragskalkulator.validering.BeregningRequestValidator
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/beregning")
@ProtectedWithClaims(issuer = SecurityConstants.TOKENX)
class BeregningController(private val beregningService: BeregningService) {

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
    fun beregnBarnebidrag(@Valid @RequestBody request: BeregningRequestDto): BeregningsresultatDto = runBlocking(
        Dispatchers.IO + MDCContext()
    ) {
        BeregningRequestValidator.valider(request)
        beregningService.beregnBarnebidrag(request)
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
    @PostMapping("/anonym/barnebidrag")
    @Unprotected
    fun beregnBarnebidragÅpen(@Valid @RequestBody request: ÅpenBeregningRequestDto): BeregningsresultatDto  = runBlocking(
        Dispatchers.IO + MDCContext()
    ) {
        beregningService.beregnBarnebidragAnonym(request)
    }
}
