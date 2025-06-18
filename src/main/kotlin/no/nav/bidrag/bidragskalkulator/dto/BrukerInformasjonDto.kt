package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

@Schema(description = "Inneholder informasjon om en person")
data class PersonInformasjonDto(
    @Schema(description = "Unik identifikator for person (fødselsnummer eller D-nummer)", example = "12345678901")
    val ident: Personident,

    @Schema(description = "Fornavn til person", example = "Ola")
    val fornavn: String,

    @Schema(description = "Fullt navn til person", example = "Ola Nordmann")
    val fulltNavn: String,

    @Schema(description = "Alder til person", example = "12")
    val alder: Int
)

@Schema(description = "Inneholder informasjon om et barn")
data class BarnInformasjonDto(
    @Schema(description = "Unik identifikator for barnet (fødselsnummer eller D-nummer)", example = "12345678901")
    val ident: Personident,

    @Schema(description = "Fornavn til barnet", example = "Ola")
    val fornavn: String,

    @Schema(description = "Fullt navn til barnet", example = "Ola Nordmann")
    val fulltNavn: String,

    @Schema(description = "Alder til barnet", example = "12")
    val alder: Int,

    @Schema(description = "Underholdskostnad til barnet, gruppert etter aldersintervall (0-5, 6-10, 11-14, 15+)", example = "4738")
    val underholdskostnad: BigDecimal,
)

@Schema(description = "Representerer en foreldre-barn-relasjon, med felles barn og motpart")
data class BarneRelasjonDto(
    @Schema(description = "Motparten i relasjonen, vanligvis den andre forelderen.")
    val motpart: PersonInformasjonDto,

    @Schema(description = "Liste over felles barn mellom pålogget person og motparten")
    val fellesBarn: List<BarnInformasjonDto>
)

@Schema(description = "Informasjon om pålogget person og relasjoner til barn")
data class BrukerInformasjonDto(
    @Schema(description = "Personinformasjon for pålogget bruker")
    val person: PersonInformasjonDto,

    @Schema(description = "Summert inntekt for pålogget bruker", example = "500000.0")
    var inntekt: BigDecimal? = null,

    @Schema(description = "Liste over barn til pålogget person, gruppert med motpart")
    val barnerelasjoner: List<BarneRelasjonDto>,

    @Schema(description = "Liste over underholdskostnader for alle aldre fra 0-25 år")
    val underholdskostnader: Map<Int, BigDecimal>,

    val samværsfradrag: List<SamværsfradragPeriode>
)


@Schema(description = "Samværsfradrag for et gitt aldersintervall")
data class SamværsfradragPeriode(

    @Schema(description = "Start på aldersintervall")
    val alderFom: Int,

    @Schema(description = "Slutt på aldersintervall")
    val alderTom: Int,

    @Schema(description = "Beløp per samværsfradragsklasse for dette aldersintervallet. For eksempel SAMVÆRSKLASSE_1, SAMVÆRSKLASSE_2 osv.")
    val beløpFradrag: Map<String, BigDecimal>
)
