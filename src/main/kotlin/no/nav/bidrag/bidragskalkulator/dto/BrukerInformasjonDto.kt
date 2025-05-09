package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

@Schema(description = "Inneholder informasjon om en person")
data class PersonInformasjonDto(
    @Schema(description = "Unik identifikator for barnet (fødselsnummer eller D-nummer)", example = "12345678901")
    val ident: Personident,

    @Schema(description = "Fornavn til person", example = "Ola")
    val fornavn: String,

    @Schema(description = "Fullt navn til person", example = "Ola Nordmann")
    val fulltNavn: String,

    @Schema(description = "Alder til person", example = "12")
    val alder: Int
)

@Schema(description = "Representerer en foreldre-barn-relasjon, med felles barn og motpart")
data class BarneRelasjonDto(
    @Schema(description = "Motparten i relasjonen, vanligvis den andre forelderen.")
    val motpart: PersonInformasjonDto?,

    @Schema(description = "Liste over felles barn mellom pålogget person og motparten")
    val fellesBarn: List<PersonInformasjonDto>
)

@Schema(description = "Informasjon om pålogget person og relasjoner til barn")
data class BrukerInformasjonDto(
    @Schema(description = "Personinformasjon for pålogget bruker")
    val person: PersonInformasjonDto,

    @Schema(description = "Summert inntekt for pålogget bruker", example = "500000.0")
    var inntekt: BigDecimal? = null,

    @Schema(description = "Liste over barn til pålogget person, gruppert med motpart")
    val barnerelasjoner: List<BarneRelasjonDto>,
)
