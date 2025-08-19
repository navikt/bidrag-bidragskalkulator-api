package no.nav.bidrag.bidragskalkulator.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
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

interface PrivatAvtalePdf {
    val språk: Språkkode
    val bidragsmottaker: PrivatAvtalePart
    val bidragspliktig: PrivatAvtalePart
    val oppgjør: Oppgjør
    val vedlegg: Vedleggskrav
    val andreBestemmelser: AndreBestemmelserSkjema
}

@Schema(description = "Representerer informasjon om part i en privat avtale")
data class PrivatAvtalePart(
    @param:NotEmpty(message = FEILMELDING_FORNAVN)
    @param:Schema(
        description = "Partens fornavn fra folkeregisteret",
        required = true,
        example = "Ola"
    )
    override val fornavn: String,

    @param:NotEmpty(message = FEILMELDING_ETTERNAVN)
    @param:Schema(
        description = "Partens etternavn fra folkeregisteret",
        required = true,
        example = "Nordmann"
    )
    override val etternavn: String,

    @param:NotEmpty(message = FEILMELDING_FODSELSNUMMER)
    @param:Schema(
        description = "Partens personnummer (11 siffer)",
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

@Schema(
    description = "Hvis dette er en endring av en eksisterende avtale, må `oppgjorsformIdag` angi nåværende oppgjørsform. " +
            "Dersom oppgjørsformen endres i den nye avtalen, skal en kopi av avtalen sendes til Nav. " +
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

@Schema(description = "Informasjon om bidrag som skal betales i en privat avtale")
data class Bidrag(
    @param:Min(value = 1, message = "Beløpet må være større enn 0")
    @param:Schema(description = "Bidragsbeløp per måned i NOK", required = true, example = "1000.0")
    val bidragPerMåned: Double,

    @param:Schema(description = "Bidraget skal betales fra og med", required = true, example = "01-2025")
    val fraDato: String,

    @param:Schema(description = "Bidraget skal betales til og med", required = true, example = "12-2025")
    val tilDato: String
)

@Schema(description = "Informasjon for generering av en privat avtale PDF")
data class PrivatAvtalePdfDto(
    @param:Schema(ref = "#/components/schemas/Språkkode", required = true)
    override val språk: Språkkode,

    @param:Schema(description = "Informasjon om bidragsmottaker", required = true)
    override val bidragsmottaker: PrivatAvtalePart,

    @param:Schema(description = "Informasjon om bidragspliktig", required = true)
    override val bidragspliktig: PrivatAvtalePart,

    @param:Schema(description = "Informasjon om barnet", required = true)
    val barn: List<PrivatAvtaleBarn>,

    @param:Schema(description = "Opplysninger om oppgjør av barnebidrag, inkludert om avtalen er ny eller en endring, " +
            "nåværende oppgjørsform og ønsket oppgjørsform.", required = true)
    @field:NotNull
    override val oppgjør: Oppgjør,

    @field:Valid
    @field:NotNull
    @param:Schema(ref = "#/components/schemas/Vedleggskrav")
    override val vedlegg: Vedleggskrav,

    @field:NotNull
    @field:Valid
    @param:Schema(description = "Eventuelle andre bestemmelser som er inkludert i avtalen")
    override val andreBestemmelser: AndreBestemmelserSkjema
): PrivatAvtalePdf  {
    @JsonIgnore
    @Schema(hidden = true)
    fun medNorskeDatoer(): PrivatAvtalePdfDto = this.copy(
        barn = this.barn.map { it.copy(fraDato = it.fraDato.tilNorskDatoFormat()) },
    )
}

@Schema(description = "Informasjon for generering av en privat avtale PDF for barn over 18 år")
data class PrivatAvtaleBarnOver18RequestDto (
    @param:Schema(ref = "#/components/schemas/Språkkode", required = true)
    override val språk: Språkkode,

    @param:Schema(description = "Informasjon om bidragsmottaker (barn over 18 år)", required = true)
    override val bidragsmottaker: PrivatAvtalePart,

    @param:Schema(description = "Informasjon om bidragspliktig", required = true)
    override val bidragspliktig: PrivatAvtalePart,

    @param:Schema(description = "Informasjon om bidrag som skal betales i en privat avtale", required = true)
    val bidrag: List<Bidrag>,

    @param:Schema(description = "Opplysninger om oppgjør av barnebidrag, inkludert om avtalen er ny eller en endring, " +
            "nåværende oppgjørsform og ønsket oppgjørsform.", required = true)
    @field:NotNull
    override val oppgjør: Oppgjør,

    @field:Valid
    @field:NotNull
    @param:Schema(ref = "#/components/schemas/Vedleggskrav")
    override val vedlegg: Vedleggskrav,

    @field:NotNull
    @field:Valid
    @param:Schema(description = "Eventuelle andre bestemmelser som er inkludert i avtalen")
    override val andreBestemmelser: AndreBestemmelserSkjema
): PrivatAvtalePdf


/**
 * Sjekker om førsteside skal genereres basert på oppgjørsform og avtaletype.
 * - For ny avtale: Generer kun når ønsket oppgjørsform er INNKREVING.
 * - For eksisterende avtale: Generer for alle unntatt PRIVAT -> PRIVAT.
 */
fun Oppgjør.skalFoerstesideGenereres(): Boolean {
    if (nyAvtale) {
        // Ny avtale: generer kun når ønsket er INNKREVING
        return oppgjørsformØnsket == Oppgjørsform.INNKREVING
    } else {
        // Eksisterende avtale: generer for alle unntatt PRIVAT -> PRIVAT
        val idag = requireNotNull(oppgjørsformIdag) { "oppgjørsformIdag må settes når nyAvtale=false" }

        val privatOppgjørPåNyOgGammelAvtale = idag == Oppgjørsform.PRIVAT && oppgjørsformØnsket == Oppgjørsform.PRIVAT;


        return !privatOppgjørPåNyOgGammelAvtale
    }
}
