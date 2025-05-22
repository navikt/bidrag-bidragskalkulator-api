package no.nav.bidrag.bidragskalkulator.mapper

import no.nav.bidrag.bidragskalkulator.model.FamilieRelasjon
import no.nav.bidrag.transport.person.MotpartBarnRelasjon
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("MotpartBarnRelasjonMapper")

fun List<MotpartBarnRelasjon>.filtrerRelasjonerMedGyldigMotpartOgBarn(): List<FamilieRelasjon> =
    this.asSequence()
        .mapNotNull { relasjon ->
            val motpart = relasjon.motpart
            if(motpart == null) {
                logger.info("Fjerner relasjon hvor motpart == null")
                // Hopp over denne relasjonen og ikke inkluder dette elementet i resultatet. Fortsett med resten
                return@mapNotNull null
            }

            if (motpart.erDød() || motpart.harFortroligAdresse()) return@mapNotNull null
            if(relasjon.fellesBarn.isEmpty()) return@mapNotNull null

            val filtrerteBarn = relasjon.fellesBarn.filter { it.erLevendeOgIkkeSkjermet() }
            if (filtrerteBarn.isEmpty()) return@mapNotNull null
            FamilieRelasjon(motpart, filtrerteBarn)
        }
        .toList()

fun List<MotpartBarnRelasjon>.tilFamilieRelasjon(): List<FamilieRelasjon> = this.filtrerRelasjonerMedGyldigMotpartOgBarn()