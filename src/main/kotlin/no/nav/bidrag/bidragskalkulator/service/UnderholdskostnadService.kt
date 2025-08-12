package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.config.CacheConfig
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUnderholdskostnad
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
open class UnderholdskostnadService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,

) {
    val logger = getLogger(UnderholdskostnadService::class.java)

    @Cacheable(CacheConfig.UNDERHOLDSKOSTNAD)
    open fun genererUnderholdskostnadstabell(): Map<Int, BigDecimal> {
        return (0..25).associateWith { alder ->
            beregnCachedPersonUnderholdskostnad(alder)
        }.also { logger.info("Generert underholdskostnadstabell for aldersintervall 0-25") }
    }

    open fun beregnCachedPersonUnderholdskostnad(alder: Int): BigDecimal {
        val fødselsdato = LocalDate.now().minusYears(alder.toLong())
        val grunnlag = beregningsgrunnlagMapper.mapTilUnderholdkostnadsgrunnlag(fødselsdato, "x")
        return beregnBarnebidragApi.beregnUnderholdskostnad(grunnlag)
            .firstOrNull { it.type == Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD }
            ?.innholdTilObjekt<DelberegningUnderholdskostnad>()
            ?.underholdskostnad
            ?: throw IllegalStateException("Ingen underholdskostnad funnet for alder $alder")
    }
}
