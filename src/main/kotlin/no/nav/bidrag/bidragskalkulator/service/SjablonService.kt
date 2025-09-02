package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.bidragskalkulator.config.CacheConfig
import no.nav.bidrag.bidragskalkulator.dto.SamværsfradragPeriode
import no.nav.bidrag.commons.service.sjablon.Samværsfradrag
import no.nav.bidrag.commons.service.sjablon.SjablonProvider
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

@Service
class SjablonService {

    /**
     * Henter samværsfradrag fra sjablon, filtrerer på dato og grupperer etter alderTom.
     * Se punktet om Samværsfradrag i https://lovdata.no/nav/rundskriv/v1-55-02
     * Returnerer en liste av SamværsfradragPeriode med alderFom, alderTom og beløpFradrag.
     */
    @Cacheable(CacheConfig.SAMVÆRSFRADRAG)
    fun hentSamværsfradrag(): List<SamværsfradragPeriode> {
        logger.info { "Henter samværsfradrag" }
        val nåværendeDato = LocalDate.now()

        val (filtrert, varighet) = runCatching {
            measureTimedValue {
                val alle = SjablonProvider.hentSjablonSamværsfradrag()
                val ugyldige = alle.count { it.datoFom == null || it.datoTom == null }
                if (ugyldige > 0) {
                    logger.warn("Fant $ugyldige ugyldige elementer i sjablon for samværsfradrag")
                }

                alle.filter { it.datoFom != null && it.datoTom != null }
                    .filter { nåværendeDato >= it.datoFom && nåværendeDato <= it.datoTom }
            }
        }.onFailure { e ->
            logger.error{ "Kall til sjablon provider feilet" }
            secureLogger.error(e) { "Kall til sjablon provider feilet: ${e.message}" }
        }.getOrThrow()

        logger.info { "Kall til sjablon provider OK (varighet_ms=${varighet.inWholeMilliseconds})" }

        // 2) Gruppér på alderTom
        val alderTomGrupper = filtrert.groupBy { it.alderTom ?: 99 }

        // 3) Lag sortert liste av alderTom
        val sortertAlderTom = alderTomGrupper.keys.sorted()

        // 4) Map til SamværsfradragPeriode
        val resultat = mapTilSamværsfradragPeriode(sortertAlderTom, alderTomGrupper)

        logger.info {
            "Mapping til samværsfradrag-perioder fullført."
        }

        return resultat
    }

    /**
     * Mapper en sortert liste av alderTom og en map av alderTom til SamværsfradragPeriode.
     * AlderFom settes til 0 for første periode, og oppdateres etter hver iterasjon.
     */
    private fun mapTilSamværsfradragPeriode(
        sortedAlderTom: List<Int>,
        alderTomGrupper: Map<Int, List<Samværsfradrag>>
    ): List<SamværsfradragPeriode> {
        val resultat = mutableListOf<SamværsfradragPeriode>()
        var alderFom = 0

        for (alderTom in sortedAlderTom) {
            val fradragMap = mutableMapOf<String, BigDecimal>()
            val fradragElementer = alderTomGrupper[alderTom] ?: emptyList()

            fradragElementer.forEach { entry ->
                val klasseNummer = entry.samvaersklasse?.padStart(2, '0') ?: "00"
                val klasseNavn = "SAMVÆRSKLASSE_${klasseNummer.toInt()}"
                fradragMap[klasseNavn] = entry.belopFradrag ?: BigDecimal.ZERO
            }

            resultat.add(
                SamværsfradragPeriode(
                    alderFra = alderFom,
                    alderTil = alderTom,
                    beløpFradrag = fradragMap
                )
            )

            alderFom = alderTom + 1
        }

        return resultat
    }
}
