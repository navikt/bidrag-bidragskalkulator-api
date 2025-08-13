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
    val sumBidrag: Double  // Beløp in NOK
) : PrivatAvtalePerson

enum class Oppgjørsform {
    INNKREVING, PRIVAT
}

enum class TilknyttetAvtaleVedlegg {
    SENDES_MED_SKJEMA,
    ETTERSENDES,
    LEVERT_TIDLIGERE
}

enum class AnnenDokumentasjon {
    SENDES_MED_SKJEMA,
    INGEN_EKSTRA_DOKUMENTASJON,
}

@Schema(description = "Skjema for vedlegg tilknyttet avtalen")
data class Vedlegg(
    @field:NotNull
    @param:Schema(ref = "#/components/schemas/TilknyttetAvtaleVedlegg")
    val tilknyttetAvtale: TilknyttetAvtaleVedlegg,
    @field:NotNull
    @param:Schema(ref = "#/components/schemas/AnnenDokumentasjon")
    val annenDokumentasjon: AnnenDokumentasjon
)


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
    @param:Schema(description = "Er dette en ny avtale?", required = true)
    val nyAvtale: Boolean,
    @param:Schema(ref = "#/components/schemas/Oppgjørsform", required = true)
    val oppgjorsform: Oppgjørsform,
    @param:Schema(description = "Er dette en avtale som skal sendes til NAV?", required = true)
    val tilInnsending: Boolean = false,
    @param:Schema(description = "Gjeldende dato for avtalen")
    val fraDato: String,
    @field:NotNull
    @field:Valid
    @param:Schema(description = "Vedlegg knyttet til avtalen")
    val vedlegg: Vedlegg,
    @field:NotNull
    @field:Valid
    @param:Schema(description = "Eventuelle andre bestemmelser som er inkludert i avtalen")
    val andreBestemmelser: AndreBestemmelserSkjema
)  {
    @JsonIgnore
    @Schema(hidden = true)
    fun tilNorskDatoFormat(): PrivatAvtalePdfDto =
        this.copy(fraDato = fraDato.tilNorskDatoFormat())
}
