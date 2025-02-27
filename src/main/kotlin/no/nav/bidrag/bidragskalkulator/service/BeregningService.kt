package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.controller.dto.BeregningResultatDto
import org.springframework.stereotype.Service

@Service
class BeregningService() {
    private val beregnBarnebidragApi = BeregnBarnebidragApi()

    fun beregnBarneBidrag(): BeregningResultatDto{
        println("---beregnBarneBidrag service---")
        return BeregningResultatDto(resultat = 100.0)
    }
}