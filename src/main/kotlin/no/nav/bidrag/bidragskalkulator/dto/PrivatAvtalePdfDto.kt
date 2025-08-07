package no.nav.bidrag.bidragskalkulator.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import no.nav.bidrag.bidragskalkulator.utils.tilNorskDatoFormat

interface PrivatAvtalePerson {
    val fulltNavn: String
    val fodselsnummer: String
}

data class PrivatAvtaleBidragsmottaker(
    @field:NotEmpty(message = "fulltNavn må være satt")
    @Schema(description = "Bidragsmottakers fulle navn", required = true)
    override val fulltNavn: String,

    @field:NotEmpty(message = "Ident/fødselsnummer må være satt")
    @Schema(description = "Bidragsmottakers fødselsnummer", required = true)
    override val fodselsnummer: String,
) : PrivatAvtalePerson

data class PrivatAvtaleBidragspliktig(
    @field:NotEmpty(message = "fulltNavn må være satt")
    @Schema(description = "Bidragsmottakers fulle navn", required = true)
    override val fulltNavn: String,

    @field:NotEmpty(message = "Ident/fødselsnummer må være satt")
    @Schema(description = "Bidragsmottakers fødselsnummer", required = true)
    override val fodselsnummer: String,
) : PrivatAvtalePerson

@Schema(description = "Informasjon om barnet i en privat avtale")
data class PrivatAvtaleBarn(
    @field:NotEmpty(message = "fulltNavn må være satt")
    @Schema(description = "Bidragsmottakers fulle navn", required = true)
    override val fulltNavn: String,

    @field:NotEmpty(message = "Ident/fødselsnummer må være satt")
    @Schema(description = "Bidragsmottakers fødselsnummer", required = true)
    override val fodselsnummer: String,

    @field:Min(value = 1, message = "Bidraget må være større enn 0")
    @Schema(description = "Barnets fødselsnummer", required = true)
    val sumBidrag: Double  // Beløp in NOK
) : PrivatAvtalePerson

// TODO: Definer oppgjørsformer som en enum (?)
enum class Oppgjorsform {
    // Add your enum values here
}

@Schema(description = "Informasjon for generering av en privat avtale PDF")
data class PrivatAvtalePdfDto(
    @Schema(description = "", required = true)
    val innhold: String,
    @Schema(description = "Informasjon om bidragsmottaker", required = true)
    val bidragsmottaker: PrivatAvtaleBidragsmottaker,
    @Schema(description = "Informasjon om bidragspliktig", required = true)
    val bidragspliktig: PrivatAvtaleBidragspliktig,
    @Schema(description = "Informasjon om barnet", required = true)
    val barn: List<PrivatAvtaleBarn>,
    @Schema(description = "Er dette en ny avtale?", required = true)
    val nyAvtale: Boolean,
    @Schema(description = "Oppgjørsform for betaling av bidraget", required = true)
    val oppgjorsform: String,  // Consider using the Oppgjorsform enum instead of String
    @Schema(description = "Er dette en avtale som skal sendes til NAV?", required = true)
    val tilInnsending: Boolean = false,
    @Schema(description = "Gjeldende dato for avtalen")
    val fraDato: String,
)  {
    @JsonIgnore
    @Schema(hidden = true)
    fun tilNorskDatoFormat(): PrivatAvtalePdfDto =
        this.copy(fraDato = fraDato.tilNorskDatoFormat())
}
