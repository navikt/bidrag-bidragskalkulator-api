package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

@Schema(description = "Inneholder beregningsresultater for hvert barn i forespørselen")
data class BeregningsresultatDto(
    val resultater: List<BeregningsresultatBarnDto>
)

@Schema(description = "Beregnet barnebidrag for et enkelt barn")
data class BeregningsresultatBarnDto(
    @Schema(description = "Unik identifikator for barnet (fødselsnummer eller D-nummer)", example = "12345678901")
    val ident: Personident,

    @Schema(description = "Fullt navn til barnet", example = "Ola Nordmann")
    val fulltNavn: String,

    @Schema(description = "Fornavn til barnet", example = "Ola")
    val fornavn: String,

    @Schema(description = "Beregnet barnebidrag", example = "3200")
    val sum: BigDecimal,

    @Schema(description = "Typen bidrag – man skal betale eller motta bidrag", example = "PLIKTIG")
    val bidragstype: BidragsType,
)