package no.nav.bidrag.bidragskalkulator.model

import no.nav.bidrag.transport.person.PersonDto

data class ForelderBarnRelasjon(
    val person: PersonDto,
    val motpartsrelasjoner: List<FamilieRelasjon> = emptyList(),
)

data class FamilieRelasjon(
    val motpart: PersonDto,
    val fellesBarn: List<PersonDto> = emptyList(),
)