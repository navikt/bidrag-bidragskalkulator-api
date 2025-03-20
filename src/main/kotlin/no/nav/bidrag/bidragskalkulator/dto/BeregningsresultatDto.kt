package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUnderholdskostnad
import java.math.BigDecimal

@Schema(description = "Inneholder beregningsresultater for hvert barn i forespørselen")
data class BeregningsresultatDto(
    val resultater: List<BeregningsresultatBarnDto>
)

@Schema(description = "Beregnet barnebidrag for et enkelt barn")
data class BeregningsresultatBarnDto(
    @Schema(description = "Beregnet barnebidrag", example = "3200")
    val sum: BigDecimal,
    @Schema(description = "Alder på barnet som beregningen gjelder for", example = "10")
    val barnetsAlder: Int,
    @Schema(description = "Underholdskostnad til barnet, gruppert etter aldersintervall (0-5, 6-10, 11-14, 15+)", example = "4738")
    val underholdskostnad: BigDecimal,
    @Schema(description = "Typen bidrag – man skal betale eller motta bidrag", example = "PLIKTIG")
    val bidragstype: BidragsType,
)