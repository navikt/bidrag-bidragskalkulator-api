package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

@Schema(description = "Inneholder informasjon om en person")
data class PersonInformasjonDto(
    @param:Schema(description = "Unik identifikator for person (fødselsnummer eller D-nummer)")
    val ident: Personident,

    @param:Schema(description = "Fornavn til person", example = "Ola")
    val fornavn: String,

    @param:Schema(description = "Fullt navn til person", example = "Ola Nordmann")
    val fulltNavn: String,

    @param:Schema(description = "Alder til person", example = "12")
    val alder: Int
)

@Schema(description = "Inneholder informasjon om et barn")
data class BarnInformasjonDto(
    @param:Schema(description = "Unik identifikator for barnet (fødselsnummer eller D-nummer)")
    val ident: Personident,

    @param:Schema(description = "Fornavn til barnet", example = "Ola")
    val fornavn: String,

    @param:Schema(description = "Fullt navn til barnet", example = "Ola Nordmann")
    val fulltNavn: String,

    @param:Schema(description = "Alder til barnet", example = "12")
    val alder: Int,

    @param:Schema(
        description = "Underholdskostnad til barnet, gruppert etter aldersintervall (0-5, 6-10, 11-14, 15+)",
        example = "4738"
    )
    val underholdskostnad: BigDecimal,
)

@Schema(description = "Representerer en foreldre-barn-relasjon, med felles barn og motpart")
data class BarneRelasjonDto(
    @param:Schema(description = "Motparten i relasjonen, vanligvis den andre forelderen.")
    val motpart: PersonInformasjonDto,

    @param:Schema(description = "Liste over felles barn mellom pålogget person og motparten")
    val fellesBarn: List<BarnInformasjonDto>
)

@Schema(description = "Informasjon om pålogget person og relasjoner til barn")
data class BrukerInformasjonDto(
    @param:Schema(description = "Personinformasjon for pålogget bruker")
    val person: PersonInformasjonDto,

    @param:Schema(description = "Summert inntekt for pålogget bruker", example = "500000.0")
    var inntekt: BigDecimal? = null,

    @param:Schema(description = "Liste over barn til pålogget person, gruppert med motpart")
    val barnerelasjoner: List<BarneRelasjonDto>,

    @param:Schema(description = "Liste over underholdskostnader for alle aldre fra 0-25 år")
    val underholdskostnader: Map<Int, BigDecimal>,

    val samværsfradrag: List<SamværsfradragPeriode>
)


@Schema(description = "Samværsfradrag for et gitt aldersintervall")
data class SamværsfradragPeriode(

    @param:Schema(description = "Start på aldersintervall")
    val alderFra: Int,

    @param:Schema(description = "Slutt på aldersintervall")
    val alderTil: Int,

    @param:Schema(description = "Beløp per samværsfradragsklasse for dette aldersintervallet. For eksempel SAMVÆRSKLASSE_1, SAMVÆRSKLASSE_2 osv.")
    val beløpFradrag: Map<String, BigDecimal>
)
