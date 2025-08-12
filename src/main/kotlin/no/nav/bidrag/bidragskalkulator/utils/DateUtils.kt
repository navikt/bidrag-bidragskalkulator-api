package no.nav.bidrag.bidragskalkulator.utils


import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val secureLogger = LoggerFactory.getLogger("secureLogger")

fun kalkulerAlder(fødselsdato: LocalDate): Int {
    return try {
        Period.between(fødselsdato, LocalDate.now()).years
    } catch (e: DateTimeParseException) {
        secureLogger.warn("Feil ved kalkulering av alder for fødselsdato $fødselsdato", e)
        0
    }
}

fun String.tilNorskDatoFormat(): String {
    return try {
        // Forutsatt at inndataene er i formatet åååå-MM-dd eller dd.MM.åååå
        val date = when {
            this.contains("-") -> LocalDate.parse(this)
            this.contains(".") -> LocalDate.parse(this, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            else -> throw IllegalArgumentException("Ugyldig datoformat. Dato må være på format 'dd.MM.yyyy' eller 'yyyy-MM-dd'. Mottok: '$this'")
        }

        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    } catch (e: DateTimeParseException) {
        throw IllegalArgumentException(
            "Kunne ikke konvertere '$this' til gyldig dato. Dato må være på format 'dd.MM.yyyy' eller 'yyyy-MM-dd",
            e
        )
    }
}
