package no.nav.bidrag.bidragskalkulator.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident

@Schema(description = "Inneholder informasjon om en person")
data class PersonDto(
    @Schema(description = "Unik identifikator for personen (fødselsnummer eller D-nummer)", example = "12345678901")
    val ident: Personident,

    @Schema(description = "Fornavn til personen", example = "Ola")
    val fornavn: String,

    @Schema(description = "Visningsnavn, som er fornavn og etternavn kombinert", example = "Ola Nordmann")
    val visningsnavn: String,

    @Schema(description = "Alder til personen", example = "12",)
    val alder: Int
)

@Schema(description = "Representerer en foreldre-barn-relasjon, med felles barn og motpart")
data class BarneRelasjonDto(
    // I enkelte tilfeller er ikke motpart registrert, og feltet kan derfor være null.
    @Schema(description = "Motparten i relasjonen, vanligvis den andre forelderen")
    val motpart: PersonDto?,

    @Schema(description = "Liste over felles barn mellom pålogget person og motparten")
    val fellesBarn: List<PersonDto>
)

@Schema(description = "Informasjon om pålogget person og relasjoner til barn")
data class BrukerInfomasjonDto(
    @Schema(description = "Informasjon om pålogget person")
    val paaloggetPerson: PersonDto,

    @Schema(description = "Liste over barn til pålogget person, gruppert med motpart")
    val barnRelasjon: List<BarneRelasjonDto>
)