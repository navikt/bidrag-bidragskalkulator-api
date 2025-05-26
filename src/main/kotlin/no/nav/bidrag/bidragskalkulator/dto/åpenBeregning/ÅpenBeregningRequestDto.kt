package no.nav.bidrag.bidragskalkulator.dto.åpenBeregning

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.bidragskalkulator.dto.BarnDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BidragsType
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Schema(description = "Informasjon om et barn i beregningen")
data class BarnForÅpenBeregningDto(
    @field:NotNull(message = "Alder må være satt")
    @field:Min(value = 0, message = "Alder kan ikke være negativ")
    @field:Max(value = 25, message = "Alder kan ikke være høyere enn 25")
    @Schema(description = "Alder til barnet", required = true, example = "10")
    val alder: Int,

    @field:NotNull(message = "Samværsklasse må være satt")
    @Schema(ref = "#/components/schemas/Samværsklasse") // Reference dynamically registered schema. See BeregnBarnebidragConfig
    val samværsklasse: Samværsklasse,

    @field:NotNull(message = "Bidragstype må være satt")
    @Schema(description = "Angir om den påloggede personen er pliktig eller mottaker for dette barnet", required = true)
    val bidragstype: BidragsType
){
    @JsonIgnore
    @Schema(hidden = true) // Hides from Swagger
    //Når barnet har alder = 15, blir fødselsmåneden alltid satt til juli, uavhengig av den faktiske fødselsdatoen (usikkert hvor denne regelen stammer fra).
    // Dette betyr at barnet ikke anses som 15 år før juli.
    // I alle beregningsperioder før juli vil barnet derfor fortsatt regnes som 14 år.
    fun getEstimertFødselsdato(): LocalDate = LocalDate.now().minusYears(alder.toLong())

    @JsonIgnore
    @Schema(hidden = true)
    fun getMockIdent(): Personident = Personident(genererMockFnr(getEstimertFødselsdato()))
}

data class ÅpenBeregningRequestDto (
    @field:NotNull(message = "Inntekt for forelder 1 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 1 kan ikke være negativ")
    @Schema(description = "Inntekt for forelder 1 i norske kroner", required = true, example = "500000.0")
    val inntektForelder1: Double,

    @field:NotNull(message = "Inntekt for forelder 2 må være satt")
    @field:Min(value = 0, message = "Inntekt for forelder 2 kan ikke være negativ")
    @Schema(description = "Inntekt for forelder 2 i norske kroner", required = true, example = "450000.0")
    val inntektForelder2: Double,

    @field:NotEmpty(message = "Liste over barn kan ikke være tom")
    @field:Valid
    @Schema(description = "Liste over barn som inngår i beregningen", required = true)
    val barn: List<BarnForÅpenBeregningDto>,
)

fun BarnForÅpenBeregningDto.tilBarnDto(): BarnDto =
    BarnDto(ident = this.getMockIdent(), samværsklasse = this.samværsklasse, bidragstype = this.bidragstype)

fun ÅpenBeregningRequestDto.tilBeregningRequestDto(): BeregningRequestDto =
    BeregningRequestDto(
        inntektForelder1 = this.inntektForelder1,
        inntektForelder2 = this.inntektForelder2,
        barn = this.barn.map { it.tilBarnDto() }
    )

fun genererMockFnr(fodselsdato: LocalDate): String {
    // Format: DDMMYY
    val datoDel = fodselsdato.format(DateTimeFormatter.ofPattern("ddMMyy"))

    // Lag et tilfeldig individnummer (3 siffer)
    val individnummer = (100..999).random().toString()

    // Kombiner dato + individnummer (9 siffer)
    val niSiffer = datoDel + individnummer

    // Kalkuler kontrollsifre (mod11)
    val k1 = kalkulerKontrollsiffer(niSiffer, listOf(3, 7, 6, 1, 8, 9, 4, 5, 2))
    val k2 = kalkulerKontrollsiffer(niSiffer + k1, listOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2))

    return if (k1 == null || k2 == null) {
        throw IllegalArgumentException("Klarte ikke å generere gyldige kontrollsifre")
    } else {
        niSiffer + k1 + k2
    }
}

private fun kalkulerKontrollsiffer(sifre: String, vekter: List<Int>): Int? {
    val sum = sifre.mapIndexed { index, c -> Character.getNumericValue(c) * vekter[index] }.sum()
    val rest = sum % 11
    return when (val kontrollsiffer = 11 - rest) {
        11 -> 0
        10 -> null // ugyldig – må forkastes
        else -> kontrollsiffer
    }
}