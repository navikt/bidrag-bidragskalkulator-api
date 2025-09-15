package no.nav.bidrag.bidragskalkulator.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.NavSkjemaId
import no.nav.bidrag.bidragskalkulator.dto.førstesidegenerator.Språkkode
import no.nav.bidrag.bidragskalkulator.validering.GyldigPeriode
import no.nav.bidrag.bidragskalkulator.validering.ValidAndreBestemmelser
import no.nav.bidrag.bidragskalkulator.validering.ValidOppgjør
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

private const val FEILMELDING_FORNAVN = "Fornavn må være utfylt"
private const val FEILMELDING_ETTERNAVN = "Etternavn må være utfylt"
private const val FEILMELDING_FODSELSNUMMER = "Gyldig fødselsnummer må være utfylt"

enum class Oppgjørsform {
    INNKREVING, PRIVAT
}

enum class Vedleggskrav {
    SENDES_MED_SKJEMA,
    INGEN_EKSTRA_DOKUMENTASJON,
}

interface PrivatAvtalePerson {
    val fornavn: String
    val etternavn: String
    val ident: Personident
}

sealed interface PrivatAvtalePdf {
    val språk: Språkkode
    val bidragstype: BidragsType
    val bidragsmottaker: PrivatAvtalePart
    val bidragspliktig: PrivatAvtalePart
    val oppgjør: Oppgjør
    val vedlegg: Vedleggskrav
    val andreBestemmelser: AndreBestemmelserSkjema
}

@Schema(description = "Representerer informasjon om part i en privat avtale")
data class PrivatAvtalePart(
    @param:NotBlank(message = FEILMELDING_FORNAVN)
    @param:Schema(
        description = "Partens fornavn fra folkeregisteret",
        required = true,
        example = "Ola"
    )
    override val fornavn: String,

    @param:NotBlank(message = FEILMELDING_ETTERNAVN)
    @param:Schema(
        description = "Partens etternavn fra folkeregisteret",
        required = true,
        example = "Nordmann"
    )
    override val etternavn: String,

    @param:NotBlank(message = FEILMELDING_FODSELSNUMMER)
    @param:Schema(
        description = "Partens personnummer r eller d-nummer (11 siffer)",
        required = true,
        example = "12345678901"
    )
    override val ident: Personident,
) : PrivatAvtalePerson

@Schema(description = "Informasjon om barn under 18 år i en privat avtale")
data class PrivatAvtaleBarn(
    @param:NotBlank(message = FEILMELDING_FORNAVN)
    @param:Schema(
        description = "Barn fornavn fra folkeregisteret",
        required = true,
        example = "Ola"
    )
    override val fornavn: String,

    @param:NotBlank(message = FEILMELDING_ETTERNAVN)
    @param:Schema(
        description = "Barn etternavn fra folkeregisteret",
        required = true,
        example = "Nordmann"
    )
    override val etternavn: String,

    @param:NotBlank(message = FEILMELDING_FODSELSNUMMER)
    @param:Schema(
        description = "Barnets personnummer eller d-nummer (11 siffer)",
        required = true,
        example = "12345678901"
    )
    override val ident: Personident,

    @param:Schema(description = "Barnets fødselsnummer eller d-nummer (11 siffer)", required = true, example = "12345678901")
    val sumBidrag: BigDecimal,  // Beløp in NOK

    @param:Schema(description = "Gjelder fra og med dato (dd.MM.yyyy)", required = true, example = "01.01.2025")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    val fraDato: LocalDate,
) : PrivatAvtalePerson

@ValidAndreBestemmelser
@Schema(description = "Andre bestemmelser tilknyttet avtalen")
data class AndreBestemmelserSkjema(
    @param:Schema(description = "Angir om det finnes andre bestemmelser")
    val harAndreBestemmelser: Boolean,

    @param:Schema(description = "Beskrivelse dersom andre bestemmelser er valgt")
    val beskrivelse: String? = null
)

@ValidOppgjør
@Schema(
    description = "Hvis dette er en endring av en eksisterende avtale, må `oppgjorsformIdag` angi nåværende oppgjørsform. " +
            "Dersom oppgjørsformen endres i den nye avtalen, skal en kopi av avtalen sendes til Nav. " +
            "Eksempel: Hvis oppgjorsformIdag er INNKREVING og oppgjørsformØnsket er PRIVATE, eller motsatt, " +
            "må kopi av ny avtale sendes til Nav."
)
data class Oppgjør(
    @param:Schema(description = "Er dette en ny avtale?", required = true)
    val nyAvtale: Boolean,

    @param:Schema(description ="Ønsket oppgjørsform",  ref = "#/components/schemas/Oppgjørsform", required = true)
    val oppgjørsformØnsket: Oppgjørsform,

    @param:Schema(
        description = "Oppgjørsform i dag", ref = "#/components/schemas/Oppgjørsform"
    )
    val oppgjørsformIdag: Oppgjørsform? = null
)

