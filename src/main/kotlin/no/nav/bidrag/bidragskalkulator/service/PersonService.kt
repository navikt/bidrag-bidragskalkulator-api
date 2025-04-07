package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import org.springframework.stereotype.Service

@Service
class PersonService(private val personConsumer: BidragPersonConsumer) {

    fun hentFamilierelasjon(): MotpartBarnRelasjonDto? {
        val personIdent: String = TokenUtils.hentBruker()
            ?: throw IllegalArgumentException("Brukerident er ikke tilgjengelig i token")

        return runCatching {
            personConsumer.hentFamilierelasjon(personIdent)
        }.onFailure { e ->
            secureLogger.error(e) { "Feil ved henting av familierelasjon for ident $personIdent" }
        }.getOrNull()
    }
}