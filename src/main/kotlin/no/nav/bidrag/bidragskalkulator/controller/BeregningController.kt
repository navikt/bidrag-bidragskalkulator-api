package no.nav.bidrag.bidragskalkulator.controller

import no.nav.bidrag.bidragskalkulator.controller.dto.BeregningResultatDto
import no.nav.bidrag.bidragskalkulator.controller.dto.EnkelBeregningRequestDto
import jakarta.validation.Valid
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/v1/beregning")
class BeregningController(private val beregningService: BeregningService) {

    @PostMapping("/enkel")
    fun beregnBidrag(@Valid @RequestBody request: EnkelBeregningRequestDto): BeregningResultatDto {
        // For now, return a simple calculation result
        // TODO: Implement actual calculation logic using the request parameters
        return BeregningResultatDto(resultat = BigDecimal(100))
    }


    @GetMapping("/barnebidrag")
    fun beregnBarneBidrag(@Valid @RequestBody request: EnkelBeregningRequestDto): BeregningResultatDto {
        return beregningService.beregnBarneBidrag(request)
    }
}
