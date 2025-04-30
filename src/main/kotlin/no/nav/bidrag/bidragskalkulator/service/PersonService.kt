package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.BrukerInformasjonMapper
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.NavnFødselDødDto
import org.springframework.stereotype.Service

@Service
class PersonService(private val personConsumer: BidragPersonConsumer, private val grunnlagService: GrunnlagService) {

    fun hentInformasjon(personIdent: String): BrukerInformasjonDto {
        val inntektsGrunnlag = grunnlagService.hentInntektsGrunnlag(personIdent)
        val familierelasjon =  SikkerhetsKontekst.medApplikasjonKontekst {
            personConsumer.hentFamilierelasjon(personIdent)
        }
        return BrukerInformasjonMapper.tilBrukerInformasjonDto(familierelasjon, inntektsGrunnlag)
    }

    fun hentNavnFoedselDoed(personIdent: Personident): NavnFødselDødDto {
        return  SikkerhetsKontekst.medApplikasjonKontekst {
            personConsumer.hentNavnFoedselDoed(personIdent)
        }
    }
}