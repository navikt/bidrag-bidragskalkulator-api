package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.coroutineScope
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.tilPersonInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.toInntektResultatDto
import no.nav.bidrag.bidragskalkulator.utils.asyncCatching
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrukerinformasjonService(
    private val personService: PersonService,
    private val grunnlagService: GrunnlagService,
    private val beregningService: BeregningService
) {
    private val logger = LoggerFactory.getLogger(BrukerinformasjonService::class.java)

    suspend fun hentBrukerinformasjon(personIdent: String): BrukerInformasjonDto = coroutineScope {
        logger.info("Starter henting av inntektsgrunnlag, familierelasjoner og underholdskostnad for barn for å utforme brukerinformasjon")

        val inntektsGrunnlagJobb = asyncCatching(logger, "inntektsgrunnlag") {
            grunnlagService.hentInntektsGrunnlag(personIdent)
        }

        val familierelasjonJobb = asyncCatching(logger, "familierelasjon") {
            personService.hentGyldigFamilierelasjon(personIdent)
        }

        val familierelasjon = familierelasjonJobb.await()
        val barnMedUnderholdskostnadJobb = asyncCatching(logger, "underholdskostnad") {
            beregningService.beregnUnderholdskostnaderForBarnerelasjoner(familierelasjon.motpartsrelasjoner)
        }

        logger.info("Ferdig med henting av inntektsgrunnlag, familierelasjoner og underholdskostnad for barn for å utforme brukerinformasjon")

        BrukerInformasjonDto(
            person = familierelasjonJobb.await().person.tilPersonInformasjonDto(),
            inntekt = inntektsGrunnlagJobb.await()?.toInntektResultatDto()?.inntektSiste12Mnd,
            barnerelasjoner = barnMedUnderholdskostnadJobb.await()
        )
    }
}