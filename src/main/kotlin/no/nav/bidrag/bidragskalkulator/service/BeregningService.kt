package no.nav.bidrag.bidragskalkulator.service

import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningRequestDto
import no.nav.bidrag.bidragskalkulator.dto.BeregningsresultatBarnDto
import no.nav.bidrag.bidragskalkulator.mapper.BeregningsgrunnlagMapper
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.*
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class BeregningService(
    private val beregnBarnebidragApi: BeregnBarnebidragApi,
    private val beregningsgrunnlagMapper: BeregningsgrunnlagMapper,
) {

    private val logger = getLogger(BeregningsgrunnlagMapper::class.java)

    fun beregnBarnebidrag(beregningRequest: BeregningRequestDto): BeregningsresultatDto {
        val beregningsgrunnlag = beregningsgrunnlagMapper.mapTilBeregningsgrunnlag(beregningRequest)

        val start = System.currentTimeMillis()
        val beregningsresultat = beregningsgrunnlag.parallelStream().map { data ->
            val beregnetSum = beregnBarnebidragApi.beregn(data.grunnlag)
                .beregnetBarnebidragPeriodeListe
                .sumOf { it.resultat.beløp ?: BigDecimal.ZERO }

            BeregningsresultatBarnDto(
                sum = beregnetSum.divide(BigDecimal(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100)),
                barnetsAlder = data.barnetsAlder,
                underholdskostnad = hentUnderholdskostnad(data.grunnlag)
            )
        }.toList()
        
        val duration = System.currentTimeMillis() - start;
        logger.info("Beregning av ${beregningsresultat.size} barn tok $duration ms")

        return BeregningsresultatDto(beregningsresultat)
    }

    internal fun hentUnderholdskostnad(grunnlag: BeregnGrunnlag): BigDecimal =
        beregnBarnebidragApi.beregnUnderholdskostnad(grunnlag)
            .firstOrNull { it.type == Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD }
            ?.innholdTilObjekt<DelberegningUnderholdskostnad>()
            ?.underholdskostnad
            ?: BigDecimal.ZERO
}