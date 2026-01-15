package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Min
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

@Schema(description = "Angir hvilken rolle den påloggede personen har i bidragsberegningen")
enum class BidragsType {
    PLIKTIG, // Pålogget person er bidragspliktig
    MOTTAKER // Pålogget person er bidragsmottaker
}

@Schema(description = "Type voksne over 18 år i husholdningen")
enum class VoksneOver18Type {
    SAMBOER_ELLER_EKTEFELLE,
    EGNE_BARN_OVER_18,
}

@Schema(
    description = "Opplysninger om barnepass/barnetilsyn for barnet. " +
            "Enten oppgis faktisk månedlig utgift (beløp), eller så oppgis at det mottas stønad til barnetilsyn med plass-type."
)
data class BarnetilsynDto(
    @param:Schema(
        description = "Månedlig utgift i kroner. Brukes når det ikke mottas stønad til barnetilsyn.",
        example = "2000",
        required = false
    )
    @field:Min(0)
    @field:DecimalMin(value = "0.00", inclusive = true, message = "Barnetilsynutgift kan ikke være negativ")
    val månedligUtgift: BigDecimal? = null,

    @param:Schema(
        description = "Plass-type (heltid/deltid) når det mottas stønad til barnetilsyn.",
        required = false,
        ref = "#/components/schemas/Tilsynstype"
    )
    val plassType: Tilsynstype? = null,
)

@Schema(description = "Opplysninger om kontantstøtte knyttet til barnet.")
data class KontantstøtteDto(
    @field:Min(0)
    @field:DecimalMin(value = "0.00", inclusive = true, message = "Kontantstøtte kan ikke være negativ")
    @param:Schema(
        description = "Kontantstøtte per måned knyttet til barnet (relevant kun når alder = 1). " +
            "Beløpet legges til inntekt for bidragsmottaker (BM).",
        example = "7500",
        required = false)
    val beløp: BigDecimal? = null,

    @param:Schema(
        description = "Angir om kontantstøtte skal deles mellom foreldrene.",
        example = "false",
        required = false
    )
    val deles: Boolean? = null
)

interface IFellesBarnDto {
    val samværsklasse: Samværsklasse
    val barnetilsyn: BarnetilsynDto?
    val inntekt: BigDecimal?
    val kontantstøtte: KontantstøtteDto?
}

@Schema(description = "Informasjon om et barn i beregningen")
data class BarnMedIdentDto(
    @field:NotNull(message = "Barnets identifikator må være satt")
    @param:Schema(description = "Fødselsnummer eller D-nummer til barnet", required = true, example = "12345678901")
    val ident: Personident,

    @field:NotNull(message = "Samværsklasse må være satt")
    @param:Schema(ref = "#/components/schemas/Samværsklasse") // Reference dynamically registered schema. See BeregnBarnebidragConfig
    override val samværsklasse: Samværsklasse,

    @param:Schema(description = "Opplysninger om barnetilsyn for dette barnet.",
        required = false,
        nullable = true,
        implementation = BarnetilsynDto::class)
    override val barnetilsyn: BarnetilsynDto? = null,

    @param:Schema(
        description = "Inntekt i kroner per måned for dette barnet. Oppgis kun hvis barnet har egen inntekt.",
        required = false,
        example = "5000"
    )
    @field:Min(value = 0)
    @field:DecimalMin(value = "0.00", inclusive = true, message = "Barnets inntekt kan ikke være negativ")
    override val inntekt: BigDecimal? = null,

    @param:Schema(
        description ="Kontantstøtte knyttet til dette barnet.",
        required = false,
        nullable = true,
        implementation = KontantstøtteDto::class
    )
    override val kontantstøtte: KontantstøtteDto? = null,
) : IFellesBarnDto

@Schema(description = "Modellen brukes til å beregne barnebidragbasert på barnets id")
data class BeregningRequestDto(
    @field:NotEmpty(message = "Liste over barn kan ikke være tom")
    @field:Valid
    override val barn: List<BarnMedIdentDto>,

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
) : FellesBeregningRequestDto<BarnMedIdentDto>(
    bidragsmottakerInntekt, bidragspliktigInntekt, bidragstype, barn, dittBoforhold, medforelderBoforhold, utvidetBarnetrygd, småbarnstillegg
)

@Schema(description = "Boforholdsinformasjon for en forelder")
data class BoforholdDto(
    @field:NotNull(message = "Antall barn som bor fast hos forelderen må være satt")
    @param:Schema(description = "Antall barn under 18 år som bor fast hos forelderen", required = true, example = "3")
    val antallBarnUnder18BorFast: Int,

    @param:Schema(
        description = "Typer voksne over 18 år i husholdningen (kan være null).",
        required = false
    )
    val voksneOver18Type: Set<VoksneOver18Type>? = null,

    @field:Min(0)
    @param:Schema(
        description = "Antall egne barn over 18 år som går på videregående skole. Må settes hvis voksneOver18Type inneholder EGNE_BARN_OVER_18.",
        required = false,
        example = "1"
    )
    val antallBarnOver18Vgs: Int? = null,
)

@Schema(
    description = "Opplysninger om utvidet barnetrygd for beregningen. Brukes når bidragsmottaker har utvidet barnetrygd og eventuelt deler denne med bidragspliktig."
)
data class UtvidetBarnetrygdDto(
    @param:Schema(
        description = "Om det foreligger utvidet barnetrygd i perioden som beregningen gjelder for.",
        example = "true",
        required = true
    )
    val harUtvidetBarnetrygd: Boolean,

    @param:Schema(
        description = "Om utvidet barnetrygd deles med bidragspliktig. Relevant kun når harUtvidetBarnetrygd = true.",
        example = "false",
        required = true
    )
    val delerMedMedforelder: Boolean,
)

@Schema(description = "Inntektsopplysninger for en forelder som brukes i beregningen.")
data class ForelderInntektDto(
    @field:NotNull(message = "Inntekt må være satt")
    @field:DecimalMin(value = "0.00", inclusive = true, message = "Inntekt kan ikke være negativ")
    @field:Digits(integer = 12, fraction = 2, message = "Inntekt må ha maks 2 desimaler")
    @param:Schema(
        description = "Årlig inntekt i kroner.",
        required = true,
        example = "550000.00"
    )
    val inntekt: BigDecimal,

    @field:DecimalMin(value = "0.00", inclusive = true, message = "Netto positiv kapitalinntekt kan ikke være negativ")
    @field:Digits(integer = 12, fraction = 2, message = "Netto positiv kapitalinntekt må ha maks 2 desimaler")
    @param:Schema(
        description =
            "Årlig netto positiv kapitalinntekt, for eksempel inntekter fra utleie. Beløp under 10 000 kr per år vil ikke påvirke beregningen på grunn av bunntrekk.",
        required = false,
        example = "25000.00",
        defaultValue = "0.00"
    )
    val nettoPositivKapitalinntekt: BigDecimal = BigDecimal.ZERO,
)

abstract class FellesBeregningRequestDto<T : IFellesBarnDto>(
    open val bidragsmottakerInntekt: ForelderInntektDto,
    open val bidragspliktigInntekt: ForelderInntektDto,
    open val bidragstype: BidragsType,
    open val barn: List<T>,
    open val dittBoforhold: BoforholdDto? = null,
    open val medforelderBoforhold: BoforholdDto? = null,
    open val utvidetBarnetrygd: UtvidetBarnetrygdDto? = null,
    open val småbarnstillegg: Boolean = false,
)
