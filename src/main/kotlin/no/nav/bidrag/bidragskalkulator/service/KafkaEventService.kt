package no.nav.bidrag.bidragskalkulator.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.sak.Sakshendelse
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class KafkaEventService() {

    fun prosesserSakshendelse(saksHendelse: Sakshendelse) {
        secureLogger.info { "Prosesseserer sakshendelse for saksnummer=${saksHendelse.saksnummer} og sporingId=${saksHendelse.sporingId}" }
    }

}