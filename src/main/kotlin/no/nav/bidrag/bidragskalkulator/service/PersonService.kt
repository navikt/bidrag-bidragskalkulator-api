package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.BrukerInformasjonMapper
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PersonService(
    @Qualifier("bidragPersonConsumer") private val personConsumer: BidragPersonConsumer, private val grunnlagService: GrunnlagService) {

    fun hentInformasjon(personIdent: String): BrukerInformasjonDto = runBlocking(Dispatchers.IO + MDCContext()) {
        val inntektsGrunnlag = async { grunnlagService.hentInntektsGrunnlag(personIdent) }
        val familierelasjon =  async { personConsumer.hentFamilierelasjon(personIdent) }
        BrukerInformasjonMapper.tilBrukerInformasjonDto(familierelasjon.await(), inntektsGrunnlag.await())
    }

    fun hentPersoninformasjon(personIdent: Personident): PersonDto = personConsumer.hentPerson(personIdent)
}