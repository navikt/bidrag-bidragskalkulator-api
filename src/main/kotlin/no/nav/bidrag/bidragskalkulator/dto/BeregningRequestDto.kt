package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Min
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

@Schema(description = "Angir hvilken rolle den påloggede personen har i bidragsberegningen")
enum class BidragsType {
    PLIKTIG, // Pålogget person er bidragspliktig
    MOTTAKER // Pålogget person er bidragsmottaker
}

interface IFellesBarnDto {
    val bidragstype: BidragsType
    val samværsklasse: Samværsklasse
    val barnetilsynsutgift: BigDecimal?
    val inntekt: BigDecimal?
}

@Schema(description = "Informasjon om et barn i beregningen")
data class BarnMedIdentDto(
    @field:NotNull(message = "Barnets identifikator må være satt")
    @param:Schema(description = "Fødselsnummer eller D-nummer til barnet", required = true, example = "12345678901")
    val ident: Personident,

    @field:NotNull(message = "Samværsklasse må være satt")
    @param:Schema(ref = "#/components/schemas/Samværsklasse") // Reference dynamically registered schema. See BeregnBarnebidragConfig
    override val samværsklasse: Samværsklasse,

    @field:NotNull(message = "Bidragstype må være satt")
    @param:Schema(description = "Angir om den påloggede personen er pliktig eller mottaker for dette barnet", required = true)
    override val bidragstype: BidragsType,

    @param:Schema(description = "Utgifter i kroner per måned som den bidragsmottaker har til barnetilsyn for dette barnet", required = false, example = "2000")
    @field:Min(value = 0)
    override val barnetilsynsutgift: BigDecimal? = null,

    @param:Schema(
        description = "Inntekt i kroner per måned for dette barnet. Oppgis kun hvis barnet har egen inntekt.",
        required = false,
        example = "5000"
    )
    @field:Min(value = 0)
    override val inntekt: BigDecimal? = null,
) : IFellesBarnDto

@Schema(description = "Modellen brukes til å beregne barnebidragbasert på barnets id")
data class BeregningRequestDto(
    @field:NotNull(message = "Inntekt for forelder 1 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 1 kan ikke være negativ")
    @param:Schema(description = "Inntekt for forelder 1 i norske kroner", required = true, example = "500000.0")
    override val inntektForelder1: Double,

    @field:NotNull(message = "Inntekt for forelder 2 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 2 kan ikke være negativ")
    @param:Schema(description = "Inntekt for forelder 2 i norske kroner", required = true, example = "450000.0")
    override val inntektForelder2: Double,

    @field:NotEmpty(message = "Liste over barn kan ikke være tom")
    @field:Valid
    override val barn: List<BarnMedIdentDto>,

    @param:Schema(description = "Boforhold for den påloggede personen. Må være satt hvis bidragstype for minst ett barn er PLIKTIG", required = false)
    override val dittBoforhold: BoforholdDto? = null,

    @param:Schema(description = "Boforhold for den andre forelderen. Må være satt hvis bidragstype for minst ett barn er MOTTAKER", required = false)
    override val medforelderBoforhold: BoforholdDto? = null,
) : FellesBeregningRequestDto<BarnMedIdentDto>(
    inntektForelder1, inntektForelder2, barn, dittBoforhold, medforelderBoforhold
)

@Schema(description = "Boforholdsinformasjon for en forelder")
data class BoforholdDto(
    @field:NotNull(message = "Antall barn som bor fast hos forelderen må være satt")
    @param:Schema(description = "Antall barn under 18 år som bor fast hos forelderen", required = true, example = "3")
    val antallBarnBorFast: Int,

    @field:NotNull(message = "Antall barn som har avtalt delt bosted hos forelderen må være satt")
    @param:Schema(description = "Antall barn under 18 år med delt bosted hos forelderen", required = true, example = "3")
    val antallBarnDeltBosted: Int,

    @field:NotNull(message = "Indikator om forelderen deler bolig med en annen voksen må være satt")
    @param:Schema(description = "Indikerer om forelderen deler bolig med en annen voksen", required = true, example = "false")
    val borMedAnnenVoksen: Boolean,
)

abstract class FellesBeregningRequestDto<T : IFellesBarnDto>(
    open val inntektForelder1: Double,
    open val inntektForelder2: Double,
    open val barn: List<T>,
    open val dittBoforhold: BoforholdDto? = null,
    open val medforelderBoforhold: BoforholdDto? = null,
)
