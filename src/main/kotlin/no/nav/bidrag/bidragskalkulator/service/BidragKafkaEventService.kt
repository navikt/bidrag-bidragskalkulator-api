package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.sak.Sakshendelse
import org.springframework.stereotype.Service

@Service
class BidragKafkaEventService() {

    fun prosesserSakshendelse(saksHendelse: Sakshendelse) {
        secureLogger.info { "Prosesseserer sakshendelse for saksnummer=${saksHendelse.saksnummer} og sporingId=${saksHendelse.sporingId}" }
    }

}