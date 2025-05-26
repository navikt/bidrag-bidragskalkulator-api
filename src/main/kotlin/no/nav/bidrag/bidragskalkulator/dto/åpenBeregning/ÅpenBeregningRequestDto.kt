package no.nav.bidrag.bidragskalkulator.dto.åpenBeregning

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.bidragskalkulator.dto.BarnDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Schema(description = "Informasjon om et barn i beregningen")
data class BarnForÅpenBeregningDto(
    @field:NotNull(message = "Alder må være satt")
    @field:Min(value = 0, message = "Alder kan ikke være negativ")
    @field:Max(value = 25, message = "Alder kan ikke være høyere enn 25")
    @Schema(description = "Alder til barnet", required = true, example = "10")
    val alder: Int,

    @field:NotNull(message = "Samværsklasse må være satt")
    @Schema(ref = "#/components/schemas/Samværsklasse") // Reference dynamically registered schema. See BeregnBarnebidragConfig
    val samværsklasse: Samværsklasse,

    @field:NotNull(message = "Bidragstype må være satt")
    @Schema(description = "Angir om den påloggede personen er pliktig eller mottaker for dette barnet", required = true)
    val bidragstype: BidragsType
){
    @JsonIgnore
    @Schema(hidden = true) // Hides from Swagger
    //Når barnet har alder = 15, blir fødselsmåneden alltid satt til juli, uavhengig av den faktiske fødselsdatoen (usikkert hvor denne regelen stammer fra).
    // Dette betyr at barnet ikke anses som 15 år før juli.
    // I alle beregningsperioder før juli vil barnet derfor fortsatt regnes som 14 år.
    fun getEstimertFødselsdato(): LocalDate = LocalDate.now().minusYears(alder.toLong())

    @JsonIgnore
    @Schema(hidden = true)
    fun getMockIdent(): Personident = Personident(mockPersonnummer(getEstimertFødselsdato()))
}

data class ÅpenBeregningRequestDto (
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
    val barn: List<BarnForÅpenBeregningDto>,
)

fun BarnForÅpenBeregningDto.tilBarnDto(): BarnDto =
    BarnDto(ident = this.getMockIdent(), samværsklasse = this.samværsklasse, bidragstype = this.bidragstype)

fun ÅpenBeregningRequestDto.tilBeregningRequestDto(): BeregningRequestDto =
    BeregningRequestDto(
        inntektForelder1 = this.inntektForelder1,
        inntektForelder2 = this.inntektForelder2,
        barn = this.barn.map { it.tilBarnDto() }
    )


fun mockPersonnummer(fodselsdato: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("ddMMyy")
    val fodselsnummerPrefix = fodselsdato.format(formatter)

    // De neste 5 sifrene er vanligvis individuell del og kontrollsiffer, men vi mocker dem her
    val tilfeldigSuffiks = (1..5)
        .map { Random.nextInt(0, 10) }
        .joinToString("")

    return fodselsnummerPrefix + tilfeldigSuffiks
}