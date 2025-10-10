package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Informasjon som brukes i bidragskalkuleringen")
data class BidragskalkulatorGrunnlagDto(
    @param:Schema(description = "bo- og forbruksutgifter per aldersgruppe")
    val boOgForbruksutgifter: Map<Int, BigDecimal>,

    @param:Schema(description = "Samværsfradrag per aldersintervall")
    val samværsfradrag: List<SamværsfradragPeriode>
)
