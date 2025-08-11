package no.nav.bidrag.bidragskalkulator.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.bidragskalkulator.dto.foerstesidegenerator.Språkkode
import no.nav.bidrag.bidragskalkulator.utils.tilNorskDatoFormat

interface PrivatAvtalePerson {
    val fulltNavn: String
    val fodselsnummer: String
}

@Schema(description = "Representerer informasjon om bidragsmottaker i en privat avtale")
data class PrivatAvtaleBidragsmottaker(
    @param:NotEmpty(message = FEILMELDING_NAVN)
    @param:Schema(
        description = "Bidragsmottakers fulle navn fra folkeregisteret",
        required = true,
        example = "Ola Nordmann"
    )
    override val fulltNavn: String,

    @param:NotEmpty(message = FEILMELDING_FODSELSNUMMER)
    @param:Schema(
        description = "Bidragsmottakers personnummer (11 siffer)",
        required = true,
        example = "12345678901"
    )
    override val fodselsnummer: String,
) : PrivatAvtalePerson {
    companion object {
        private const val FEILMELDING_NAVN = "Navn må være utfylt"
        private const val FEILMELDING_FODSELSNUMMER = "Gyldig fødselsnummer må være utfylt"
    }
}

data class PrivatAvtaleBidragspliktig(
    @param:NotEmpty(message = "fulltNavn må være satt")
    @param:Schema(description = "Bidragsmottakers fulle navn", required = true)
    override val fulltNavn: String,

    @param:NotEmpty(message = "Ident/fødselsnummer må være satt")
    @param:Schema(description = "Bidragsmottakers fødselsnummer", required = true)
    override val fodselsnummer: String,
) : PrivatAvtalePerson

@Schema(description = "Informasjon om barnet i en privat avtale")
data class PrivatAvtaleBarn(
    @param:NotEmpty(message = "fulltNavn må være satt")
    @param:Schema(description = "Bidragsmottakers fulle navn", required = true)
    override val fulltNavn: String,

    @param:NotEmpty(message = "Ident/fødselsnummer må være satt")
    @param:Schema(description = "Bidragsmottakers fødselsnummer", required = true)
    override val fodselsnummer: String,

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
    @param:Schema(description = "Hvordan dokumentasjonen knyttet til avtalen håndteres")
    val tilknyttetAvtale: TilknyttetAvtaleVedlegg,
    @field:NotNull
    @param:Schema(description = "Angir om annen dokumentasjon legges ved")
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
    @param:Schema(description = "Valgte språk", required = true)
    val språk: Språkkode,
    @param:Schema(description = "", required = true)
    val innhold: String,
    @param:Schema(description = "Informasjon om bidragsmottaker", required = true)
    val bidragsmottaker: PrivatAvtaleBidragsmottaker,
    @param:Schema(description = "Informasjon om bidragspliktig", required = true)
    val bidragspliktig: PrivatAvtaleBidragspliktig,
    @param:Schema(description = "Informasjon om barnet", required = true)
    val barn: List<PrivatAvtaleBarn>,
    @param:Schema(description = "Er dette en ny avtale?", required = true)
    val nyAvtale: Boolean,
    @param:Schema(description = "Oppgjørsform for betaling av bidraget", required = true)
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
