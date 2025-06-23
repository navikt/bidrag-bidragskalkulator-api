package no.nav.bidrag.bidragskalkulator.utils

import com.github.benmanes.caffeine.cache.Expiry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

/**
 * Utløpspolicy for Caffeine som utløper på en fast dato hvert år.
 * Eksempel: 1. juli hvert år.
 */
class FastDatoPerÅrUtløp(
    private val måned: Month,
    private val dagIMåned: Int
) : Expiry<Any, Any> {

    override fun expireAfterCreate(nøkkel: Any, verdi: Any, nåværendeTid: Long): Long {
        val iDag = LocalDate.now()
        val nesteDato = if (iDag.isBefore(LocalDate.of(iDag.year, måned, dagIMåned))) {
            LocalDate.of(iDag.year, måned, dagIMåned)
        } else {
            LocalDate.of(iDag.year + 1, måned, dagIMåned)
        }

        val sekunderTilNesteDato = Duration.between(
            Instant.now(),
            nesteDato.atStartOfDay().toInstant(ZoneOffset.UTC)
        ).seconds

        return TimeUnit.SECONDS.toNanos(sekunderTilNesteDato)
    }

    override fun expireAfterUpdate(nøkkel: Any, verdi: Any, nåværendeTid: Long, nåværendeVarighet: Long): Long {
        // Oppdatering påvirker ikke utløpet
        return nåværendeVarighet
    }

    override fun expireAfterRead(nøkkel: Any, verdi: Any, nåværendeTid: Long, nåværendeVarighet: Long): Long {
        // Lesing påvirker ikke utløpet
        return nåværendeVarighet
    }
}
