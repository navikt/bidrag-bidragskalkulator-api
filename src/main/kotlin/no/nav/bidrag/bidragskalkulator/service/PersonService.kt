package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PersonService(
    @Qualifier("bidragPersonConsumer") private val personConsumer: BidragPersonConsumer,
    ) {

    fun hentPersoninformasjon(personIdent: Personident): PersonDto = personConsumer.hentPerson(personIdent)

}

