package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident

@Schema(description = "Inneholder informasjon om pålogget bruker")
data class PåloggetPersonDto(
    @Schema(description = "Unik identifikator for personen (fødselsnummer eller D-nummer)", example = "12345678901")
    val ident: Personident,

    @Schema(description = "Fornavn til personen", example = "Ola")
    val fornavn: String,

    @Schema(description = "Fullt navn, som er fornavn og etternavn kombinert", example = "Ola Nordmann")
    val fulltNavn: String,
)

@Schema(description = "Inneholder informasjon om en person")
data class PersonInformasjonDto(
    @Schema(description = "Unik identifikator for barnet (fødselsnummer eller D-nummer)", example = "12345678901")
    val ident: Personident,

    @Schema(description = "Fornavn til barnet", example = "Ola")
    val fornavn: String,

    @Schema(description = "Fullt navn til barnet", example = "Ola Nordmann")
    val fulltNavn: String,

    @Schema(description = "Alder til barnet", example = "12")
    val alder: Int
)

@Schema(description = "Representerer en foreldre-barn-relasjon, med felles barn og motpart")
data class BarneRelasjonDto(
    // I enkelte tilfeller er ikke motpart registrert, og feltet kan derfor være null.
    @Schema(description = "Motparten i relasjonen, vanligvis den andre forelderen. I enkelte tilfeller er ikke motpart registrert, og feltet kan derfor være null.")
    val motpart: PersonInformasjonDto?,

    @Schema(description = "Liste over felles barn mellom pålogget person og motparten")
    val fellesBarn: List<PersonInformasjonDto>
)

@Schema(description = "Informasjon om pålogget person og relasjoner til barn")
data class BrukerInfomasjonDto(
    @Schema(description = "Informasjon om pålogget person")
    val påloggetPerson: PåloggetPersonDto,

    @Schema(description = "Liste over barn til pålogget person, gruppert med motpart")
    val barnRelasjon: List<BarneRelasjonDto>
)