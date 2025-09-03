package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.mapper.tilFamilieRelasjon
import no.nav.bidrag.bidragskalkulator.model.ForelderBarnRelasjon
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PersonService(
    private val personConsumer: BidragPersonConsumer,
    ) {

    fun hentPersoninformasjon(personIdent: Personident): PersonDto = personConsumer.hentPerson(personIdent)

    fun hentGyldigFamilierelasjon(personIdent: String): ForelderBarnRelasjon {
        logger.info { "Hent gyldig familierelasjoner" }
        val familierelasjon = personConsumer.hentFamilierelasjon(personIdent)
        val gyldigeRelasjoner = familierelasjon.personensMotpartBarnRelasjon.tilFamilieRelasjon()

        return ForelderBarnRelasjon (
            person = familierelasjon.person,
            motpartsrelasjoner = gyldigeRelasjoner
        ).also { logger.info { "Ferdig med henting gyldig familierelasjoner" } }

    }
}
