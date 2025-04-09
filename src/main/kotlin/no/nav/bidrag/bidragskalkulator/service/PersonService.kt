package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.springframework.stereotype.Service

@Service
class PersonService(private val personConsumer: BidragPersonConsumer) {

    fun hentFamilierelasjon(personIdent: String): MotpartBarnRelasjonDto? {
        val familierelasjon = personConsumer.hentFamilierelasjon(personIdent)

        if (familierelasjon == null) {
            secureLogger.info { "Fant ikke person med ident $personIdent" }
            throw NoContentException("Fant ikke person med ident $personIdent")
        }

        return familierelasjon

    }
}