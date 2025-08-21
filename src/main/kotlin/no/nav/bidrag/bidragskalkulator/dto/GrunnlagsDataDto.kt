package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Informasjon som brukes i bidragskalkuleringen")
data class GrunnlagsDataDto(
    @Schema(description = "Underholdskostnader per aldersgruppe")
    val underholdskostnader: Map<Int, BigDecimal>,
    
    @Schema(description = "Samværsfradrag per aldersintervall")
    val samværsfradrag: List<SamværsfradragPeriode>
)