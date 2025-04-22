package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.BrukerInfomasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.BrukerInformasjonMapper
import org.springframework.stereotype.Service

@Service
class PersonService(private val personConsumer: BidragPersonConsumer) {

    fun hentInformasjon(personIdent: String): BrukerInfomasjonDto {
        val familierelasjon = personConsumer.hentFamilierelasjon(personIdent)

        return BrukerInformasjonMapper.tilBrukerInformasjonDto(familierelasjon)
    }
}