package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Min
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.ident.Personident

@Schema(description = "Angir hvilken rolle den påloggede personen har i bidragsberegningen")
enum class BidragsType {
    PLIKTIG, // Pålogget person er bidragspliktig
    MOTTAKER // Pålogget person er bidragsmottaker
}

@Schema(description = "Informasjon om et barn i beregningen")
data class BarnDto(
    @field:NotNull(message = "Barnets identifikator må være satt")
    @Schema(description = "Fødselsnummer eller D-nummer til barnet", required = true, example = "12345678901")
    val ident: Personident,

    @field:NotNull(message = "Samværsklasse må være satt")
    @Schema(ref = "#/components/schemas/Samværsklasse") // Reference dynamically registered schema. See BeregnBarnebidragConfig
    val samværsklasse: Samværsklasse,

    @field:NotNull(message = "Bidragstype må være satt")
    @Schema(description = "Angir om den påloggede personen er pliktig eller mottaker for dette barnet", required = true)
    val bidragstype: BidragsType
)

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
    val barn: List<BarnDto>,

    @Schema(description = "Boforhold for den påloggede personen. Må være satt hvis bidragstype for barnet er PLIKTIG", required = false)
    val dinBoforhold: BoforholdDto? = null,

    @Schema(description = "Boforhold for den andre forelderen. Må være satt hvis bidragstype for barnet er MOTTAKER", required = false)
    val medforelderBoforhold: BoforholdDto? = null,
)

@Schema(description = "Boforholdsinformasjon for en forelder")
data class BoforholdDto(
    @field:NotNull(message = "Antall barn som bor fast hos forelderen må være satt")
    @Schema(description = "Antall barn under 18 år som bor fast hos forelderen", required = true, example = "3")
    val antallBarnBorFast: Int,

    @field:NotNull(message = "Antall barn som har avtalt delt bosted hos forelderen må være satt")
    @Schema(description = "Antall barn under 18 år med delt bosted hos forelderen", required = true, example = "3")
    val antallBarnDeltBosted: Int,

    @field:NotNull(message = "Indikator om forelderen deler bolig med en annen voksen må være satt")
    @Schema(description = "Indikerer om forelderen deler bolig med en annen voksen", required = true, example = "false")
    val borMedAnnenVoksen: Boolean,
)