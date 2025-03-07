package no.nav.bidrag.bidragskalkulator.controller

import no.nav.bidrag.bidragskalkulator.dto.BeregningResultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import jakarta.validation.Valid
import no.nav.bidrag.bidragskalkulator.service.BeregningService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.*

@RestController
@Protected
@RequestMapping("/v1/beregning")
class BeregningController(private val beregningService: BeregningService) {

    @PostMapping("/barnebidrag")
    fun beregnBarnebidrag(@Valid @RequestBody request: BeregningRequestDto): BeregningResultatDto {
        return beregningService.beregnBarnebidrag(request)
    }
}
