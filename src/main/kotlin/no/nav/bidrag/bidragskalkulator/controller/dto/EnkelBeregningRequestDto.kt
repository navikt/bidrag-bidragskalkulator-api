package no.nav.bidrag.bidragskalkulator.controller.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max

data class BarnDto(
    @field:NotNull(message = "Alder må være satt")
    @field:Min(value = 0, message = "Alder kan ikke være negativ")
    @field:Max(value = 25, message = "Alder kan ikke være høyere enn 25")
    val alder: Int,

    @field:NotNull(message = "Samværsgrad må være satt")
    @field:Min(value = 0, message = "Samværsgrad kan ikke være negativ")
    @field:Max(value = 100, message = "Samværsgrad kan ikke være høyere enn 100")
    val samværsgrad: Int
)

data class EnkelBeregningRequestDto(
    @field:NotNull(message = "Inntekt for forelder 1 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 1 kan ikke være negativ")
    val inntektForelder1: Double,

    @field:NotNull(message = "Inntekt for forelder 2 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 2 kan ikke være negativ")
    val inntektForelder2: Double,

    @field:NotEmpty(message = "Liste over barn kan ikke være tom")
    @field:Valid
    val barn: List<BarnDto>
)
