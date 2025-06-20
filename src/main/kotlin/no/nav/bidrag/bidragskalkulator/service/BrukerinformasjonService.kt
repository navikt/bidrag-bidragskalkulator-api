package no.nav.bidrag.bidragskalkulator.service

import kotlinx.coroutines.coroutineScope
import no.nav.bidrag.bidragskalkulator.config.CacheConfig
import no.nav.bidrag.bidragskalkulator.dto.BrukerInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.tilPersonInformasjonDto
import no.nav.bidrag.bidragskalkulator.mapper.toInntektResultatDto
import no.nav.bidrag.bidragskalkulator.utils.asyncCatching
import no.nav.bidrag.domene.ident.Personident
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class BrukerinformasjonService(
    private val personService: PersonService,
    private val grunnlagService: GrunnlagService,
    private val sjablonService: SjablonService,
    private val underholdskostnadService: UnderholdskostnadService
) {
    private val logger = LoggerFactory.getLogger(BrukerinformasjonService::class.java)

    @Cacheable(CacheConfig.PERSONINFORMASJON)
    suspend fun hentBrukerinformasjon(personIdent: String): BrukerInformasjonDto = coroutineScope {
        logger.info("Starter henting av person informasjon og inntektsgrunnlag for å utforme brukerinformasjon")

        val inntektsGrunnlagJobb = asyncCatching(logger, "inntektsgrunnlag") {
            grunnlagService.hentInntektsGrunnlag(personIdent)
        }

        val personinformasjonJobb = asyncCatching(logger, "person informasjon") {
            personService.hentPersoninformasjon(Personident(personIdent))
        }

        val samværsfradragJobb = asyncCatching(logger, "samværsfradrag") {
            sjablonService.hentSamværsfradrag()
        }

        logger.info("Ferdig med henting av person informasjon og inntektsgrunnlag for å utforme brukerinformasjon")

        BrukerInformasjonDto(
            person = personinformasjonJobb.await().tilPersonInformasjonDto(),
            inntekt = inntektsGrunnlagJobb.await()?.toInntektResultatDto()?.inntektSiste12Mnd,
            barnerelasjoner = emptyList(),
            underholdskostnader = underholdskostnadService.genererUnderholdskostnadstabell(),
            samværsfradrag = samværsfradragJobb.await()
        )
    }
}
