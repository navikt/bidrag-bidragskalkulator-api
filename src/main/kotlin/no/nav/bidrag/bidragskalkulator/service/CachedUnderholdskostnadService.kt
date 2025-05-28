package no.nav.bidrag.bidragskalkulator.service

import Cachenøkler
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
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
open class CachedUnderholdskostnadService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,

) {
    val logger = getLogger(CachedUnderholdskostnadService::class.java)

    @Cacheable(Cachenøkler.UNDERHOLDSKOSTNAD)
    open fun beregnCachedPersonUnderholdskostnad(alder: Int): BigDecimal {
        val fødselsdato = LocalDate.now().minusYears(alder.toLong())
        val grunnlag = beregningsgrunnlagMapper.mapTilUnderholdkostnadsgrunnlag(fødselsdato, "x")
        logger.info("Henter underholdskostnad for person med grunnlag: $grunnlag")
        return beregnBarnebidragApi.beregnUnderholdskostnad(grunnlag)
            .firstOrNull { it.type == Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD }
            ?.innholdTilObjekt<DelberegningUnderholdskostnad>()
            ?.underholdskostnad
            ?: BigDecimal.ZERO.also { logger.info("Ferdig beregnet underholdskostnad for en person") }
    }
}