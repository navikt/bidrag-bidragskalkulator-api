package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.bidrag.bidragskalkulator.consumer.BidragPersonConsumer
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.BrukerInformasjonMapper
import no.nav.bidrag.bidragskalkulator.utils.asyncCatching
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class BrukerinformasjonService(
    @Qualifier("bidragPersonConsumer") private val personConsumer: BidragPersonConsumer,
    private val grunnlagService: GrunnlagService,
    private val beregningService: BeregningService
) {
    private val logger = LoggerFactory.getLogger(BrukerinformasjonService::class.java)

    fun hentInformasjon(personIdent: String): BrukerInformasjonDto = runBlocking(Dispatchers.IO + MDCContext()) {
        val inntektsGrunnlag = async { grunnlagService.hentInntektsGrunnlag(personIdent) }
        val familierelasjonJobb =  async { personConsumer.hentFamilierelasjon(personIdent) }
        val familierelasjon = familierelasjonJobb.await()

        val underholdskostnad =  async { beregningService.beregnUnderholdskostnaderForBarnIFamilierelasjon(familierelasjon) }

        BrukerInformasjonMapper.tilBrukerInformasjonDto(familierelasjon, underholdskostnad.await(), inntektsGrunnlag.await())
    }

    suspend fun hentBrukerinformasjon(personIdent: String): BrukerInformasjonDto = coroutineScope {
        logger.info("Starter henting av inntektsgrunnlag, familierelasjoner og underholdskostnad for barn for å utforme brukerinformasjon")

        val inntektsGrunnlagJobb = asyncCatching(logger, "inntektsgrunnlag") {
            grunnlagService.hentInntektsGrunnlag(personIdent)
        }

        val familierelasjonJobb = asyncCatching(logger, "familierelasjon") {
            personConsumer.hentFamilierelasjon(personIdent)
        }

        val familierelasjon = familierelasjonJobb.await()
        val underholdskostnadJobb = asyncCatching(logger, "underholdskostnad") {
            beregningService.beregnUnderholdskostnaderForBarnIFamilierelasjon(familierelasjon)
        }

        logger.info("Ferdig med henting av inntektsgrunnlag, familierelasjoner og underholdskostnad for barn for å utforme brukerinformasjon")

        BrukerInformasjonMapper.tilBrukerInformasjonDto(familierelasjon, underholdskostnadJobb.await(), inntektsGrunnlagJobb.await())
    }
}