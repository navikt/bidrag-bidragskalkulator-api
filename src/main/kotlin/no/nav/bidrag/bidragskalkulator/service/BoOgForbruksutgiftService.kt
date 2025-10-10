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
open class BoOgForbruksutgiftService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,

) {
    @Cacheable(CacheConfig.BOOGFORBRUKSUTGIFT)
    open fun genererBoOgForbruksutgiftstabell(): Map<Int, BigDecimal> {
        logger.info { "Genererer bo- og forbruksutgiftstabell for aldersintervall 0–25." }
        val (resultat, varighet) = runCatching {
            measureTimedValue {
                val map = linkedMapOf<Int, BigDecimal>()

                // beregn først alder 1, gjenbruk for alder 0
                val verdiFor1 = beregnCachedPersonBoOgForbruksutgiftskostnad(1)
                map[0] = verdiFor1
                map[1] = verdiFor1

                // legg til 2–25
                (2..25).forEach { alder ->
                    map[alder] = beregnCachedPersonBoOgForbruksutgiftskostnad(alder)
                }

                return@measureTimedValue map
            }
        }.onFailure { e ->
            logger.error{ "Generering av bo- og forbruksutgiftstabell feilet." }
            secureLogger.error(e) { "Generering av bo- og forbruksutgiftstabell feilet: ${e.message}" }
        }.getOrThrow()

        logger.info { "Kall til beregn-barnebidrag-api OK (varighet_ms=${varighet.inWholeMilliseconds})." }
        return resultat
    }

    open fun beregnCachedPersonBoOgForbruksutgiftskostnad(alder: Int): BigDecimal {
        val fødselsdato = LocalDate.now().minusYears(alder.toLong())
        val grunnlag = beregningsgrunnlagMapper.mapTilBoOgForbruksutgiftsgrunnlag(fødselsdato, "x")
        return beregnBarnebidragApi.beregnUnderholdskostnad(grunnlag)
            .firstOrNull { it.type == Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD }
            ?.innholdTilObjekt<DelberegningUnderholdskostnad>()
            ?.underholdskostnad
            ?: throw IllegalStateException("Ingen bo- og forbruksutgift funnet for alder $alder")
    }
}
