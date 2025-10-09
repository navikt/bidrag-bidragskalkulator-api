package no.nav.bidrag.bidragskalkulator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.config.CacheConfig
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUnderholdskostnad
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

@Service
open class UnderholdskostnadService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,

) {
    @Cacheable(CacheConfig.UNDERHOLDSKOSTNAD)
    open fun genererUnderholdskostnadstabell(): Map<Int, BigDecimal> {
        logger.info { "Genererer underholdskostnadstabell for aldersintervall 0–25." }
        val (resultat, varighet) = runCatching {
            measureTimedValue {
                (1..25).associateWith { alder -> beregnCachedPersonUnderholdskostnad(alder) }
            }
        }.onFailure { e ->
            logger.error{ "Generering av underholdskostnadstabell feilet." }
            secureLogger.error(e) { "Generering av underholdskostnadstabell feilet: ${e.message}" }
        }.getOrThrow()

        logger.info { "Kall til beregn-barnebidrag-api OK (varighet_ms=${varighet.inWholeMilliseconds})." }
        return resultat
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
