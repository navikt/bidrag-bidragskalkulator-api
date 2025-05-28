package no.nav.bidrag.bidragskalkulator.dto.åpenBeregning

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import java.time.LocalDate

@Schema(description = "Informasjon om et barn i beregningen")
data class BarnMedAlderDto(
    @field:NotNull(message = "Alder må være satt")
    @field:Min(value = 0, message = "Alder kan ikke være negativ")
    @field:Max(value = 25, message = "Alder kan ikke være høyere enn 25")
    @Schema(description = "Alder til barnet", required = true, example = "10")
    val alder: Int,

    @field:NotNull(message = "Samværsklasse må være satt")
    @Schema(ref = "#/components/schemas/Samværsklasse") // Reference dynamically registered schema. See BeregnBarnebidragConfig
    override val samværsklasse: Samværsklasse,

    @field:NotNull(message = "Bidragstype må være satt")
    @Schema(description = "Angir om den påloggede personen er pliktig eller mottaker for dette barnet", required = true)
    override val bidragstype: BidragsType
): IFellesBarnDto {
    @JsonIgnore
    @Schema(hidden = true) // Hides from Swagger
    //Når barnet har alder = 15, blir fødselsmåneden alltid satt til juli, uavhengig av den faktiske fødselsdatoen (usikkert hvor denne regelen stammer fra).
    // Dette betyr at barnet ikke anses som 15 år før juli.
    // I alle beregningsperioder før juli vil barnet derfor fortsatt regnes som 14 år.
    fun getEstimertFødselsdato(): LocalDate = LocalDate.now().minusYears(alder.toLong())
}

@Schema(description = "Modellen brukes til å beregne barnebidrag basert på barnets alder")
data class ÅpenBeregningRequestDto(
    @field:NotNull(message = "Inntekt for forelder 1 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 1 kan ikke være negativ")
    @Schema(description = "Inntekt for forelder 1 i norske kroner", required = true, example = "500000.0")
    override val inntektForelder1: Double,

    @field:NotNull(message = "Inntekt for forelder 2 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 2 kan ikke være negativ")
    @Schema(description = "Inntekt for forelder 2 i norske kroner", required = true, example = "450000.0")
    override val inntektForelder2: Double,

    @field:NotEmpty(message = "Liste over barn kan ikke være tom")
    @field:Valid
    @Schema(description = "Liste over barn som inngår i beregningen", required = true)
    override val barn: List<BarnMedAlderDto>,

    @Schema(description = "Boforhold for den påloggede personen. Må være satt hvis bidragstype for minst ett barn er PLIKTIG", required = false)
    override val dittBoforhold: BoforholdDto? = null,

    @Schema(description = "Boforhold for den andre forelderen. Må være satt hvis bidragstype for minst ett barn er MOTTAKER", required = false)
    override val medforelderBoforhold: BoforholdDto? = null,
) : FellesBeregningRequestDto<BarnMedAlderDto>(
    inntektForelder1, inntektForelder2, barn, dittBoforhold, medforelderBoforhold
)