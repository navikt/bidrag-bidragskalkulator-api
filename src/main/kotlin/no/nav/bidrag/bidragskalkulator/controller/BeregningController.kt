package no.nav.bidrag.bidragskalkulator.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import jakarta.validation.Valid
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/beregning")
class BeregningController(private val beregningService: BeregningService) {

    @Operation(summary = "Beregner barnebidrag", description = "Beregner barnebidrag basert på inntekten til foreldre og barnets alder. Returnerer 200 ved vellykket beregning.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Beregning fullført"),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel - mangler eller feil i inputdata"),
            ApiResponse(responseCode = "500", description = "Intern serverfeil")
        ]
    )
    @PostMapping("/barnebidrag")
    fun beregnBarnebidrag(@Valid @RequestBody request: BeregningRequestDto): BeregningsresultatDto {
        return beregningService.beregnBarnebidrag(request)
    }
}
