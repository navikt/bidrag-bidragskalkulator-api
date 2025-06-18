package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.dto.SamværsfradragPeriode
import no.nav.bidrag.commons.service.sjablon.SjablonProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class SjablonService {
    val logger = LoggerFactory.getLogger(SjablonService::class.java)

    /**
     * Henter samværsfradrag fra sjablon, filtrerer på dato og grupperer etter alderTom.
     * Se punktet om Samværsfradrag i https://lovdata.no/nav/rundskriv/v1-55-02
     * Returnerer en liste av SamværsfradragPeriode med alderFom, alderTom og beløpFradrag.
     */
    suspend fun hentSamværsfradrag(): List<SamværsfradragPeriode> {
        logger.info("Henter samværsfradrag")
        val nåværendeDato = LocalDate.now()

        // 1) Hent original liste
        val alle = SjablonProvider.hentSjablonSamværsfradrag()
            .filter { it.datoFom != null && it.datoTom != null }
            .filter { nåværendeDato >= it.datoFom && nåværendeDato <= it.datoTom }

        // 2) Gruppér på alderTom
        val alderTomGrupper = alle.groupBy { it.alderTom ?: 99 }

        // 3) Lag sortert liste av alderTom
        val sortedAlderTom = alderTomGrupper.keys.sorted()

        // 4) Bygg ny struktur
        val resultat = mutableListOf<SamværsfradragPeriode>()
        var alderFom = 0

        for (alderTom in sortedAlderTom) {
            val fradragMap = mutableMapOf<String, BigDecimal>()
            val items = alderTomGrupper[alderTom]!!

            items.forEach { entry ->
                val klasseNummer = entry.samvaersklasse?.padStart(2, '0') ?: "00"
                val klasseNavn = "SAMVÆRSKLASSE_${klasseNummer.toInt()}"
                fradragMap[klasseNavn] = entry.belopFradrag ?: BigDecimal.ZERO
            }

            resultat.add(
                SamværsfradragPeriode(
                    alderFom = alderFom,
                    alderTom = alderTom,
                    beløpFradrag = fradragMap
                )
            )

            alderFom = alderTom + 1
        }

        return resultat
    }
}
