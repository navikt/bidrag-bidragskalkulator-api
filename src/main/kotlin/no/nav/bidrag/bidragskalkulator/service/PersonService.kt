package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.BrukerInfomasjonDto
import no.nav.bidrag.bidragskalkulator.exception.NoContentException
import no.nav.bidrag.bidragskalkulator.mapper.BrukerInformasjonMapper.tilBrukerInformasjonDto
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.stereotype.Service

@Service
class PersonService(private val personConsumer: BidragPersonConsumer) {

    fun hentInformasjon(personIdent: String): BrukerInfomasjonDto {
        val familierelasjon = personConsumer.hentFamilierelasjon(personIdent)

        if (familierelasjon == null) {
            secureLogger.warn { "Fant ikke person med ident $personIdent" }
            throw NoContentException("Fant ikke person med ident $personIdent")
        }

        return tilBrukerInformasjonDto(familierelasjon)
    }
}