package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
open class CachedUnderholdskostnadService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
) {
    @Cacheable(Cacher.UNDERHOLDSKOSTNAD)
    open fun beregnCachedPersonUnderholdskostnad(grunnlag: BeregnGrunnlag): List<GrunnlagDto> {
        return beregnBarnebidragApi.beregnUnderholdskostnad(grunnlag)
    }
}