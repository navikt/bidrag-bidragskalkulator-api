package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.BrukerInformasjonMapper
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.NavnFødselDødDto
import org.springframework.stereotype.Service

@Service
class PersonService(private val personConsumer: BidragPersonConsumer, private val grunnlagService: GrunnlagService) {

    fun hentInformasjon(personIdent: String): BrukerInformasjonDto = runBlocking(Dispatchers.Default) {
        val inntektsGrunnlag = async { grunnlagService.hentInntektsGrunnlag(personIdent) }
        val familierelasjon =  async {
            SikkerhetsKontekst.medApplikasjonKontekst {
                personConsumer.hentFamilierelasjon(personIdent)
            }
        }
        BrukerInformasjonMapper.tilBrukerInformasjonDto(familierelasjon.await(), inntektsGrunnlag.await())
    }

    fun hentNavnFødselDød(personIdent: Personident): NavnFødselDødDto {
        return  SikkerhetsKontekst.medApplikasjonKontekst {
            personConsumer.hentNavnFødselDød(personIdent)
        }
    }
}