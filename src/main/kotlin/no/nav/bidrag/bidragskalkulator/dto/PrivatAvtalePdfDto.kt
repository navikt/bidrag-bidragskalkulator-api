package no.nav.bidrag.bidragskalkulator.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.NavSkjemaId
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.Språkkode
import no.nav.bidrag.bidragskalkulator.utils.tilNorskDatoFormat

private const val FEILMELDING_FORNAVN = "Fornavn må være utfylt"
private const val FEILMELDING_ETTERNAVN = "Etternavn må være utfylt"
private const val FEILMELDING_FODSELSNUMMER = "Gyldig fødselsnummer må være utfylt"

interface PrivatAvtalePerson {
    val fornavn: String
    val etternavn: String
    val ident: String
}

@Schema(description = "Representerer informasjon om bidragsmottaker i en privat avtale")
data class PrivatAvtaleBidragsmottaker(
    @param:NotEmpty(message = FEILMELDING_FORNAVN)
    @param:Schema(
        description = "Bidragsmottakers fornavn fra folkeregisteret",
        required = true,
        example = "Ola"
    )
    override val fornavn: String,

    @param:NotEmpty(message = FEILMELDING_ETTERNAVN)
    @param:Schema(
        description = "Bidragsmottakers etternavn fra folkeregisteret",
        required = true,
        example = "Nordmann"
    )
    override val etternavn: String,

    @param:NotEmpty(message = FEILMELDING_FODSELSNUMMER)
    @param:Schema(
        description = "Bidragsmottakers personnummer (11 siffer)",
        required = true,
        example = "12345678901"
    )
    override val ident: String,
) : PrivatAvtalePerson

data class PrivatAvtaleBidragspliktig(
    @param:NotEmpty(message = FEILMELDING_FORNAVN)
    @param:Schema(
        description = "Bidragspliktig fornavn fra folkeregisteret",
        required = true,
        example = "Ola"
    )
    override val fornavn: String,

    @param:NotEmpty(message = FEILMELDING_ETTERNAVN)
    @param:Schema(
        description = "Bidragspliktig etternavn fra folkeregisteret",
        required = true,
        example = "Nordmann"
    )
    override val etternavn: String,

    @param:NotEmpty(message = FEILMELDING_FODSELSNUMMER)
    @param:Schema(
        description = "Bidragspliktig personnummer (11 siffer)",
        required = true,
        example = "12345678901"
    )
    override val ident: String,
) : PrivatAvtalePerson

@Schema(description = "Informasjon om barnet i en privat avtale")
data class PrivatAvtaleBarn(
    @param:NotEmpty(message = FEILMELDING_FORNAVN)
    @param:Schema(
        description = "Barn fornavn fra folkeregisteret",
        required = true,
        example = "Ola"
    )
    override val fornavn: String,

    @param:NotEmpty(message = FEILMELDING_ETTERNAVN)
    @param:Schema(
        description = "Barn etternavn fra folkeregisteret",
        required = true,
        example = "Nordmann"
    )
    override val etternavn: String,

    @param:NotEmpty(message = FEILMELDING_FODSELSNUMMER)
    @param:Schema(
        description = "Barn personnummer (11 siffer)",
        required = true,
        example = "12345678901"
    )
    override val ident: String,

    @param:Min(value = 1, message = "Bidraget må være større enn 0")
    @param:Schema(description = "Barnets fødselsnummer", required = true)
    val sumBidrag: Double,  // Beløp in NOK

    @param:Schema(description = "Gjeldende dato for avtalen", required = true, example = "2022-01-01")
    val fraDato: String,
) : PrivatAvtalePerson

enum class Oppgjørsform {
    INNKREVING, PRIVAT
}

enum class Vedleggskrav {
    SENDES_MED_SKJEMA,
    INGEN_EKSTRA_DOKUMENTASJON,
}

@Schema(description = "Andre bestemmelser tilknyttet avtalen")
data class AndreBestemmelserSkjema(
    @param:Schema(description = "Angir om det er andre bestemmelser")
    val harAndreBestemmelser: Boolean,

    @param:Schema(description = "Tekstlig beskrivelse dersom andre bestemmelser er valgt")
    val beskrivelse: String? = null
)

@Schema(description = "Informasjon for generering av en privat avtale PDF")
data class PrivatAvtalePdfDto(
    @param:Schema(ref = "#/components/schemas/NavSkjemaId", required = true)
    val navSkjemaId: NavSkjemaId,

    @param:Schema(ref = "#/components/schemas/Språkkode", required = true)
    val språk: Språkkode,

    @param:Schema(description = "Informasjon om bidragsmottaker", required = true)
    val bidragsmottaker: PrivatAvtaleBidragsmottaker,

    @param:Schema(description = "Informasjon om bidragspliktig", required = true)
    val bidragspliktig: PrivatAvtaleBidragspliktig,

    @param:Schema(description = "Informasjon om barnet", required = true)
    val barn: List<PrivatAvtaleBarn>,

    @param:Schema(description = "Opplysninger om oppgjør av barnebidrag, inkludert om avtalen er ny eller en endring, " +
            "nåværende oppgjørsform og ønsket oppgjørsform.", required = true)
    @field:NotNull
    val oppgjør: Oppgjør,

    @field:Valid
    @field:NotNull
    @param:Schema(ref = "#/components/schemas/Vedleggskrav")
    val vedlegg: Vedleggskrav,

    @field:NotNull
    @field:Valid
    @param:Schema(description = "Eventuelle andre bestemmelser som er inkludert i avtalen")
    val andreBestemmelser: AndreBestemmelserSkjema
)  {
    @JsonIgnore
    @Schema(hidden = true)
    fun medNorskeDatoer(): PrivatAvtalePdfDto = this.copy(
        barn = this.barn.map { it.copy(fraDato = it.fraDato.tilNorskDatoFormat()) },
    )
}

@Schema(
    description = "Hvis dette er en endring av en eksisterende avtale, må `oppgjorsformIdag` angi nåværende oppgjørsform. " +
            "Dersom oppgjørsformen endres i den nye avtalen, skal en kopi av avtalen sendes til NAV. " +
            "Eksempel: Hvis oppgjorsformIdag er INNKREVING og oppgjørsformØnsket er PRIVATE, eller motsatt, " +
            "må kopi av ny avtale sendes til Nav."
)
data class Oppgjør(
    @param:Schema(description = "Er dette en ny avtale?", required = true)
    val nyAvtale: Boolean,

    @param:Schema(description ="Hvilken oppgjørsform ønskes?",  ref = "#/components/schemas/Oppgjørsform", required = true)
    val oppgjørsformØnsket: Oppgjørsform,

    @param:Schema(
        description = "Hvilken oppgjørsform har dere i dag?", ref = "#/components/schemas/Oppgjørsform"
    )
    val oppgjørsformIdag: Oppgjørsform? = null
)

fun Oppgjør.erOppgjørsformEndret(): Boolean =  !this.nyAvtale && this.oppgjørsformIdag != null && this.oppgjørsformØnsket !== this.oppgjørsformIdag
