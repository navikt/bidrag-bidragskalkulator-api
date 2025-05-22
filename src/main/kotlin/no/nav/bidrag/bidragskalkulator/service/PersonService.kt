package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.mapper.erDød
import no.nav.bidrag.bidragskalkulator.mapper.erLevendeOgIkkeSkjermet
import no.nav.bidrag.bidragskalkulator.mapper.harFortroligAdresse
import no.nav.bidrag.bidragskalkulator.model.FamilieRelasjon
import no.nav.bidrag.bidragskalkulator.model.ForelderBarnRelasjon
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.MotpartBarnRelasjon
import no.nav.bidrag.transport.person.PersonDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PersonService(
    @Qualifier("bidragPersonConsumer") private val personConsumer: BidragPersonConsumer,
    ) {
    val logger = LoggerFactory.getLogger(PersonService::class.java)

    fun hentPersoninformasjon(personIdent: Personident): PersonDto = personConsumer.hentPerson(personIdent)

    fun hentGyldigFamilierelasjon(personIdent: String): ForelderBarnRelasjon {
        val familierelasjon = personConsumer.hentFamilierelasjon(personIdent)
        val gyldigeRelasjoner = familierelasjon.personensMotpartBarnRelasjon.filtrerRelasjonerMedGyldigMotpartOgBarn()

        return ForelderBarnRelasjon (
            person = familierelasjon.person,
            motpartsrelasjoner = gyldigeRelasjoner
        )

    }

    private fun List<MotpartBarnRelasjon>.filtrerRelasjonerMedGyldigMotpartOgBarn(): List<FamilieRelasjon> =
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
}