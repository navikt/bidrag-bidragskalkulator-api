package no.nav.bidrag.bidragskalkulator.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class BarnDto(
    @field:NotNull(message = "Alder må være satt")
    @field:Min(value = 0, message = "Alder kan ikke være negativ")
    @field:Max(value = 25, message = "Alder kan ikke være høyere enn 25")
    val alder: Int,

    @field:NotNull(message = "samværsklasse må være satt")
    val samværsklasse: Samværsklasse
){
    val fødselsdato get() = LocalDate.parse("${ YearMonth.now().minusYears(alder.toLong()).format(DateTimeFormatter.ofPattern("yyyy-MM")) }-01")
}

data class BeregningRequestDto(
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
