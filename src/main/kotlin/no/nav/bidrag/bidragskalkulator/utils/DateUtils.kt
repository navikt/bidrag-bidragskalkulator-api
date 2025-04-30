package no.nav.bidrag.bidragskalkulator.utils


import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import org.slf4j.LoggerFactory

private val secureLogger = LoggerFactory.getLogger("secureLogger")

fun kalkulereAlder(fødselsdato: LocalDate): Int {
    return try {
        Period.between(fødselsdato, LocalDate.now()).years
    } catch (e: DateTimeParseException) {
        secureLogger.warn("Feil ved kalkulering av alder for fødselsdato $fødselsdato", e)
        0
    }
}