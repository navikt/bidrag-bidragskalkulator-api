package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.mapper.tilFamilieRelasjon
import no.nav.bidrag.bidragskalkulator.model.ForelderBarnRelasjon
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PersonService(
    @Qualifier("bidragPersonConsumer") private val personConsumer: BidragPersonConsumer,
    ) {
    val logger = LoggerFactory.getLogger(PersonService::class.java)

    fun hentPersoninformasjon(personIdent: Personident): PersonDto = personConsumer.hentPerson(personIdent)

    fun hentGyldigFamilierelasjon(personIdent: String): ForelderBarnRelasjon {
        logger.info("Hent gyldig familierelasjoner")
        val familierelasjon = personConsumer.hentFamilierelasjon(personIdent)
        val gyldigeRelasjoner = familierelasjon.personensMotpartBarnRelasjon.tilFamilieRelasjon()

        return ForelderBarnRelasjon (
            person = familierelasjon.person,
            motpartsrelasjoner = gyldigeRelasjoner
        ).also { logger.info("Ferdig med henting gyldig familierelasjoner") }

    }
}