@GyldigPeriode
@Schema(description = "Informasjon om bidrag som skal betales i en privat avtale for barn over 18 år")
data class Bidrag(
    @param:Schema(description = "Bidragsbeløp per måned i NOK", required = true, example = "1000")
    val bidragPerMåned: BigDecimal,

    @param:Schema(description = "Bidraget skal betales fra og med", required = true, example = "2025-01")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM")
    val fraDato: YearMonth,

    @param:Schema(description = "Bidraget skal betales til og med", required = true, example = "2025-02")
    @param:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM")
    val tilDato: YearMonth
)

@Schema(description = "Informasjon for generering av en privat avtale PDF for barn under 18 år")
data class PrivatAvtaleBarnUnder18RequestDto(
    @param:Schema(ref = "#/components/schemas/Språkkode", required = true)
    override val språk: Språkkode,

    @param:Schema(ref = "#/components/schemas/BidragsType", required = true)
    override val bidragstype: BidragsType,

    @field:Valid
    @param:Schema(description = "Informasjon om bidragsmottaker", required = true)
    override val bidragsmottaker: PrivatAvtalePart,

    @field:Valid
    @param:Schema(description = "Informasjon om bidragspliktig", required = true)
    override val bidragspliktig: PrivatAvtalePart,

    @param:Schema(description = "Informasjon om barnet", required = true)
    @field:Size(min = 1, message = "Minst ett barn må oppgis")
    @field:Valid
    val barn: List<PrivatAvtaleBarn>,

    @param:Schema(description = "Opplysninger om oppgjør (ny/endring, oppgjørsform i dag og ønsket oppgjørsform)", required = true)
    @field:NotNull
    @field:Valid
    override val oppgjør: Oppgjør,

    @field:Valid
    @field:NotNull
    @param:Schema(ref = "#/components/schemas/Vedleggskrav", required = true)
    override val vedlegg: Vedleggskrav,

    @field:NotNull
    @field:Valid
    @param:Schema(description = "Eventuelle andre bestemmelser som er inkludert i avtalen", required = true)
    override val andreBestemmelser: AndreBestemmelserSkjema
): PrivatAvtalePdf

@Schema(description = "Informasjon for generering av en privat avtale PDF for barn over 18 år")
data class PrivatAvtaleBarnOver18RequestDto (
    @param:Schema(ref = "#/components/schemas/Språkkode", required = true)
    override val språk: Språkkode,

    @param:Schema(ref = "#/components/schemas/BidragsType", required = true)
    override val bidragstype: BidragsType,

    @field:Valid
    @param:Schema(description = "Informasjon om bidragsmottaker (barn over 18 år)", required = true)
    override val bidragsmottaker: PrivatAvtalePart,

    @field:Valid
    @param:Schema(description = "Informasjon om bidragspliktig", required = true)
    override val bidragspliktig: PrivatAvtalePart,

    @param:Schema(description = "Informasjon om bidrag som skal betales i en privat avtale", required = true)
    @field:Size(min = 1, message = "Minst ett bidrag må oppgis")
    @field:Valid
    val bidrag: List<Bidrag>,

    @param:Schema(description = "Opplysninger om oppgjør av barnebidrag, inkludert om avtalen er ny eller en endring, " +
            "nåværende oppgjørsform og ønsket oppgjørsform.", required = true)
    @field:NotNull
    @field:Valid
    override val oppgjør: Oppgjør,

    @field:NotNull
    @param:Schema(ref = "#/components/schemas/Vedleggskrav")
    override val vedlegg: Vedleggskrav,

    @field:NotNull
    @field:Valid
    @param:Schema(description = "Andre bestemmelser som er inkludert i avtalen", required = true)
    override val andreBestemmelser: AndreBestemmelserSkjema
): PrivatAvtalePdf

data class GenererPrivatAvtalePdfRequest(
   val privatAvtalePdf: PrivatAvtalePdf,
    val navSkjemaId: NavSkjemaId
)
