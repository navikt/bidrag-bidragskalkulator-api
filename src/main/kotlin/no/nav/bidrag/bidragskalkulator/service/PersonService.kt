package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.BrukerInfomasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.BrukerInformasjonMapper
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import org.springframework.stereotype.Service

@Service
class PersonService(private val personConsumer: BidragPersonConsumer) {

    fun hentInformasjon(personIdent: String): BrukerInfomasjonDto {
        val familierelasjon =  SikkerhetsKontekst.medApplikasjonKontekst {
            personConsumer.hentFamilierelasjon(personIdent)
        }

        return BrukerInformasjonMapper.tilBrukerInformasjonDto(familierelasjon)
    }
}