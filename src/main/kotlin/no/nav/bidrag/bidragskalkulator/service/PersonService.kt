package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.BrukerInfomasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.BrukerInformasjonMapper
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import org.springframework.stereotype.Service

@Service
class PersonService(private val personConsumer: BidragPersonConsumer, private val grunnlagService: GrunnlagService) {

    fun hentInformasjon(personIdent: String): BrukerInfomasjonDto {
        val familierelasjon = personConsumer.hentFamilierelasjon(personIdent)
        val inntektsGrunnlag = grunnlagService.hentInntektsGrunnlag(personIdent)
        return BrukerInformasjonMapper.tilBrukerInformasjonDto(familierelasjon, inntektsGrunnlag)
    }
}