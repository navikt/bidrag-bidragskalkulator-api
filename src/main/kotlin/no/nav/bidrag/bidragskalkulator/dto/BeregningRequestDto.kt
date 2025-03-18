package no.nav.bidrag.bidragskalkulator.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import java.time.LocalDate

@Schema(description = "Type bidrag")
enum class BidragsType {
    PLIKTIG,
    MOTTAKER
}

@Schema(description = "Informasjon om et barn i beregningen")
data class BarnDto(
    @field:NotNull(message = "Alder må være satt")
    @field:Min(value = 0, message = "Alder kan ikke være negativ")
    @field:Max(value = 25, message = "Alder kan ikke være høyere enn 25")
    @Schema(description = "Alder til barnet", required = true, example = "10")
    val alder: Int,

    @field:NotNull(message = "samværsklasse må være satt")
    @Schema(ref = "#/components/schemas/Samværsklasse") // Reference dynamically registered schema. See OpenApiConfig
    val samværsklasse: Samværsklasse,

    @field:NotNull(message = "bidragstype må være satt")
    @Schema(description = "Type bidrag", required = true)
    val bidragstype: BidragsType
){
    @JsonIgnore
    @Schema(hidden = true) // 🚀 Hides from Swagger
    fun getEstimertFødselsdato(): LocalDate = LocalDate.now().minusYears(alder.toLong())
}

@Schema(description = "Modellen brukes til å beregne barnebidrag")
data class BeregningRequestDto(
    @field:NotNull(message = "Inntekt for forelder 1 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 1 kan ikke være negativ")
    @Schema(description = "Inntekt for forelder 1 i norske kroner", required = true, example = "500000.0")
    val inntektForelder1: Double,

    @field:NotNull(message = "Inntekt for forelder 2 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 2 kan ikke være negativ")
    @Schema(description = "Inntekt for forelder 2 i norske kroner", required = true, example = "450000.0")
    val inntektForelder2: Double,

    @field:NotEmpty(message = "Liste over barn kan ikke være tom")
    @field:Valid
    @Schema(description = "Liste over barn som inngår i beregningen", required = true)
    val barn: List<BarnDto>
)
