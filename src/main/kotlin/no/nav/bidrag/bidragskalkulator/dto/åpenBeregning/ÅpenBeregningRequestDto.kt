package no.nav.bidrag.bidragskalkulator.dto.åpenBeregning

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.annotation.Nullable
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.bidragskalkulator.dto.*
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "Informasjon om et barn i beregningen")
data class BarnMedFødselsdatoDto(
    @field:NotNull(message = "Fødselsdato må være satt")
    @param:Schema(description = "Fødselsdato til barnet", required = true, example = "2015-06-30")
    val fødselsdato: LocalDate,

    @field:NotNull(message = "Samværsklasse må være satt")
    @param:Schema(ref = "#/components/schemas/Samværsklasse") // Reference dynamically registered schema. See BeregnBarnebidragConfig
    override val samværsklasse: Samværsklasse,

    @param:Schema(description = "Opplysninger om barnetilsyn for dette barnet.",
        required = false,
        nullable = true,
        implementation = BarnetilsynDto::class)
    override val barnetilsyn: BarnetilsynDto? = null,

    @field:Nullable
    @param:Schema(
        description = "Inntekt i kroner per måned for dette barnet. Oppgis kun hvis barnet har egen inntekt.",
        required = false,
        example = "5000"
    )
    override val inntekt: BigDecimal? = null,

    @param:Schema(
        description ="Kontantstøtte knyttet til dette barnet.",
        required = false,
        nullable = true,
        implementation = KontantstøtteDto::class
    )
    override val kontantstøtte: KontantstøtteDto? = null,
): IFellesBarnDto {
    @JsonIgnore
    @Schema(hidden = true) // Hides from Swagger
    fun getAlder(): Int = LocalDate.now().year - fødselsdato.year

    @JsonIgnore
    @Schema(hidden = true)
    fun getAntallMåneder(): Int {
        val today = LocalDate.now()
        return (today.year - fødselsdato.year) * 12 + (today.monthValue - fødselsdato.monthValue)
    }
}

@Schema(description = "Modellen brukes til å beregne barnebidrag basert på barnets alder")
data class ÅpenBeregningRequestDto(
    @field:NotEmpty(message = "Liste over barn kan ikke være tom")
    @field:Valid
    @param:Schema(description = "Liste over barn som inngår i beregningen", required = true)
    override val barn: List<BarnMedFødselsdatoDto>,

    @param:Schema(description = "Boforhold for den påloggede personen. Må være satt hvis bidragstype for minst ett barn er PLIKTIG", required = false)
    override val dittBoforhold: BoforholdDto? = null,

    @param:Schema(description = "Boforhold for den andre forelderen. Må være satt hvis bidragstype for minst ett barn er MOTTAKER", required = false)
    override val medforelderBoforhold: BoforholdDto? = null,

    @param:Schema(
        description = "Opplysninger om utvidet barnetrygd. Kan utelates dersom det ikke foreligger utvidet barnetrygd.",
        nullable = true,
        implementation = UtvidetBarnetrygdDto::class
    )
    override val utvidetBarnetrygd: UtvidetBarnetrygdDto? = null,

    @param:Schema(
        description = "Angir om bidragsmottaker mottar småbarnstillegg. Gjelder kun når minst ett barn er 0–3 år.",
        required = true,
        example = "false",
    )
    override val småbarnstillegg: Boolean = false,

    @field:NotNull(message = "Bidragsmottaker sin inntekt må være satt")
    @field:Valid
    @param:Schema(
        description = "Inntektsopplysninger for bidragsmottaker (BM).",
        required = true,
        implementation = ForelderInntektDto::class
    )
    override val bidragsmottakerInntekt: ForelderInntektDto,

    @field:NotNull(message = "Bidragspliktig sin inntekt må være satt")
    @field:Valid
    @param:Schema(
        description = "Inntektsopplysninger for bidragspliktig (BP).",
        required = true,
        implementation = ForelderInntektDto::class
    )
    override val bidragspliktigInntekt: ForelderInntektDto,

    @field:NotNull(message = "Bidragstype må være satt")
    @param:Schema(description = "Angir om den personen er pliktig eller mottaker", required = true)
    override val bidragstype: BidragsType,
) : FellesBeregningRequestDto<BarnMedFødselsdatoDto>(
    bidragsmottakerInntekt, bidragspliktigInntekt, bidragstype, barn, dittBoforhold, medforelderBoforhold, utvidetBarnetrygd, småbarnstillegg
)
