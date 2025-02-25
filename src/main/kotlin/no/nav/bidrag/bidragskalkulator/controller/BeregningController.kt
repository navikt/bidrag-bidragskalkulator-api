package no.nav.bidrag.bidragskalkulator.controller

import no.nav.bidrag.bidragskalkulator.controller.dto.BeregningResultatDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/beregning")
class BeregningController {

    @GetMapping("/enkel")
    fun hentEnkelBeregning(): BeregningResultatDto {
        // For now, return a simple calculation result
        return BeregningResultatDto(resultat = 100.0)
    }
}