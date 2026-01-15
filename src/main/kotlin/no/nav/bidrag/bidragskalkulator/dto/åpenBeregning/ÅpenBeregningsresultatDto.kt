package no.nav.bidrag.bidragskalkulator.dto.åpenBeregning

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Inneholder beregningsresultater for hvert barn i forespørselen")
data class ÅpenBeregningsresultatDto(
    val resultater: List<ÅpenBeregningsresultatBarnDto>
)

@Schema(description = "Beregnet barnebidrag for et enkelt barn")
data class ÅpenBeregningsresultatBarnDto(
    @field:Schema(description = "Alder til barnet", required = true, example = "10")
    val alder: Int,

    @field:Schema(description = "Beregnet barnebidrag", example = "3200")
    val sum: BigDecimal
)
