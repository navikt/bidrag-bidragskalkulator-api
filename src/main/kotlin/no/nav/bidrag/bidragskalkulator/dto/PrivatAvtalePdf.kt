package no.nav.bidrag.bidragskalkulator.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.Språkkode
import no.nav.bidrag.bidragskalkulator.utils.tilNorskDatoFormat
import no.nav.bidrag.bidragskalkulator.validering.ValidAndreBestemmelser
import no.nav.bidrag.bidragskalkulator.validering.ValidOppgjør
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

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
        description = "Partens personnummer (11 siffer)",
        required = true,
        example = "12345678901"
    )
    override val ident: Personident,
) : PrivatAvtalePerson

@Schema(description = "Informasjon om barnet i en privat avtale")
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
        description = "Barn personnummer (11 siffer)",
        required = true,
        example = "12345678901"
    )
    override val ident: Personident,

    @param:Min(value = 1, message = "Bidraget må være større enn 0")
    @param:Schema(description = "Barnets fødselsnummer", required = true, example = "2000")
    val sumBidrag: BigDecimal,  // Beløp in NOK

    @param:Schema(description = "Gjelder fra og med dato (YYYY-MM-DD)", required = true, example = "2022-01-01")
    val fraDato: String,
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

@Schema(description = "Informasjon om bidrag som skal betales i en privat avtale")
data class Bidrag(
    @param:Min(value = 1, message = "Beløpet må være større enn 0")
    @param:Schema(description = "Bidragsbeløp per måned i NOK", required = true, example = "1000")
    val bidragPerMåned: BigDecimal,

    @param:Schema(description = "Bidraget skal betales fra og med", required = true, example = "01-2025")
    val fraDato: String,

    @param:Schema(description = "Bidraget skal betales til og med", required = true, example = "12-2025")
    val tilDato: String
)

@Schema(description = "Informasjon for generering av en privat avtale PDF for barn under 18 år")
data class PrivatAvtaleBarnUnder18RequestDto(
    @param:Schema(ref = "#/components/schemas/Språkkode", required = true)
    override val språk: Språkkode,

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
    override val oppgjør: Oppgjør,

    @field:Valid
    @field:NotNull
    @param:Schema(ref = "#/components/schemas/Vedleggskrav", required = true)
    override val vedlegg: Vedleggskrav,

    @field:NotNull
    @field:Valid
    @param:Schema(description = "Eventuelle andre bestemmelser som er inkludert i avtalen", required = true)
    override val andreBestemmelser: AndreBestemmelserSkjema
): PrivatAvtalePdf  {
    @JsonIgnore
    @Schema(hidden = true)
    fun medNorskeDatoer(): PrivatAvtaleBarnUnder18RequestDto = this.copy(
        barn = this.barn.map { it.copy(fraDato = it.fraDato.tilNorskDatoFormat()) },
    )
}

@Schema(description = "Informasjon for generering av en privat avtale PDF for barn over 18 år")
data class PrivatAvtaleBarnOver18RequestDto (
    @param:Schema(ref = "#/components/schemas/Språkkode", required = true)
    override val språk: Språkkode,

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
    override val oppgjør: Oppgjør,

    @field:NotNull
    @param:Schema(ref = "#/components/schemas/Vedleggskrav")
    override val vedlegg: Vedleggskrav,

    @field:NotNull
    @param:Schema(description = "Andre bestemmelser som er inkludert i avtalen")
    override val andreBestemmelser: AndreBestemmelserSkjema
): PrivatAvtalePdf
