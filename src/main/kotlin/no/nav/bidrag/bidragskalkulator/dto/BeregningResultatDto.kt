package no.nav.bidrag.bidragskalkulator.dto

import java.math.BigDecimal

data class BeregningResultatDto(
    val beregningsResultater: List<BeregningResultatPerBarnDto>
)

data class BeregningResultatPerBarnDto(
    val resultat: BigDecimal,
    val barnetsAlder: Int
)