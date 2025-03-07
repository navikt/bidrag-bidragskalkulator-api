package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Inneholder beregningsresultater for hvert barn i forespørselen")
data class BeregningResultatDto(
    val beregningsResultater: List<BeregningResultatPerBarnDto>
)

@Schema(description = "Beregnet barnebidrag for et enkelt barn")
data class BeregningResultatPerBarnDto(
    @Schema(description = "Beregningsresultat", example = "3200")
    val resultat: BigDecimal,
    @Schema(description = "Alder på barnet som beregningen gjelder for", example = "10")
    val barnetsAlder: Int
)