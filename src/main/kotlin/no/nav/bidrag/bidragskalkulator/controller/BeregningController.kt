package no.nav.bidrag.bidragskalkulator.controller

import no.nav.bidrag.bidragskalkulator.controller.dto.BeregningResultatDto
import no.nav.bidrag.bidragskalkulator.controller.dto.EnkelBeregningRequestDto
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/beregning")
class BeregningController {

    @PostMapping("/enkel")
    fun beregnBidrag(@Valid @RequestBody request: EnkelBeregningRequestDto): BeregningResultatDto {
        // For now, return a simple calculation result
        // TODO: Implement actual calculation logic using the request parameters
        return BeregningResultatDto(resultat = 100.0)
    }
}
