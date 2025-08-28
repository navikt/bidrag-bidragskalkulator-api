package no.nav.bidrag.bidragskalkulator.utils

import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

private val logger = LoggerFactory.getLogger("DateUtils")

fun kalkulerAlder(fødselsdato: LocalDate): Int {
    return try {
        Period.between(fødselsdato, LocalDate.now()).years
    } catch (e: DateTimeParseException) {
        logger.warn("Feil ved kalkulering av alder for fødselsdato $fødselsdato", e)
        0
    }
}
