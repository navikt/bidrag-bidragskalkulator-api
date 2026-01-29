package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Informasjon som brukes i bidragskalkuleringen")
data class BidragskalkulatorGrunnlagDto(
    @field:Schema(description = "bo- og forbruksutgifter per aldersgruppe")
    val boOgForbruksutgifter: Map<Int, BigDecimal>,

    @field:Schema(description = "Samværsfradrag per aldersintervall")
    val samværsfradrag: List<SamværsfradragPeriode>,

    @field:Schema(
        description = "Dersom barnets årsinntekt overstiger denne grensen, tas inntekten med i beregningen av barnebidrag."
    )
    val barnInntektsgrense: BigDecimal,

    @field:Schema(
        description = "Årsinntekt over denne grensen medfører at barnet regnes som selvforsørget."
    )
    val selvforsørgetBarnInntektsgrense: BigDecimal

)
