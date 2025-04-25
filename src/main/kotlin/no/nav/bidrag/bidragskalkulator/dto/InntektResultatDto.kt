package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Inneholder informasjon om inntektsgrunnlag for en person")
data class InntektResultatDto(
    @Schema(description = "Kalkulert inntekt for de siste 12 månedene", example = "500000.0")
    val inntektSiste12Mnd: BigDecimal
)